package com.topfox.sql;


import com.topfox.common.CommonException;
import com.topfox.common.DataDTO;
import com.topfox.data.DbState;
import com.topfox.data.Field;
import com.topfox.data.TableInfo;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.Misc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntitySql {//extends EntitySelect
    private EntitySelect entitySelect; //实现查询SQL语句生成
    private EntityUpdate entityUpdate; //实现更新SQL语句生成
    private EntityDelete entityDelete; //实现删除SQL语句生成
    private TableInfo tableInfo;//表机构对象

    private String dbState;//DbState: i新增 u修改  d删除  n无/查询

    //应该改成线程容器
    static ConcurrentHashMap<Class<?>,EntitySql> mapEntitySql;
    static {
        mapEntitySql=new ConcurrentHashMap();
    }

    /**
     * 不保证多线程安全.
     * 多线程高并发情况下, 应该使用 RestSessionHandler.getEntitySql 获得EntitySql
     * @param clazz
     * @return
     */
    public static EntitySql get(Class<?> clazz){
        EntitySql entitySql= mapEntitySql.get(clazz);
        if (entitySql==null) {
            entitySql = new EntitySql(clazz);
            mapEntitySql.put(clazz,entitySql);
        }

        return entitySql;
    }

    /**
     * 私有构造函数
     * @param clazz
     */
    public EntitySql(Class<?> clazz){
        tableInfo = TableInfo.get(clazz);
    }

    /**
     * 初始化查询,更新,删除的对象,为空则创建
     * @param dbState
     */
    private void init(String dbState){
        this.dbState=dbState;
        if (DbState.NONE.equals(dbState)) {
            getEntitySelect().clean();
        }else if (DbState.UPDATE.equals(dbState)) {
            getEntityUpdate().where().clean();
        }else if (DbState.DELETE.equals(dbState)) {
            getEntityDelete().where().clean();
        }
    }

    public EntityUpdate getEntityUpdate() {
        if(entityUpdate==null){
            entityUpdate=new EntityUpdate(tableInfo);
        }
        return entityUpdate;
    }

    public EntityDelete getEntityDelete() {
       if(entityDelete==null){
            entityDelete=new EntityDelete(tableInfo);
       }
       return entityDelete;
    }

    public EntitySelect getEntitySelect() {
        if(entitySelect==null){
            entitySelect=new EntitySelect(tableInfo);
        }
        return entitySelect;
    }

    /**
     * 获得 有自定义条件的 查询 更新  删除的SQL语句
     * @return
     */
    public String getSql(){
        if (dbState== DbState.NONE && entitySelect!=null){
            return entitySelect.getSql();//查询的SQL
        }else if (dbState== DbState.UPDATE && entityUpdate!=null){
            return entityUpdate.getSql();
        }else if (dbState== DbState.DELETE && entityDelete!=null){
            return entityDelete.getSql();
        }else{
            throw CommonException.newInstance("SQL_002").text("无法生成明确的SQL");
        }
    }

    /**
     * 获得查询的总行数
     */
    public String getCountSql(){
        if (dbState== DbState.NONE && entitySelect!=null){
            return entitySelect.getSelectCountSql();
        }else{
            throw CommonException.newInstance("SQL_003").text("没有生成查询SQL");
        }
    }
    public EntitySelect select() {
        init(DbState.NONE);
        return entitySelect.select(null,true);
    }

    /**
     * 只查询指定的字段
     * @param fields 指定返回的字段, 或者自定义计算字段, 带函数的字段
     * @return
     */
    public EntitySelect select(String fields) {
        init(DbState.NONE);
        return entitySelect.select(fields,false);
    }

    /**
     * @param fields 自定义计算字段, 带函数的字段
     * @param isAppendAllFields  是否追加所有字段.
     * @return
     */
    public EntitySelect select(String fields,Boolean isAppendAllFields) {
        init(DbState.NONE);
        return entitySelect.select(fields,isAppendAllFields);
    }

    /**
     * 自定义的更新SQL的条件,为空的字段,不更新
     * @param bean
     * @return
     */
    public EntityUpdate updateBatch(DataDTO bean) {
        init(DbState.UPDATE);
        return entityUpdate.updateBatch(bean,false);
    }

    /**
     * 自定义的更新SQL的条件
     * @param bean
     * @param isNullValue2Sql 字段值为null是否要生成更新SQL, true 要(所有字段) false否(生成有值字段的SQL)
     * @return
     */
    public EntityUpdate updateBatch(DataDTO bean, boolean isNullValue2Sql) {
        init(DbState.UPDATE);
        //bean.setState(DbState.UPDATE.getCode());
        entityUpdate.updateBatch(bean,isNullValue2Sql);
        //entityUpdate.buildUpdateSetValues(BeanUtil.bean2Map(tableInfo,bean,isNullValue2Sql));
        return entityUpdate;
    }

    /**
     * 根据Id更新DO, 为空的字段不更新
     * @param bean
     * @return
     */
    public String getUpdateByIdSql(DataDTO bean) {
        init(DbState.UPDATE);
        return entityUpdate.getUpdateByIdSql(bean);
    }

    /**
     * 根据Id更新DO, 为空的字段不更新
     * @param bean
     * @param isNullValue2SetSql 字段值为null是否要生成更新SQL, true 要(所有字段) false否(生成有值字段的SQL)
     * @return
     */
    public String getUpdateByIdSql(DataDTO bean, boolean isNullValue2SetSql) {
        init(DbState.UPDATE);
        return entityUpdate.getUpdateByIdSql(bean,isNullValue2SetSql);
    }

    /**

     * @param bean
     *
     * @param updateMode
     * updateMode重要参数: 更新时DTO序列化策略 和 更新SQL生成策略
     * # 1 时, service的DTO=提交的数据.               更新SQL 提交的数据不等null的字段 的字段生成 set field=value
     * # 2 时, service的DTO=修改前的原始数据+提交的数据. 更新SQL (当前值 != 原始数据)    的字段生成 set field=value
     * # 3 时, service的DTO=修改前的原始数据+提交的数据. 更新SQL (当前值 != 原始数据 + 提交的数据不等null) 的字段生成 set field=value
     *
     * @return
     */
    public String getUpdateByIdSql(DataDTO bean, int updateMode ) {
        init(DbState.UPDATE);
        return entityUpdate.getUpdateByIdSql(bean, updateMode);
    }

    /**
     * 自定义删除的条件
     * @return
     */
    public EntityDelete deleteBatch() {
        init(DbState.DELETE);
        return entityDelete.deleteBatch();
    }

    /**
     * 根据bean的idValue删除, 如果bean里面版本不为null,还要增加乐观锁
     * @param bean
     * @return
     */
    public String getDeleteByIdSql(DataDTO bean) {
        init(DbState.DELETE);
        return entityDelete.getDeleteByIdSql(bean);
    }

    public String getDeleteByIdSql(Object... idValues) {
        init(DbState.DELETE);
        return entityDelete.getDeleteByIdSql(null,idValues);
    }
//    public String getDeleteByIdSql(String... idValues) {
//        init(DbState.DELETE);
//        return entityDelete.getDeleteByIdSql(null,idValues);
//    }
    public String getDeleteByIdSql(String idValues) {
        init(DbState.DELETE);
        return entityDelete.getDeleteByIdSql(null, Misc.string2Array(idValues,","));
    }
//    public String getDeleteByIdSql(Integer... idValues) {
//        init(DbState.DELETE);
//        return entityDelete.getDeleteByIdSql(null,idValues);
//    }


    public String getDeleteByIdSql(Object idValue, Integer versionValue){
        init(DbState.DELETE);
        return entityDelete.getDeleteByIdSql(versionValue,idValue);
    }

//    public String getDeleteByIdSql(Integer idValue, Integer versionValue){
//        init(DbState.DELETE);
//        return entityDelete.getDeleteByIdSql(versionValue,idValue);
//    }

    /**
     *
     * @param idValue
     * @param versionValue 指定版本号, !=null时, 增加乐观锁, 即: WHERE version= xx
     * @return
     */
    public String getDeleteByIdSql(String idValue, Integer versionValue){
        init(DbState.DELETE);
        return entityDelete.getDeleteByIdSql(versionValue,idValue);
    }


//    private String getDeleteByIdSql(Number idValue, Integer versionValue){
//        init(DbState.DELETE);
//        return entityDelete.getDeleteByIdSql(idValue,versionValue);
//    }
//    /**
//     * 根据指定的Id和版本号删除
//     * @param idValue
//     * @param versionValue
//     * @return
//     */
//    public String getDeleteByIdSql(String idValue, Integer versionValue){
//        return entityDelete.getDeleteByIdSql(idValue,versionValue);
//    }

    //插入SQL语句生成
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String getInsertSql(DataDTO bean) {
        //通过 BeanUtil.bean2Map 数据
        return getInsertSql(bean, false);
    }
    /**
     * 依据Bean 生成插入SQL
     * @param bean
     * @param isNullValueSql 字段值为null是否要生成SQL  true 要(所有字段) false否(生成有值字段的SQL)
     * @return
     */
    public String getInsertSql(DataDTO bean, Boolean isNullValueSql) {
        List<DataDTO> list = new ArrayList<>(1);
        list.add(bean);
        return getInsertSql(list,isNullValueSql);
        //return getInsertSql(((DataDTO)bean).mapSave(BeanUtil.bean2Map(bean,isNullValueSql)));
    }

//    /**
//     * 依据Map 生成插入SQL
//     * @param mapData
//     * @return
//     */
//    public String getInsertSql(Map<String,Object> mapData) {
//        Map<String, Field> fields = tableInfo.getFields();
//
//        StringBuilder stringBuilder = new StringBuilder();
//        StringBuilder stringBuilderValues = new StringBuilder();
//        //开始拼SQL语句
//        stringBuilder.append("INSERT INTO ").append(tableInfo.getTableName()).append("(");
//        for (String fieldName : mapData.keySet()){
//            Field field=fields.get(fieldName);
//            stringBuilder.append(fieldName).append(",");//拼接插入的列名
//            BeanUtil.getSqlValue(field, fieldName,mapData.get(fieldName),stringBuilderValues);//拼接插入的数据
//            stringBuilderValues.append(",");//每个值之间用逗号 隔开
//        }
//        stringBuilder.setLength(stringBuilder.length()-1);            //去掉最后一个逗号
//        stringBuilderValues.setLength(stringBuilderValues.length()-1);//去掉最后一个逗号
//
//        stringBuilder.append(")");
//        stringBuilder.append("\n\r VALUES(").append(stringBuilderValues).append(")");
//        return stringBuilder.toString();
//    }

    /**
     * 依据Bean的所有字段 构建批量插入SQL
     * @param list
     * @return
     */
    public String getInsertSql(List<DataDTO> list) {
        return getInsertSql(list,false);
    }
    /**
     * 构建批量插入SQL
     * @param list
     * @param isNullValueSql 字段值为null是否要生成SQL  true 要(所有字段) false否(生成有值字段的SQL)
     * @return
     */
    public String getInsertSql(List<DataDTO> list, Boolean isNullValueSql) {
        Map<String,Field> fields = tableInfo.getFields();//得到表 DTO 的所有字段
        if (isNullValueSql==false) {
            //获得多行数据有值(不为Null)的字段总集合
            Set<String> setNotNullValueFieldNames = new HashSet<>();
            for (DataDTO bean : list) {
                setNotNullValueFieldNames.addAll(BeanUtil.bean2Map(bean, isNullValueSql).keySet());
            }

            //有值的字段 必须在DO定义的字段中,不在则排除掉. 结果放入fields2中
            Map<String, Field> fields2 = new LinkedHashMap<>();
            for (String key : fields.keySet()) {
                if (setNotNullValueFieldNames.contains(key)) {
                    fields2.put(key, fields.get(key));
                }
            }
            fields=fields2;//将有值的字段变量 传递给fields变量
        }
        StringBuilder stringBuilder = new StringBuilder();

        //拼接插入的列名
        stringBuilder.append("INSERT INTO ").append(tableInfo.getTableName()).append("(");
        fields.forEach((key,field)->{
            if (field.getAnnotationName() && !field.getDbName().equals(field.getName())) return;
            stringBuilder.append(field.getDbName()).append(",");//拼接插入的列名
        });
//        for (String key : fields.keySet()){ //遍历所有 🈶值得字段
//            stringBuilder.append(tableInfo.getColumn(key)).append(",");//拼接插入的列名
//        }
        stringBuilder.setLength(stringBuilder.length()-1);//去掉最后一个逗号
        stringBuilder.append(")");

        //拼接插入的数据VALUES
        String rowNoFieldName = tableInfo.getRowNoFieldName();
        stringBuilder.append("\nVALUES");
        for (DataDTO bean : list) {
            Map<String,Object> mapSave = new HashMap<>();
            if (Misc.isNotNull(rowNoFieldName)) {
                mapSave.put(rowNoFieldName, bean.dataRowId());
            }
            stringBuilder.append("(");
            for (String fieldName : fields.keySet()){
                Field field = fields.get(fieldName);
                Object value = BeanUtil.getValue(tableInfo, bean, field);
                BeanUtil.getSqlValue(field, fieldName, value, stringBuilder);//拼接插入的数据
                stringBuilder.append(","); //每个值之间用逗号 隔开

                mapSave.put(field.getName(), value);
            }
            stringBuilder.setLength(stringBuilder.length()-1);
            stringBuilder.append("),\n ");
            bean.mapSave(mapSave); //输出到 前端(调用方)用
        }

        stringBuilder.setLength(stringBuilder.length()-3);

        return stringBuilder.toString();
    }
}
