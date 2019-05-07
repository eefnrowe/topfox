package com.topfox.sql;

import com.topfox.common.CommonException;
import com.topfox.common.DataQTO;
import com.topfox.common.ResponseCode;
import com.topfox.data.DataHelper;
import com.topfox.data.DataType;
import com.topfox.data.Field;
import com.topfox.data.TableInfo;
import com.topfox.misc.BeanUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Condition<B extends IEntitySql> {
    private StringBuilder where;
    private TableInfo tableInfoQTO;
    private TableInfo tableInfoDTO;
    private Boolean isAll=true;

    //    private Class<?> clazzDO=null;
    private Class<?> clazzQTO=null;
    private B entitySql;


    public Condition(B entitySql, TableInfo tableInfo){
        this.tableInfoDTO =tableInfo;
        this.entitySql=entitySql;

        this.where=new StringBuilder();
    }
    public Condition(Class<?> clazz){
        this.tableInfoDTO = TableInfo.get(clazz);
        this.where=new StringBuilder();
    }
//    public Condition(TableInfo tableInfo){
//        this.tableInfo = tableInfo;
//        this.where=new StringBuilder();
//    }
    public static Condition create(Class<?> clazzDO){
        return new Condition(clazzDO);
    }
//    public static Condition create(TableInfo tableInfo){
//        return new Condition(tableInfo);
//    }


    public void setEntitySql(B entitySql) {
        this.entitySql = entitySql;
    }

    private Field getField(String fieldName){
        Field fieldDTO,fieldQTO=null;
        fieldDTO= tableInfoDTO.getFields().get(fieldName);
        if (tableInfoDTO !=null){
            fieldQTO= tableInfoDTO.getFields().get(fieldName);
        }

        if (fieldDTO!=null && fieldQTO!=null && fieldDTO.getDataType()!=fieldQTO.getDataType()){
            throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID)
                    .text(fieldName+"在"+tableInfoQTO.clazzEntity.getName()+" 和 "+clazzQTO.getName()+" 中类型不一致");
        }
        if (fieldDTO!=null){
            return fieldDTO;
        }
//        else{
//            throw new RuntimeException(tableInfoDTO.clazzEntity.getName()+"."+fieldName+" 不存在");
//        }

        return null;
    }

