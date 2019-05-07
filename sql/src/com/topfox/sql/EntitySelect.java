package com.topfox.sql;

import com.topfox.data.TableInfo;
import com.topfox.misc.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntitySelect extends IEntitySql {
//    private TableInfo tableInfoQTO=null;
//    private Class<?> clazzQTO=null;
//    private Class<?> clazzDO=null;

    private Boolean isAppendAllFields=true;
    private Integer pageIndex=null, pageSize=null;
    private String fieldsSelect=null;
    private String fieldsOrderBy=null;
    private String fieldsGroupBy=null;
    private String fieldsHaving=null;
    private StringBuilder selectSql;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    public EntitySelect(Class<?> clazz){
        super(clazz);
        selectSql = new StringBuilder();
    }
    public EntitySelect(TableInfo tableInfo){
        super(tableInfo);
        selectSql = new StringBuilder();
//        this.tableInfo = tableInfo;
//        condition=new Condition(this,tableInfo);
    }

    protected void clean(){
        fieldsSelect =null;
        fieldsOrderBy=null;
        fieldsGroupBy=null;
        fieldsHaving =null;
        isAppendAllFields=true;
//        pageIndex=0;
//        pageSize=100;
        selectSql.setLength(0);
        condition.setQTO(null);
        if (condition.getQTO()!=null && condition.getQTO().getPageSize()!=null){
            pageSize=condition.getQTO().getPageSize();
        }
        condition.clean();//清空上次的查询条件
    }

//    public EntitySelect setQTO(Object qto) {
//        if (tableInfoQTO==null) {
//            clazzQTO=qto.getClass();
//            tableInfoQTO= new TableInfo(clazzQTO);
//        }
//        Map<String,Object> mapCondition=BeanUtil.bean2Map(tableInfoQTO, qto,false);
//        condition.eqMap(mapCondition);
//        this.pageIndex=DataHelper.parseInt(mapCondition.get(pageIndex));
//        this.pageSize =DataHelper.parseInt(mapCondition.get(pageSize));
//        return this;
//    }

    public EntitySelect select() {
        return select(null,true);
    }
    public EntitySelect select(String fields) {
        return select(fields,false);
    }
    public EntitySelect select(String fields,Boolean isAppendAllFields) {
        clean();//初始化

        if(Misc.isNull(fields)){
            //假如没有指定字段查询, 则默认查询所有字段
            isAppendAllFields=true;
        }
        this.fieldsSelect=fields;
        this.isAppendAllFields=isAppendAllFields;
        return this;
    }

    /**
     * 返回条件对象
     * @return
     */
    public Condition<EntitySelect> where(){
        return condition;
    }

    public EntitySelect orderBy(String fields){
        this.fieldsOrderBy=fields;
        return this;
    }

    public EntitySelect groupBy(String fields){
        this.fieldsGroupBy=fields;
        return this;
    }

    public EntitySelect having(String fields){
        this.fieldsHaving=fields;
        return this;
    }

    public EntitySelect setPage(Integer pageIndex, Integer pageSize) {
        this.pageIndex=pageIndex;
        this.pageSize=pageSize;
        return this;
    }

    public String getSelectCountSql() {
        selectSql.setLength(0);

        selectSql.append("SELECT count(1) ");
        if (fieldsGroupBy==null) {
            //selectSql.append("\nFROM ").append(tableInfo.getTableName())
            selectSql.append("\nFROM ").append(tableInfo.getTableName()).append(" a")
            .append(condition.getWhereSql());
            return selectSql.toString();
        }else{
            selectSql.append("\nFROM \n(select 1 FROM ").append(tableInfo.getTableName()).append(condition.getWhereSql());
            if (fieldsGroupBy!=null) {
                selectSql.append("\nGROUP BY ").append(fieldsGroupBy);
            }
            if (fieldsHaving!=null) {
                selectSql.append("\nHAVING ").append(fieldsHaving);
            }
            selectSql.append(") TEMP");
            return selectSql.toString();
        }
    }
    public String getSql(){
        selectSql.setLength(0);
        selectSql.append("SELECT ");
        if(fieldsSelect!=null){
            selectSql.append(fieldsSelect);
        }

        //生成查询的字段
        if (isAppendAllFields) {
            StringBuilder sbSelectFields = new StringBuilder();
            tableInfo.getFields().forEach((fieldName, field)->{
                if (field.getAnnotationName() && !field.getDbName().equals(field.getName())) return;
                sbSelectFields.append(field.getDbName()).append(",");
            });
//            for (String fieldName : tableInfo.getFields().keySet()) {
//                sbSelectFields.append(tableInfo.getColumn(fieldName)).append(",");
////                if (logger.isDebugEnabled()){
////                    sbSelectFields.append("\n\t");
////                }
//            }
            selectSql.append(sbSelectFields.substring(0, sbSelectFields.length() - (logger.isDebugEnabled()?1:1)));
        }
        selectSql.append("\nFROM ").append(tableInfo.getTableName()).append(" a");

        //得到条件
        selectSql.append(condition.getWhereSql());

        if (fieldsGroupBy!=null) {
            selectSql.append("\nGROUP BY ").append(fieldsGroupBy);
        }
        if (fieldsHaving!=null) {
            selectSql.append("\nHAVING ").append(fieldsHaving);
        }
        if (fieldsOrderBy!=null) {
            selectSql.append("\nORDER BY ").append(fieldsOrderBy);
        }

        //分页
        if (pageIndex!=null && pageSize!=null) {
            selectSql.append("\nLIMIT ").append(pageIndex).append(",").append(pageSize);
        }

        String sqlString=selectSql.toString();
        //init();//查询 初始化,便于下次使用
        return sqlString;
    }
}
