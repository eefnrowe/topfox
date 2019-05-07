package com.topfox.spring;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.topfox.common.*;
import com.topfox.conf.CustomRedisTemplate;
import com.topfox.data.*;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.Misc;
import com.topfox.misc.ReflectUtils;
import com.topfox.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class SimpleService<QTO extends DataQTO, DTO extends DataDTO>  {
    public Class<DTO> clazz;
    protected EntitySql entitySql = null; //生成SQL的工具类
    protected TableInfo tableInfo = null; //得到表结构
    protected DataCache dataCache = null; //实现一级二级缓存

    private static ThreadLocal<JSONObject> threadLocalAttributes = new ThreadLocal();
    //private static ThreadLocal<EntitySql> threadLocalEntitySql = new ThreadLocal();
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private FillDataHandler fillDataHandler;
    protected BaseDao<QTO, DTO> baseDao;

    @Autowired
    @Qualifier("sysConfigDefault")
    private SysConfigRead sysConfigRead;//单实例读取值 全局一个实例
    protected SysConfig sysConfig;          //每个service独用的实例

    @Autowired
    protected Environment environment;

//    @Autowired
//    @Qualifier("sysRedisTemplateDTO")
//    protected CustomRedisTemplate sysRedisTemplateDTO;
//
    @Autowired
    @Qualifier("sysStringRedisTemplate") //这个实例 已经 关闭事务了的
    protected CustomRedisTemplate sysStringRedisTemplate;

    @Autowired
    RestSessionHandler restSessionHandler;

    protected void setFillDataHandler(FillDataHandler fillDataHandler){
        this.fillDataHandler = fillDataHandler;
    }

    protected void setBaseDao(BaseDao<QTO, DTO> value){
        this.baseDao = value;
    }

    protected void init(){

    }

    /**
     * new一个DTO实体
     * @return
     */
    public final DTO getEntity(){
        //beforeInit();
        return BeanUtil.getEntity(clazz);
    }

    /**
     * 初始化之前, 类库要做的事情
     * @param listUpdate 更新时,  对DTO的处理用
     */
    public final void beforeInit(List<DataDTO> listUpdate){
        threadLocalAttributes.set(new JSONObject());
        if (sysConfig == null) {
            sysConfig = (SysConfig)BeanUtil.cloneBean(sysConfigRead); //为每个Service
        }

        //保证初始化 只执行一次
        if (clazz == null) {
            clazz = ReflectUtils.getClassGenricType(getClass(), 1); //获得当前DTO的Class
        }
        if (tableInfo == null){
            tableInfo = TableInfo.get(clazz);//表结构类
//            2019-04-08注释
//            tableInfo.sqlCamelToUnderscore=sysConfig.getSqlCamelToUnderscore(); //生成SQL驼峰命名转下划线
//            tableInfo.openRedis(sysConfig.isOpenRedis());
//            if (!sysConfig.isOpenRedis()){
//                logger.debug("{}重要参数openReids已经被 设置为 false", sysConfigRead.getLogPrefix());
//            }
        }

        entitySql = restSessionHandler.getEntitySql(clazz); //SQL构造类
        init();//开发者自定义的初始化逻辑
        //init()可能修改以下参数  2019-04-08///////////////////////////////////////////////////////////////
        tableInfo.sqlCamelToUnderscore=sysConfig.getSqlCamelToUnderscore(); //生成SQL驼峰命名转下划线
        tableInfo.openRedis(sysConfig.isOpenRedis());
        if (!sysConfig.isOpenRedis()){
            logger.debug("{}重要参数openReids已经被 设置为 false", sysConfigRead.getLogPrefix());
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////

        dataCache = restSessionHandler.getDataCache();//一级二级缓存处理类(每个线程会创建一个)

        //###################################################################################
        AbstractRestSession restSession = restSessionHandler.get();//获得当前线程的 restSession
        if (restSession == null) {
            restSession = restSessionHandler.create();//创建 restSession
            restSessionHandler.initRestSession(restSession, null);
        }

//        //通过消息调用Service方法时(没有经过拦截器)时, 以下两个对象可能为null
//        if (dataCache == null){
//            dataCache = new DataCache(sysRedisTemplateDTO, sysConfig);//新建一二级缓存处理对象
//            restSessionHandler.setDataCache(dataCache);
//        }

        //###################################################################################

        afterInit();

        if (listUpdate != null){
            initUpdateDTO(listUpdate);
        }
    }
//    protected final void beforeInit() {
//        beforeInit(null);
//    }
    /**
     * 线程安全级别的自定属性
     * @return
     */
    public final JSONObject attributes() {
        return threadLocalAttributes.get();
    }

    /**
     * 初始化之后, 类库要做的事情
     */
    public void afterInit(){
    }

    /**
     * 创建一个新的条件对象Condition
     * @see Condition
     * @return
     */
    public Condition where(){
//        if (clazz==null) {
//            beforeInit();
//        }
        return Condition.create(clazz);
    }

    protected final void beforeSave2(List<DTO> list, String dbState){
        list.forEach((beanDTO) -> {
            beanDTO.dataState(dbState);
            if (DbState.INSERT.equals(dbState) && beanDTO.dataVersion()==null){
                beanDTO.dataVersion(1);
            }
        });
        if (fillDataHandler != null && (DbState.INSERT.equals(dbState) || DbState.UPDATE.equals(dbState))) {
            /** 调用"填充接口实现类的方法", 主要是 创建和修改 人/时间 */
            fillDataHandler.fillData(tableInfo.getFillFields(), (List<DataDTO>) list);
        }

        beforeSave(list);

        if (DbState.INSERT.equals(dbState) || DbState.UPDATE.equals(dbState)) {
            //执行插入或更新之前的的方法
            beforeInsertOrUpdate(list);
            list.forEach((beanDTO) -> beforeInsertOrUpdate(beanDTO, dbState));
        } else if (DbState.DELETE.equals(dbState)) {
            //执行删除之前的逻辑
            beforeDelete(list);
            list.forEach((beanDTO) -> beforeDelete(beanDTO));
        }
    }
    /**
     * 增 删 改 sql执行之后 调用本方法
     * @param list
     */
    protected final void afterSave2(List<DTO> list, String dbState){
        Map<String, Field> fieldsForIncremental = tableInfo.getFieldsForIncremental();//注解为 自增减的字段 的 值 特殊处理
        //缓存处理
        list.forEach(dto->{
            if (DbState.UPDATE.equals(dto.dataState())){
                if (dto.origin()==null){
                    DTO redisDTO1 = dataCache.get2CacheById(clazz, dto.dataId(), sysConfig.isOpenRedis(), "afterSave2");
                    if (redisDTO1 == null || fieldsForIncremental.size() > 0){
                        dataCache.deleteRedis(tableInfo, dto.dataId(), sysConfig.isOpenRedis());//没有原始数据,则必须要 删除redis缓存
                        return;  //continue; 后台 直接 new 的dto, 缓存也没有, 则更新后不放入缓存. 因为要去查一次才能获得原始数据
                    }else {
                        dto.addOrigin(redisDTO1);
                    }
                }

                DTO redisDTO2 = sysConfig.getUpdateMode()==1?dto.merge():dto;
                //对递增减的所有字段循环
                fieldsForIncremental.forEach((fieldName, field) -> {
                    Number originValue = DataHelper.parseDouble(BeanUtil.getValue(tableInfo, dto.origin(), field));
                    Number currentValue = DataHelper.parseDouble(BeanUtil.getValue(tableInfo, dto, field));
                    Number result = ChangeData.incrtl(field, currentValue,originValue);
                    if (result !=null ){
                        BeanUtil.setValue(tableInfo, redisDTO2, field, result);
                        dto.mapSave().put(fieldName, result); //将递增/减 后的值返回到 调用方(如前端)
                    }
                });

                //DTO 和 mapSave 的版本字段 +  1, 便于 缓存reids 和输出到 前台
                String versionFieldName = tableInfo.getVersionFieldName();
                if (Misc.isNotNull(versionFieldName) && redisDTO2.dataVersion() != null ){
                    redisDTO2.dataVersion(redisDTO2.dataVersion() + 1);//DTO +1
                    dto.mapSave().put(versionFieldName, dto.dataVersion());  //将递增后的值返回到 调用方(如前端)
                }

                //@RowId字段始终返回前端
                String rowIdDFieldName = tableInfo.getRowNoFieldName();
                if (Misc.isNotNull(rowIdDFieldName)){
                    dto.mapSave().put(rowIdDFieldName, dto.dataRowId());
                }

                dataCache.addCacheBySave(redisDTO2, sysConfig.isOpenRedis());//记录哪些DTO需要缓存到到redis, 此时还没有保存到redis
                //dto.addOrigin(redisDTO2); //重要一个DTO 在一个线程中保存多次使用
                return;
            }

            /**
             * 新增时, 不考虑 放入缓存.  因为 DTO没有 默认值,  默认值 是在 数据库 中
             */

            if (DbState.DELETE.equals(dto.dataState())){//DbState.INSERT.equals(dto.dataState()) ||
                //删除 时放入 缓存
                dataCache.addCacheBySave(dto, sysConfig.isOpenRedis());//记录哪些DTO需要缓存到到redis, 此时还没有保存到redi
            }
        });

        /**
         * 必须是合并过的Bean(数据库原始数据 + 需改的数据 的合并)
         */
        afterSave(list);
        if(DbState.INSERT.equals(dbState) || DbState.UPDATE.equals(dbState)){
            afterInsertOrUpdate(list);
            list.forEach((beanDTO) -> afterInsertOrUpdate(beanDTO, dbState));
        }else if(DbState.DELETE.equals(dbState)){
            afterDelete(list);
            list.forEach((beanDTO) -> afterDelete(beanDTO));
        }
    }

    protected void beforeSave(List<DTO> list) {
    }
    /**
     *新增和修改的 之前的逻辑写到这里,  如通用的检查
     */
    protected void beforeInsertOrUpdate(List<DTO> list){
    }
    protected void beforeInsertOrUpdate(DTO beanDTO, String dbState){

    }

    /**
     * delete() deleteByIds() deleteBatch() deleteList()执行之前 执行本方法
     */
    protected void beforeDelete(List<DTO> list){
    }
    protected void beforeDelete(DTO beanDTO){
    }

    protected void afterSave(List<DTO> list) {
    }
    protected void afterInsertOrUpdate(DTO beanDTO, String dbState){

    }
    /**
     * 多行执行本方法一次, 新增和修改的 之后的逻辑写到这里,  如通用的检查, 计算值得处理
     */
    protected void afterInsertOrUpdate(List<DTO> list){
    }
    /**
     * delete() deleteByIds() deleteBatch() deleteList()执行之后 执行本方法
     */
    protected void afterDelete(List<DTO> list){
    }
    protected void afterDelete(DTO beanDTO){
    }

    public int insert(DTO beanDTO){
        List<DTO> list = new ArrayList<>(1);
        list.add(beanDTO);
        return insertList(list);
    }

    /**
     * 约束: 一个表只有一个 主键字段, 整型自增
     * @param beanDTO
     *
     * @return 返回 插入DTO的 主键Id
     */
    public int insertGetKey(DTO beanDTO) {
        insert(beanDTO);
        int lastId = baseDao.selectForInteger("select LAST_INSERT_ID()");//按照connect的对象隔离的
        beanDTO.dataId(lastId);
        return lastId;
    }

    /**
     *
     * 实现多个beanDTO 生成一句SQL插入
     * @param list
     *
     * @return
     */
    public int insertList(List<DTO> list) {
        return insertList(list, false);
    }

    /**
     * 内部方法
     * @param list
     * @param isSaveMethod 防止save-->insert/update/delete  重复调用before//after
     * @return
     */
    private int insertList(List<DTO> list, boolean isSaveMethod){
        if (list == null|| list.isEmpty()) return 0;
//        //自动生成一个Id
//        if(autoCreatekey) {
//            list.forEach(dto -> {
//                if (Misc.isNull(dto.dataId())) {
//                    dto.dataId(keyBuild().getKey(tableInfo.getIdColumn(), 6));
//                }
//            });
//        }
        //beforeInit();
        if (isSaveMethod == false) {
            beforeSave2(list, DbState.INSERT);
        }

        int result = baseDao.executeByInsertSql(entitySql.getInsertSql((List<DataDTO>)list));
        if (isSaveMethod == false) {
            afterSave2(list, DbState.INSERT);//执行插入之后的的方法
        }

        return result;
    }

    public int updateList(List<DTO> list) {
        return updateList(list, false);
    }

    private void initUpdateDTO(List<DataDTO> listUpdate){
        //beforeInit();
        List<DTO> listQuery;

        //获得更新之前的数据
        Set<String> setIds = listUpdate.stream().map(DataDTO::dataId).collect(Collectors.toSet());
        setIds.forEach(key->{
            if (Misc.isNull(key)) throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("更新时, 主键字段的值不能为空");
        });
        listQuery = listObjects(setIds);

        //提交的修改过的数据, list结构转成 map结构,  便于 获取
        Map<String, JSONObject> mapModifyData = new HashMap<>();
        JSONArray bodyData = abstractRestSession.getBodyData();
        if (bodyData != null) {
            bodyData.forEach(map -> {
                JSONObject mapTemp = (JSONObject) map;
                mapModifyData.put(mapTemp.getString(tableInfo.getIdFieldName()), mapTemp);
            });
        }
        Map<String, DTO> mapOriginDTO = listQuery.stream().collect(Collectors.toMap(DTO::dataId, Function.identity()));//转成map

        listUpdate.forEach(dto -> {
            if (dto.origin()==null) {
                //value是DTO. 添加 原始DTO-修改之前的数据, 来自于数据库查询或者Redis
                DTO origin = mapOriginDTO.get(dto.dataId());
                if (origin!=null && origin.hashCode() == dto.hashCode()){
                    dto.addOrigin(BeanUtil.cloneBean(origin));//重要
                }else {
                    dto.addOrigin(origin);
                }
            }
            dto.addModifyMap(mapModifyData.get(dto.dataId()));//value是JSONObject. 添加 提交的修改过的数据. 当依据变化值生成更新SQL时 需要用到

            ///////////////////////////////////////////////////////////////////////////////////
            //处理当前DTO= 原始数据 + 提交修改的数据
            if (sysConfig.getUpdateMode()>1) {
                if (dto.mapModify()==null){
                    //说明是后台 自己查询出来的数据做保存, 这里无逻辑
                }else {
                    //前台传回的数据, 执行以下逻辑
                    BeanUtil.copyBean( mapOriginDTO.get(dto.dataId()), dto);   //拷贝原始数据      到当前DTO
                    BeanUtil.map2Bean(mapModifyData.get(dto.dataId()), dto);   //拷贝提交修改的数据 到当前DTO
                    dataCache.addCacheBySelected(dto, sysConfig.isOpenRedis());//20181217添加到一级缓存
                }
            }
            ////////////////////////////////////////////////////////////////////////////////////
            //String tem=null;
        });
    }

    /**
     * 内部方法, 禁止使用
     * @param listUpdate
     * @param isSaveMethod
     * @return
     */
    private int updateList(List<DTO> listUpdate, boolean isSaveMethod){
        if (listUpdate == null || listUpdate.isEmpty()) return 0;
//        //beforeInit();

        if (isSaveMethod == false) {
            beforeSave2(listUpdate, DbState.UPDATE);
        }
        AtomicInteger result =  new AtomicInteger();//线程安全的自增类

        listUpdate.forEach((beanDTO)-> {
            DTO origin = beanDTO.origin();
            if (origin==null && beanDTO.dataVersion() != null) {
                origin = getObject(beanDTO.dataId());
            }
            if (beanDTO.dataVersion() != null && origin.dataVersion()!=null
                    && beanDTO.dataVersion()<origin.dataVersion()){
                throw CommonException.newInstance(ResponseCode.DB_UPDATE_ERROR)
                        .text(clazz.getName(), ".", beanDTO.dataId(),
                                " 可能已经被他人修改, 请刷新, 传入的版本 ",
                                beanDTO.dataVersion(),
                                " 数据库的版本 ",
                                origin.dataVersion());
            }
            int one = baseDao.executeByUpdateSql(entitySql.getUpdateByIdSql(beanDTO, sysConfig.getUpdateMode()));
            if (one == 0){
                throw CommonException.newInstance(ResponseCode.DB_UPDATE_ERROR).text(clazz.getName(), ".", beanDTO.dataId(), "更新失败, 该数据可能已经被他人修改, 请刷新");
            }
            //DTO.mapSave中处理beanDTO.dataVersion(beanDTO.dataVersion() == null?null:(beanDTO.dataVersion()+1));
            result.set(result.get() + one);
        });

        //before 可能会修改list里面的DTO, 这里重新合并一次
        if (isSaveMethod == false) {
            afterSave2(listUpdate, DbState.UPDATE);//执行更新之后的的方法
        }
        return result.get();
    }

    /**
     * 根据Id更新, version不是null, 则增加乐观锁
     * @param beanDTO
     * @return
     */
    public int update(DTO beanDTO){
        List<DTO> list = new ArrayList<>(1);
        list.add(beanDTO);
        return updateList(list, false);
    }

    /**
     * deleteByIds() deleteBatch() deleteList()不经过此方法
     */
    public int delete(DTO beanDTO){
        List<DTO> list = new ArrayList<>(1);
        list.add(beanDTO);
        return deleteList(list);
    }

    /**
     * 根据多个Id, 生成一句SQL删除, 无乐观锁
     * 先查询出来, 目的是得到每条记录的id, 然后根据Id删除redis缓存
     * @param ids
     * @return
     */
    public int deleteByIds(Object... ids){
        Set<String> setIds = Misc.array2Set(ids);
        List<DTO> list  = listObjects(setIds);//删除之前先查询出来, 带缓存
        if (ids != null && setIds.size() != list.size()){
            throw CommonException.newInstance(ResponseCode.DB_DELETE_ERROR).text("删除的数据可能已经不存在");
        }
        return deleteList(list);
    }

    /**
     * 主方法
     * 实现多个DTO批量一次删除, 不增加乐观锁
     * @param list
     * @return
     */
    public int deleteList(List<DTO> list) {
        return deleteList(list, false);
    }

    /**
     * 内部方法, 禁止使用
     * @param list
     * @param isSaveMethod
     * @return
     */
    private int deleteList(List<DTO> list, boolean isSaveMethod){
        if (list == null || list.isEmpty()) return 0;
        //beforeInit();

        if (isSaveMethod == false) {
            beforeSave2(list, DbState.DELETE);//执行删除之前的逻辑
        }

        int result=0;
        if (tableInfo.getFieldsByIds().size()==1){ //单@Id
            Set<String> setIds = list.stream().map(DataDTO::dataId).collect(Collectors.toSet());
            Object[] ids  = setIds.toArray(new String[list.size()]);
            result = baseDao.executeByDeleteSql(
                    //生成删除SQL语句
                    entitySql.getEntityDelete()
                            .deleteBatch()
                            .where().eq(tableInfo.getIdFieldName(), ids)
                            .getSql()
            );
        }else{ //多 @Id 字段 时处理
            AtomicInteger result2 =  new AtomicInteger();//线程安全的自增类
            list.forEach(dto->{ //逐行删除
                int one = baseDao.executeByDeleteSql(
                        //生成删除SQL语句
                        entitySql.getDeleteByIdSql(dto)
                );
                result2.set(result2.get() + one);
            });
            result=result2.get();
        }

        if (isSaveMethod == false) {
            afterSave2(list, DbState.DELETE);//执行删除之后的逻辑
        }
        return result;
    }

    private boolean checkIdType(Object id){
        Misc.checkObjNotNull(id,"id");
        if (id instanceof String || id instanceof Integer || id instanceof Long){
            return true;
        }
        throw CommonException.newInstance(ResponseCode.PARAM_IS_INVALID)
                .text("id 参数类型(",id.getClass().getName(),")不对, 目前仅仅支持 String Integer Long");
    }

    /**
     * 相当于selectById. 优先从缓存中获取
     * @param id
     * @return
     */
    public DTO getObject(Object id) {
        return getObject(id, true);
    }
    public DTO getObject(String id) {
        return getObject(id, true);
    }
    /**
     * 相当于selectById
     * @param id
     * @param isCache 是否要从缓存中获取
     * @return
     */
    public DTO getObject(Object id, boolean isCache) {
        checkIdType(id);
        //beforeInit();
        DTO beanDTO;
        if(isCache) {
            //该方法先去一级缓存(从当前线程)，再取二级缓存(Redis)
            beanDTO = dataCache.get2CacheById(clazz, id, sysConfig.isOpenRedis(), "getObject");
            if (beanDTO  !=  null) {
                return beanDTO;
            }
        }
        List<DTO> list = listObjects(where().eq(tableInfo.getIdFieldName(), id));
        beanDTO = list.isEmpty() ? null : list.get(0);

        return beanDTO;
    }

    public DTO getObject(QTO qto) {
        //beforeInit();
        qto.setPageIndex(0); qto.setPageSize(2);
        return getObject(where().setQTO(qto));
//        List<DTO> list = listObjects(qto);
//        if (list.isEmpty()) return null;
//        if (list.size()>1){
//            logger.warn("{}getObject获得了多条记录,默认返回第一条记录", sysConfigRead.getLogPrefix());
//        }
//        return list.get(0);
    }

    public DTO getObject(Condition where) {
        //beforeInit();
        List<DTO> list = listObjects(where);
        if (list.isEmpty()) return null;
        if (list.size()>1){
            logger.warn("{}getObject()获得了多条记录,默认返回第一条记录", sysConfigRead.getLogPrefix());
        }
        return list.get(0);
    }


    /**
     * 带缓存机制
     * 依据多个Id 查询，英文逗号串起来即可
     * @return DatasetEditor
     */
    public List<DTO> listObjects(Object... ids) {
        return listObjects(Misc.array2Set(ids), true);
    }
    public List<DTO> listObjects(Set<String> setIds) {
        return listObjects(setIds, true);
    }
    /**
     *
     * @param ids 要查找的多个id值, 用英文逗号串起来
     * @param isCache 是否要从缓存中获取
     * @return
     */
    public List<DTO> listObjects(Set<String> ids, boolean isCache) {
        boolean isAllCache = true;//是否全部可以从缓存中得到

        //查询的多个Ids都从缓存中获取一次, 如果全部获取到, 就不用查数据库了
        List<DTO> listBeans = new ArrayList();
        if (isCache) {//从缓存中获取
            for (String id : ids) {
                if (Misc.isNull(id)) continue;
                DTO beanDTO = dataCache.get2CacheById(clazz, id, sysConfig.isOpenRedis(), "listObjects");
                if (beanDTO  !=  null) {
                    listBeans.add(beanDTO);
                } else {
                    isAllCache = false;
                    break;
                }
            }
        }
        if(isCache == true && isAllCache == true){//全部命中缓存
            if(ids.size()>1) {
                logger.debug("{}listObjects({})全部命中缓存", sysConfigRead.getLogPrefix(), ids.toString()); //多条 才 打印 全部命中缓存
            }
            return listBeans;
        }else{
            return listObjects(where().eq(tableInfo.getIdFieldName(), ids));
        }
    }

    public List<DTO> listObjects(QTO qto) {
        return listObjects(null, true, where().setQTO(qto));
    }

    /**
     * 自定义条件的查询. 不会从二级缓存Redis中获取
     * @param where
     * @return
     */
    public List<DTO> listObjects(Condition where) {
        //beforeInit();
        return listObjects(null, true, where);
    }

    /**
     * 自定义条件的查询. 不会从二级缓存Redis中获取
     * @param fields
     * @param where
     * @return
     */
    public List<DTO> listObjects(String fields, Condition where){
        //beforeInit();
        return listObjects(fields, false, where);
    }

    /**
     * 自定义条件的查询. 不会从二级缓存Redis中获取
     *
     * @param fields 指定查询返回的字段,多个用逗号串联, 即select 后的字段名
     * @param isAppendAllFields 指定字段后, 是否要添加默认的所有字段
     * @param where 条件匹配器Condition对象
     * @return 返回 List< DTO >
     *
     */
    public List<DTO> listObjects(String fields, Boolean isAppendAllFields, Condition where){
        //直接从数据库中查询出结果
        List<DTO> list = selectBatch(fields, isAppendAllFields, where);

        List<DTO> listDTO =  new ArrayList<>();//定义返回的List对象
        if (list.isEmpty()){
            return listDTO;
        }

        list.forEach((dto) -> {
            //根据Id从一级缓存中获取
            DTO cacheDTO = dataCache.getCacheBySelect(clazz, dto.dataId());
            if (cacheDTO != null) {//一级缓存找到对象, 则以一级缓存的为准,作为返回
                listDTO.add(cacheDTO);
            }else {
                //fields为空, 默认返回所有字段, 所以可以更新缓存
                if(Misc.isNull(fields) || isAppendAllFields) {
                    //添加一级缓存, 二级缓存(考虑版本号)
                    dataCache.addCacheBySelected(dto, sysConfig.isOpenRedis());
                }
                listDTO.add(dto);
            }
        });

        return listDTO;
    }


    public EntitySelect select(){
        return entitySql.getEntitySelect().select();
    }
    public EntitySelect select(String fields,Boolean isAppendAllFields){
        return entitySql.getEntitySelect().select(fields,isAppendAllFields);
    }
    public EntitySelect select(String fields){
        return entitySql.getEntitySelect().select(fields);
    }

    /**
     * 自定义条件的查询, 不会从缓存中获取
     * @param where 条件对象 Condition
     * @return
     */
    public List<DTO> selectBatch(Condition where) {
        return selectBatch(null, true, where);
    }

    /**
     * 自定义条件的查询, 不会从缓存中获取
     * @param fields 指定返回的字段
     * @param where
     * @return
     */
    public List<DTO> selectBatch(String fields, Condition where){
        //beforeInit();
        return selectBatch(fields, false, where);
    }
    public List<DTO> selectBatch(String fields, Boolean isAppendAllFields, Condition where){
        //beforeInit();
        EntitySelect entitySelect=select(fields, isAppendAllFields);
        if (where.getQTO()!=null) {
            entitySelect.setPage(where.getQTO().getPageIndex(),where.getQTO().getPageSize());
        }
        List<DTO> list = baseDao.selectForDTO(entitySelect.setWhere(where).getSql());
        return list;
    }

    public List<Map<String, Object>> selectForListMap(Condition where) {
        //beforeInit();
        return selectForListMap(null, true, where);
    }

    /**
     * 自定义条件的查询, 不会从缓存中获取
     * @param fields 指定返回的字段
     * @param where
     * @return
     */
    public List<Map<String, Object>> selectForListMap(String fields, Condition where){
        //beforeInit();
        return selectForListMap(fields, false, where);
    }
    public List<Map<String, Object>> selectForListMap(String fields, Boolean isAppendAllFields, Condition where){
        //beforeInit();
        EntitySelect entitySelect=select(fields, isAppendAllFields);
        if (where.getQTO()!=null) {
            entitySelect.setPage(where.getQTO().getPageIndex(),where.getQTO().getPageSize());
        }
        List<Map<String, Object>> list = baseDao.selectBySql(entitySelect.setWhere(where).getSql());
        return list;
    }

    /**
     * 单表分页自定义SQL查询
     */
    public Response<List<DTO>> selectBatch(EntitySelect entitySelect){
        List<DTO> list = baseDao.selectForDTO(entitySelect.getSql());
        Response<List<DTO>> response= new Response(list, selectCount(entitySelect.where()));
        return response;
    }

    /**
     * 单表分页自定义SQL查询
     */
    public Response<List<Map<String, Object>>> selectForListMap(EntitySelect entitySelect){
        List<Map<String, Object>> list = baseDao.selectBySql(entitySelect.getSql());
        Response<List<Map<String, Object>>> response= new Response(list, selectCount(entitySelect.where()));
        return response;
    }

    /**
     * 自定义条件的计数查询, 不会从缓存中获取
     * @param where
     * @return
     */
    public int selectCount(Condition where){
        //beforeInit();
        Integer pageIndex = null, pageSize = null;
        DataQTO qto = where.getQTO();
        if (qto != null) {
            pageIndex = qto.getPageIndex(); pageSize = qto.getPageSize();
            qto.setPageIndex(null); qto.setPageSize(null);
        }
        List<Map<String, Object>> list = selectForListMap("count(1) count", where);
        if (qto != null) {
            qto.setPageIndex(pageIndex); qto.setPageSize(pageSize);
        }

        if (!list.isEmpty()){
            return DataHelper.parseInt(list.get(0).get("count"));
        }
        return 0;
    }

    /**
     *
     * @param fieldName 指定的字段名
     * @param where 条件匹配器
     * @param <T> 返回类型的泛型
     * @return
     */
    public <T> T  selectMax(String fieldName, Condition where){
        //beforeInit();
        List<Map<String, Object>> list = baseDao.selectBySql(
                entitySql.select("max("+fieldName+") maxData")
                        .setWhere(where)
                        .getSql()
        );
        if (!list.isEmpty()){
            return list.get(0)==null?(T)new Integer(0):(T)list.get(0).get("maxData");
        }
        return null;
    }


    /**
     * 支持一次提交存在增删改的多行, 依据DTO中dataState()状态来识别增删改操作.
     * @see DbState
     * @param list
     */
    public void save(List<DTO> list) {
        if (list == null) return;
        //beforeInit();
        List<DTO> listInsert = list.stream().filter(beanDTO -> DbState.INSERT.equals(beanDTO.dataState())).collect(toList());//所有新增的记录
        List<DTO> listUpdate = list.stream().filter(beanDTO -> DbState.UPDATE.equals(beanDTO.dataState())).collect(toList());//所有更新的记录
        List<DTO> listDelete = list.stream().filter(beanDTO -> DbState.DELETE.equals(beanDTO.dataState())).collect(toList());//所有删除的记录

        insertList(listInsert, false);
        updateList(listUpdate, false);
        deleteList(listDelete, false);
    }

    /**
     * 创建数据校验类
     * @param bean
     * @return
     */
    public final CheckData checkData(IBean bean){
        List<IBean> list  = new ArrayList(1);
        list.add(bean);
        return checkData(list);
    }
    public final CheckData checkData(List list){
        CheckData<DataDTO> checkData = new CheckData(list, tableInfo, this);
        return checkData;
    }

    public final KeyBuild keyBuild(){
        //用没有事务的 stringRedisTemplate,  支持一个线程中多次获得的序列号唯一
        return new KeyBuild(sysStringRedisTemplate,this);
    }
    public final KeyBuild keyBuild(String prefix, String dateFormat){
        return keyBuild().setPrefix(prefix).setDateFormat(dateFormat);
    }

    protected AbstractRestSession abstractRestSession;
    public void setRestSession(AbstractRestSession abstractRestSession){
        this.abstractRestSession=abstractRestSession;
    }

//    public CommonException.CommonString newCommonException(ResponseCode responseCode) {
//        return CommonException.newInstance(responseCode, abstractRestSession, this);
//    }
//    public CommonException.CommonString newCommonException(String responseCode) {
//        return CommonException.newInstance(responseCode, abstractRestSession, this);
//    }

    FormatConfig formatConfig;//格式化对象
    public FormatConfig formatConfig(){
        if (formatConfig == null){
            formatConfig = new FormatConfig();
        }
        return formatConfig;
    }
    public ChangeManager changeManager(DTO dto){
        ChangeManager changeManager = new ChangeManager(dto, formatConfig(), abstractRestSession){};
        return changeManager;
    }
    public ChangeManager changeManager(DTO dto, FormatConfig formatConfig){
        ChangeManager changeManager = new ChangeManager(dto, formatConfig, abstractRestSession){};
        return changeManager;
    }

}




//
//    public void save(List<DTO> list) {
//        if (list == null) return;
//        //beforeInit();
//        List<DTO> listInsert = list.stream().filter(beanDTO -> DbState.INSERT.equals(beanDTO.dataState())).collect(toList());//所有新增的记录
//        List<DTO> listUpdate = list.stream().filter(beanDTO -> DbState.UPDATE.equals(beanDTO.dataState())).collect(toList());//所有更新的记录
//        List<DTO> listDelete = list.stream().filter(beanDTO -> DbState.DELETE.equals(beanDTO.dataState())).collect(toList());//所有删除的记录
//
//        //listSave: 所有新增 更新 删除的记录
//        ArrayList<DTO> listAll = new ArrayList<>(listInsert.size()+listUpdate.size()+listDelete.size());
//        listAll.addAll(listInsert);
//        listAll.addAll(listUpdate);
//        listAll.addAll(listDelete);
//
//        //listInsertUpdate: 所有新增 修改 的记录
//        ArrayList<DTO> listInsertUpdate = new ArrayList<>(listInsert.size()+listUpdate.size());
//        listInsertUpdate.addAll(listInsert);
//        listInsertUpdate.addAll(listUpdate);
//
//        /** 调用"填充接口实现类的方法", 主要是 创建和修改 人/时间 */
//        if (fillDataHandler != null) {
//            fillDataHandler.fillData(tableInfo.getFillFields(), (List<DataDTO>)listInsertUpdate);
//        }
//
//        //调用新增 更新 删除SQL语句执行之前的内部方法
//        beforeSave3(listAll, listInsertUpdate, listDelete);
//
//        insertList(listInsert, false);
//        updateList(listUpdate, false);
//        deleteList(listDelete, false);
//
//        //调用新增 更新 删除SQL语句执行之后的内部方法
//        afterSave3(listAll, listInsertUpdate, listDelete);
//    }
//
//    private void beforeSave3(List<DTO> listAll, List<DTO> listInsertUpdate, List<DTO> listDelete){
//        beforeSave(listAll);
//        beforeInsertOrUpdate(listInsertUpdate);
//        listInsertUpdate.forEach((beanDTO) -> beforeInsertOrUpdate(beanDTO, beanDTO.dataState()));
//
//        //执行删除之前的逻辑
//        beforeDelete(listDelete);
//        listDelforEachete.forEach((beanDTO) -> {
//            beforeDelete(beanDTO);
//        });
//    }
//    private void afterSave3(List<DTO> listAll, List<DTO> listInsertUpdate, List<DTO> listDelete){
//        afterSave(listAll);
//        afterInsertOrUpdate(listInsertUpdate);
//        listInsertUpdate.forEach((beanDTO) -> afterInsertOrUpdate(beanDTO, beanDTO.dataState()));
//
//        //执行删除之前的逻辑
//        afterDelete(listDelete);
//        listDelete.forEach((beanDTO) -> {
//            afterDelete(beanDTO);
//        });
//
//
//        //缓存处理
//        for (DTO beanDTO : listAll) {
//            DTO  mergeDTO =beanDTO.merge();
//            dataCache.addCacheBySave(DbState.UPDATE.equals(beanDTO.dataState())?mergeDTO:beanDTO, openRedis);
//        }
//    }



//    public void flushdb() {
//        redisTemplate.execute(new RedisCallback<Object>() {
//            public String doInRedis(RedisConnection connection) throws DataAccessException {
//                connection.flushDb();
//
//                return "ok";
//            }
//        });
//    }
//    protected void saveRedis(List<DTO> list){
//        SessionCallback<Object> sessionCallback = new SessionCallback<Object>(){
//            @Override
//            public Object execute(RedisOperations operations) throws DataAccessException{
//                operations.multi();
//                for (DTO beanDTO : list) {
//                    redisHO.put(tableInfo.getTableName(), beanDTO.getId(), beanDTO);
//                }
//                Object val = operations.exec();
//                return val;
//            }
//        };
////        redisTemplate.setEnableTransactionSupport(true);
////        redisTemplate.multi();
//        redisTemplate.execute(sessionCallback);
////        redisTemplate.exec();

//        //将修改的记录同步到缓存  解决主从明细  只改明细，回写主表逻辑更新了修改时间 没有返回到前台的问题
//        if (restSession != null && restSession.outJson != null && restSession.outJson.datasets != null) {
//            Iterator iterator = restSession.outJson.datasets.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator.next();
//                DatasetEditor dataset = (DatasetEditor) entry.getValue();
//                Map<String, BaseBean> mapBeans = dataset.mapBeans;
//                if (mapBeans  !=  null && mapBeans.keySet().size() > 0 && clazz  ==  dataset.clazz
//                        && (dataset.getId().indexOf("dsData") > =  0 || dataset.getId().indexOf("dsLine") > =  0)) {
//                    for (DTO beanData : list) {
//                        BaseBean baseBean2 = mapBeans.get(beanData.getId());
//                        if (baseBean2  !=  null) {
//                            baseBean2.setModifyDate(beanData.getModifyDate());
//                            baseBean2.row.put(baseBean2.getDataset().getModifyDate(), beanData.getModifyDate());
//                        }
//                    }
//                }
//            }
//        }
//    }


//    /**
//     * 给修改的DTO添加原始的DTO
//     * @param listUpdate  //要更新的数据
//     * @param mapOriginDTO//返回 原始数据
//     * @param mapMergeDTO //返回 原始数据与要更新的数据的 合并
//     * @return
//     */
//    public void getMoreDTO(List<DTO> listUpdate, List<DTO> listQuery,
//                           Map<String, DTO> mapOriginDTO, Map<String, DTO> mapMergeDTO){
//        //转成map
//        Map<String, DTO> mapOriginDTO2 = listQuery.stream().collect(Collectors.toMap(DTO::dataId, Function.identity()));
//        mapOriginDTO.putAll(mapOriginDTO2);
//        listUpdate.forEach(dto -> dto.addOrigin(mapOriginDTO.get(dto.dataId()))); //添加原始的DTO

//
//        //转成map
//        Map<String, DTO> mapOriginDTO2 = listQuery.stream().collect(Collectors.toMap(DTO::dataId, Function.identity()));
//        mapOriginDTO.putAll(mapOriginDTO2);
//
//        //合并数据
//        if (mapMergeDTO != null) {
//            listUpdate.forEach(dto -> {
//                DTO MergeDTO = getEntity();
//                BeanUtil.copyBean(mapOriginDTO.get(dto.dataId()), MergeDTO);//将查询出来的DTO拷贝到MergeDTO
//                BeanUtil.copyBean(dto, MergeDTO);                          //将更新的DTO拷贝到MergeDTO
//                mapMergeDTO.put(dto.dataId(), MergeDTO);                    //传参式的返回 合并后的数据
//
//                dto.addMerge(MergeDTO);//添加合并的DTO
//                dto.addOrigin(mapOriginDTO.get(dto.dataId()));//添加原始的DTO
//            });
//        }
//    }
