package com.topfox.spring;

import com.topfox.common.*;
import com.topfox.data.TableInfo;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.Misc;
import com.topfox.sql.Condition;
import com.topfox.data.DataHelper;
import com.topfox.data.DbState;
import org.apache.ibatis.binding.BindingException;
import org.mybatis.spring.MyBatisSystemException;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdvancedService<QTO extends DataQTO, DTO extends DataDTO>
        extends SimpleService<QTO, DTO> {
    @Autowired SqlSessionTemplate sqlSessionTemplate;

    /**
     * 自定更新条件, 不是根据Id更新时用此方法
     */
    public int updateBatch(DTO beanDTO, Condition where){
        //beforeInit();
//        logger.debug()
        /** 原始数据 */
        List<DTO> listQuery = listObjects(tableInfo.getIdFieldName(), where);//查询出原始数据, 带缓存
        if (listQuery == null || listQuery.isEmpty()) {
            throw CommonException.newInstance(ResponseCode.DB_UPDATE_ERROR).text("查无需要更新的数据");
        }
        Map<String, DTO> mapOriginDTO = listQuery.stream().collect(
                Collectors.toMap(DTO::dataId, Function.identity())); //原始数据转成 Map

        /** 要更新的数据 注意: listUpdate 里面始终只有一条记录 */
        List<DTO> listUpdate = new ArrayList<>(1);
        /** 合并数据 */
        Map<String, DTO> mapMergeDTO = new HashMap<>(); //合并数据
        listQuery.forEach((beanQuery) ->{
            DTO newDTO = getEntity();
            newDTO.addOrigin(beanQuery);//set原始DTO
            BeanUtil.copyBean(beanQuery, newDTO);//合并1
            BeanUtil.copyBean(beanDTO, newDTO);  //合并2
            newDTO.dataVersion(newDTO.dataVersion()+1);
            listUpdate.add(newDTO);
        });

        //更新之前
        //这个方法不能调用, 否则冲突, 如果用户在 before 事件更改了DTO的值, 那更新如何处理,总不能一条条更新,这样就失去了批量更新的意义了
        //beforeSave2(listUpdate, DbState.UPDATE);

        //执行批量更新SQL
        int result = baseDao.executeByUpdateSql(entitySql.updateBatch(beanDTO).setWhere(where).getSql());
        
        afterSave2(listUpdate, DbState.UPDATE);/** 执行更新之后的方法, bean是合并后数据 */

        return result;
    }

    /**
     * 自定义删除条件, 即不是根据主键的删除才用这个
     * 先查询出来, 目的是得到每条记录的id, 然后根据Id删除redis缓存
     * @param where
     * @return
     */
    public int deleteBatch(Condition where){
        List<DTO> list = listObjects(where); //删除之前先查询出来, 不带缓存
        return deleteList(list);
    }

    /**
     * 1.设置分页参数的值. pageSize 的值小于等于0, 表示不分页, 设置最大值
     * 2.pageSize 没有值, 则设置默认值
     * 3.默认值都可以从SpringBoot配置文件中更改
     *
     * @param qto
     */
    private String initPage(QTO qto){
        int pageIndex    = qto.getPageIndex();       //获取第几页
        int pageSize    = sysConfig.getPageSize();   //每页返回条数
        int maxPageSize = sysConfig.getMaxPageSize();//每页返回最大值条数
        //以上为配置文件的信息

        if (qto.getPageSize() != null && qto.getPageSize() <= 0){
            qto.setPageSize(maxPageSize);
            //pageSize <= 0时, 表示不分页,  则默认从第一页查
            qto.setPageIndex(0);
        }
        if (qto.getPageSize() == null) {
            qto.setPageSize(pageSize);
        }

        TableInfo tableInfoQTO=TableInfo.get(qto.getClass());
        Map<String,Object> mapQTO = BeanUtil.bean2Map(qto);
        mapQTO.forEach((key, value)->{
            String valueString=value==null?"":value.toString().trim();//去掉参数值 左右的空格
            if (valueString !=null && valueString.indexOf("(")>=0) return;
            ArrayList<String> listValues= Misc.string2Array(valueString,",");
            if (key.endsWith("OrEq")){
                BeanUtil.setValue(tableInfoQTO, qto, key, Misc.getWhereOrSql(valueString, key.substring(0,key.length()-4)));
            }else if (key.endsWith("AndNe")) {
                BeanUtil.setValue(tableInfoQTO, qto, key, Misc.getWhereNotInSql(listValues, key.substring(0,key.length()-5)));
            }else if (key.endsWith("OrLike")) {
                BeanUtil.setValue(tableInfoQTO, qto, key, Misc.getWhereLikeOrSql(listValues, key.substring(0,key.length()-6)));
            }else if (key.endsWith("AndNotLike")) {
                BeanUtil.setValue(tableInfoQTO, qto, key, Misc.getWhereNotLikeSql(listValues, key.substring(0,key.length()-10)));
            }
//            else if (key.endsWith("pageIndex") && DataHelper.parseInt(value)>0) {
//                //第1页, SQL语句是0
//                BeanUtil.setValue(tableInfoQTO, qto, tableInfoQTO.getField(key), String.valueOf(DataHelper.parseInt(value)-1));
//            }
        });

        pageSize = qto.getPageSize();
        StringBuilder limit = new StringBuilder(" LIMIT ");
        limit.append(pageIndex>=2?(pageIndex-1)*pageSize:0).append(",").append(pageSize);

        qto.setLimit(limit.toString());
        return qto.getLimit();
    }

    public Response<List<DTO>> query(QTO qto, String sqlId, String index){
        String basePath = getClass().getName().replace(".service.", ".dao.").replace("Service", "")+"Dao";
        String methodQuery = basePath + "."+sqlId+(index == null?"":index);
        String methodQueryCount = basePath + "."+sqlId+"Count"+(sqlId+index == null?"":index);

//        Integer maxPageSize = DataHelper.parseInt(environment.getProperty("top.maxPageSize"));
//        if (pageSize != null && pageSize  <=  0) qto.setPageSize(maxPageSize <= 0?20000:maxPageSize); //pageSize <= 0, 表示不分页查询时, 最多只查询20000条
        initPage(qto);
        Integer pageSize = qto.getPageSize();
        Map<String,Object> maoQTO = new HashMap<>();

        BeanUtil.bean2Map(maoQTO, TableInfo.get(qto.getClass()), qto, false, true);
        List<DTO> list = sqlSessionTemplate.selectList(methodQuery, maoQTO);

        if (pageSize  <=  0){
            return new Response(list, list.size());
        }

        return new Response(list, sqlSessionTemplate.selectOne(methodQueryCount, maoQTO));
    }

    public Response<List<DTO>> list(QTO qto){
        //优先执行 mapper.xml 对应的SQL sqlId = listObjects, 没有则类库自动生成当前表所有字段的查询SQL
        Response<List<DTO>> response;
        try {
            response = query(qto, "list", "");
        }catch(BindingException e){
            response =  list2(qto);//dao文件没有对应的方法, 则 报错
        }catch(MyBatisSystemException e){
            if(e.getMessage().indexOf("Mapped Statements collection does not contain value for")>0){
                //mapper.xml没有对应的sqlId, 则 报错
                response =  list2(qto);
            }else {
                throw e;
            }
        }

        //查询结果放入缓存
        response.getData().forEach(dto->
                dataCache.addCacheBySelected(dto, sysConfig.isOpenRedis())
        );

        return response;
    }

    private Response<List<DTO>> list2(QTO qto){
        logger.debug("{}对应的mapper.xml 无对应的sqlId, 系统自动生成SQL", sysConfig.getLogPrefix());
        List<DTO> list = listObjects(null, true, where().setQTO(qto));
        return new Response(list, selectCount(where().setQTO(qto)));
    }

    public Response<List<DTO>> query1(QTO qto){
        return query(qto, "query", "1");
    }
    public Response<List<DTO>> query2(QTO qto){
        return query(qto, "query", "2");
    }
    public Response<List<DTO>> query3(QTO qto){
        return query(qto, "query", "3");
    }
    public Response<List<DTO>> query4(QTO qto){
        return query(qto, "query", "4");
    }
    public Response<List<DTO>> query5(QTO qto){
        return query(qto, "query", "5");
    }
    public Response<List<DTO>> query6(QTO qto){
        return query(qto, "query", "6");
    }
    public Response<List<DTO>> query7(QTO qto){
        return query(qto, "query", "7");
    }
    public Response<List<DTO>> query8(QTO qto){
        return query(qto, "query", "8");
    }
    public Response<List<DTO>> query9(QTO qto){
        return query(qto, "query", "9");
    }

}