//    private Field checkFieldExist(String fieldName){
//        Field field=tableInfo.getFields().get(fieldName);
//        if(field==null) {
//            throw new RuntimeException(tableInfo.clazzEntity.getName()+"."+fieldName+" 不存在");
//        }
//
//        return field;
//    }

    DataQTO qto;
    public DataQTO getQTO(){
        return this.qto;
    }
    public Condition<B> setQTO(DataQTO qto) {
        this.qto=qto;
        if (qto==null) return this;
        if (tableInfoQTO==null) {
            clazzQTO=qto.getClass();
            tableInfoQTO=TableInfo.get(clazzQTO);
        }
        Map<String,Object> mapCondition= BeanUtil.bean2Map(qto,false);

        tableInfoQTO.getFields().keySet().forEach((key)->{
            if(!mapCondition.containsKey(key)  || key.equals("allowNull")
                    || key.equals("pageIndex") || key.equals("pageSize")
                    | key.equals("limit")
            ){
                return;
            }

            Field field;
            if (key.endsWith("From")){
                String fieldName =key.substring(0,key.length()-4);
                field= tableInfoDTO.getFields().get(fieldName);
                if (field!=null){
                    ge(fieldName,mapCondition.get(key));//大于等于
                }
            }else if (key.endsWith("To")){
                String fieldName =key.substring(0,key.length()-2);
                field= tableInfoDTO.getFields().get(fieldName);
                if (field!=null){
                    le(fieldName,mapCondition.get(key));//小于等于
                }
            }else{

                field= tableInfoDTO.getFields().get(key);
                if (tableInfoDTO.getFieldsByIds().size()>1 && tableInfoDTO.getFieldsByIds().containsKey(field.getName())){
                    //多 @Id 时, Id字段 用 =
                    eq(field.getName(), mapCondition.get(key));// eq 等于= 如 fieldName = 'value'
                }else if (field!=null && field.getDataType()== DataType.STRING){
                    like(field.getName(), mapCondition.get(key).toString());// eq 等于= 如 fieldName = 'value'
                }else{
                    eq(field.getName(), mapCondition.get(key));// eq 等于= 如 fieldName = 'value'
                }
            }
        });

        return this;
    }

    public B endWhere(){
        return this.entitySql;
    }

    public String getSql(){
        return this.entitySql.getSql();
    }

    public Condition<B> clean(){
        where.setLength(0);
        qto=null;
        isAll=true;//false 按时设置为true
        return this;
    }

    public Condition<B> all(){
        isAll=true;
        return this;
    }

    public Condition<B> add(String sql){
        if (sql.indexOf(" AND ") <0 && sql.indexOf(" OR ") <0
                && sql.indexOf(" AND(") <0 && sql.indexOf(" OR(") <0) {

        }
        where.append(sql);
        return this;
    }

    public String getWhereSql(){
        if(where.length()==0){
            return isAll?"\nWHERE 1=1":"\nWHERE 1=2";
        }else {
            return "\nWHERE " + where.toString();
        }
    }

    /**
     * like 值左右增加 %, 如 fieldName like '%value%'
     * @param fieldName
     * @param values
     * @return
     */
    public Condition<B> like(String fieldName, String... values){
        return like(fieldName,SqlOperator.LIKE,true,true,values);
    }

    /**
     * like 值左边增加 %, 如 fieldName like '%value'
     * @param fieldName
     * @param values
     * @return
     */
    public Condition<B> likeLeft(String fieldName, String... values){
        return like(fieldName,SqlOperator.LIKE,true,false,values);
    }
    /**
     * like 值右边边增加 %, 如 fieldName like 'value%'
     * @param fieldName
     * @param values
     * @return
     */
    public Condition<B> likeRight(String fieldName, String... values){
        return like(fieldName,SqlOperator.LIKE,false,true,values);
    }

    /**
     * not like 值左右增加 %, 如 fieldName like '%value%'
     * @param fieldName
     * @param values
     * @return
     */
    public Condition<B> notLike(String fieldName, String... values){
        return like(fieldName,SqlOperator.NOTLIKE,true,true,values);
    }

    /**
     * not like 值左边增加 %, 如 fieldName like '%value'
     * @param fieldName
     * @param values
     * @return
     */
    public Condition<B> notLikeLeft(String fieldName, String... values){
        return like(fieldName,SqlOperator.NOTLIKE,true,false,values);
    }
    /**
     * not like 值右边边增加 %, 如 fieldName like 'value%'
     * @param fieldName
     * @param values
     * @return
     */
    public Condition<B> notLikeRight(String fieldName, String... values){
        return like(fieldName,SqlOperator.NOTLIKE,false,true,values);
    }

    /**
     * 等于= 如 fieldName = 'value'
     * @param fieldName 字段名
     * @param value 值
     * @return 返回自己
     */
    public Condition<B> eq(String fieldName, Object value){
        return operator(fieldName,SqlOperator.EQ,value);
    }
    public Condition<B> eq(String fieldName, Set set){
        return operator(fieldName,SqlOperator.EQ,set.toArray());
    }
    public Condition<B> eq(String fieldName, List list){
        return operator(fieldName,SqlOperator.EQ,list.toArray());
    }
    /**
     * 等于= 如 (fieldName = 'value1' or ieldName = 'value2' or ieldName = 'value2' ...)
     * @param fieldName
     * @param values
     * @return
     */
    public Condition<B> eq(String fieldName, Object... values){
        return operator(fieldName,SqlOperator.EQ,values);
    }


    private Condition<B> eqMap(Map<String,Object> params) {
        for (String key:params.keySet()){
            eq(key,params.get(key));// eq 等于= 如 fieldName = 'value'
        }
        return this;
    }
    private Condition<B> eq(String[] keys,Object[] values) {
        if ( keys==null || keys==null)
            throw new RuntimeException("keys 与 values 不能为空");
        if ( keys!=null && values!=null && keys.length!=values.length )
            throw new RuntimeException("keys 与 values 的个数必须相等");

        for (int i=0;i<keys.length;i++){
            eq(keys[i],values[i]);// eq 等于= 如 fieldName = 'value'
        }
        return this;
    }

    /**
     * 不等于<>
     * @param fieldName
     * @param values
     * @return
     */
    public Condition<B> ne(String fieldName, Object... values){
        return operator(fieldName,SqlOperator.NE,values);

    }

    /**
     * 大于等于>=
     * @param fieldName
     * @param value
     * @return
     */
    public Condition<B> ge(String fieldName, Object value){
        return operator(fieldName,SqlOperator.GE,value);

    }

    /**
     * 大于>
     * @param fieldName
     * @param value
     * @return
     */
    public Condition<B> gt(String fieldName, Object value){
        return operator(fieldName,SqlOperator.GT,value);

    }

    /**
     * 小于<
     * @param fieldName
     * @param value
     * @return
     */
    public Condition<B> lt(String fieldName, Object value){
        return operator(fieldName,SqlOperator.LT,value);

    }

    /**
     * 小于等于<=
     * @param fieldName
     * @param value
     * @return
     */
    public Condition<B> le(String fieldName, Object value){
        return operator(fieldName,SqlOperator.LE,value);

    }

    public Condition<B> isNull(String fieldName){
        andOr();
        where.append(tableInfoDTO.getColumn(fieldName)).append(" is null");
        return this;
    }
    public Condition<B> isNotNull(String fieldName){
        andOr();
        where.append(tableInfoDTO.getColumn(fieldName)).append(" is not null");
        return this;
    }


    private Condition<B> operator(String fieldName, SqlOperator sqlOperator, Set values) {
        return operator(fieldName,sqlOperator,values.toArray());
    }
    private Condition<B> operator(String fieldName, SqlOperator sqlOperator, Object... values) {
        /**
         * fieldName 允许传入Mysql函数,如 LEGTH(name), CONCAT(name,sex)
         */
        Field field=getField(fieldName);
        int index=0, size=values.length;
        if (size>=1) {
            andOr();
            where.append("(");

            for (Object value : values) {
                index++;
                where.append(tableInfoDTO.getColumn(fieldName)).append(sqlOperator.getCode());
                BeanUtil.getSqlValue(field, fieldName, value, where);
                if (index < size) {
                    where.append(sqlOperator==SqlOperator.NE||sqlOperator==SqlOperator.NOTLIKE?" AND ":" OR ");
                }
            }
            where.append(")");
        }
        return this;
    }

//    private Condition<B> operator(String fieldName, SqlOperator sqlOperator, Object value){
//        Field field=checkFieldExist(fieldName);
//        andOr();
//        where.append(fieldName).append(sqlOperator.getCode());
//        BeanUtil.builderFieldSql(field, value, where);
//        return this;
//    }

    /**
     * fieldName >= valueFrom and fieldName <= valueTo
     * @param fieldName
     * @param valueFrom
     * @param valueTo
     * @return
     */
    public Condition<B> between(String fieldName, Object valueFrom,Object valueTo){
        Field field=getField(fieldName);
//        if(field==null) {
//            throw new RuntimeException(tableInfoDTO.clazzEntity.getName()+"."+fieldName+" 不存在");
//        }
        andOr();
        where.append(tableInfoDTO.getColumn(fieldName)).append(SqlOperator.BETWEEN.getCode());
        BeanUtil.getSqlValue(field, fieldName,valueFrom, where);
        where.append(" AND ");
        BeanUtil.getSqlValue(field, fieldName,valueTo, where);

        return this;
    }

    private Condition<B> like(String fieldName,SqlOperator likeType,boolean isLeft,boolean isRight, String... values) {
        Field field=getField(fieldName);
//        if(field==null) {
//            throw new RuntimeException(tableInfoDTO.clazzEntity.getName()+"."+fieldName+" 不存在");
//        } else
        if(field != null && field.getDataType() != DataType.STRING){
            throw new RuntimeException(fieldName+" 的类型不是String, 不能进行增加 like 条件");
        }
        int index=0, size=values.length;
        if (size>=1){
            andOr();
            where.append("(");
            for (Object value:values){
                index++;
                where.append(tableInfoDTO.getColumn(fieldName)).append(likeType.getCode())
                        .append(isLeft?"'%":"'")
                        .append(DataHelper.parseString2(value))
                        .append(isRight?"%'":"'");
                if (index < size) {
                    where.append(" OR ");
                }
            }
            where.append(")");
        }

        return this;
    }



    private boolean isOr=false;// true就拼写 or ; false就拼写 and
    private Condition<B> andOr(){
        if(isOr) return or();
        else return  and();
    }

    public Condition<B> or() {
        return or(false);
    }
    /**
     * 是否增加左括号
     * @param isParentheses
     * @return
     */
    private Condition<B> or(boolean isParentheses){
        return andOrOr(isParentheses?"\n  OR (":" OR ");
    }
    public Condition<B> or(String parentheses){
        return or(parentheses!=null && parentheses.indexOf("(")>=0);
    }


    public Condition<B> and() {
        return and(true);
    }

    public Condition<B> and(String parentheses){
        return andOrOr((parentheses!=null && parentheses.indexOf("(")>=0)?"\n  AND (":"\n  AND ");
    }
    /**
     * 是否换行
     * @param isNewLine
     * @return
     */
    public Condition<B> and(boolean isNewLine){
        return andOrOr(isNewLine?"\n  AND ":" AND ");
    }
    private Condition<B> andOrOr(String conditionAndOr){
        int size=where.length();
        if (size > 0) {
            String value=where.substring(size-6);//得到最后一个索引的值
            if (value.indexOf(" AND ") <0 && value.indexOf(" OR ") <0 && value.indexOf(" AND(") <0 && value.indexOf(" OR(") <0) {
                where.append(conditionAndOr);
            }
        }
        isOr=conditionAndOr.indexOf("OR")>=0;
        return this;
    }

    @Override
    public String toString() {
        return getWhereSql();
    }
}