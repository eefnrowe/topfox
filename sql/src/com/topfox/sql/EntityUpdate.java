package com.topfox.sql;

import com.topfox.common.DataDTO;
import com.topfox.common.Incremental;
import com.topfox.data.Field;
import com.topfox.data.TableInfo;
import com.topfox.misc.BeanUtil;

import java.util.Map;

public class EntityUpdate extends IEntitySql{//
    private StringBuilder saveSql;
    private Map<String, Field> fields;
    private StringBuilder updateSetValues=new StringBuilder();
//
//    public EntityUpdate(Class<?> clazz){
//        this.tableInfo = new TableInfo(clazz);
//        condition=new Condition(this,tableInfo);
//        saveSql = new StringBuilder();
//
//    }

    public EntityUpdate(TableInfo tableInfo){
        super(tableInfo);
        saveSql = new StringBuilder();
        fields=tableInfo.getFields();
    }

    /**
     * 返回条件对象
     * @return
     */
    public Condition<EntityUpdate> where(){
        return condition;
    }

    public String getSql(){
        saveSql.setLength(0);
        saveSql.append("UPDATE ")
                .append(tableInfo.getTableName())
                .append("\nSET ")
                .append(updateSetValues);
        saveSql.append(where().getWhereSql());

        String sql=saveSql.toString();
//        init();//对象数据归位,初始化为空
        return sql;
    }

    public EntityUpdate updateBatch(DataDTO bean) {
        return updateBatch(bean,false);
    }
    public EntityUpdate updateBatch(DataDTO bean, boolean isNullValue2Sql) {
        clean();
        buildUpdateSetValues(bean.mapSave(BeanUtil.bean2Map(bean,isNullValue2Sql)));
        return this;
    }

    public String getUpdateByIdSql(DataDTO bean) {
        return getUpdateByIdSqlBuild(bean.mapSave(BeanUtil.bean2Map(bean,false)));
    }

    public String getUpdateByIdSql(DataDTO bean, int updateMode) {
        return getUpdateByIdSqlBuild(bean.mapSave(BeanUtil.getChangeRowData(bean, updateMode)));
    }

    public String getUpdateByIdSql(DataDTO bean, boolean isNullValue2SetSql) {
        return getUpdateByIdSqlBuild(bean.mapSave(BeanUtil.bean2Map(bean,isNullValue2SetSql)));
    }
    private StringBuilder buildUpdateSetValues(Map<String,Object> mapValues){
        updateSetValues.setLength(0);

//        //过滤出修改的字段,顺序按照fields.  mapValues的keyset是无序的,以field为准
//        Object[] fieldsArray =fields.keySet().stream().filter((fieldName) ->
//                (mapValues.containsKey(fieldName) && !fieldName.equals(tableInfo.getIdFieldName()))//过滤条件
//        ).toArray();

        //处理版本号字段, 递增
        if (mapValues.containsKey(tableInfo.getVersionFieldName())){
            String versionColumn=tableInfo.getColumn(tableInfo.getVersionFieldName());
            updateSetValues.append(versionColumn).append("=").append(versionColumn).append("+1,");
        }

        fields.forEach((fieldName, field) -> {
            //  fieldName.equals(tableInfo.getIdFieldName())
            if (tableInfo.getFieldsByIds().containsKey(fieldName) || fieldName.equals(tableInfo.getVersionFieldName())) return;
            if (!mapValues.containsKey(fieldName)) return;

            if (field.getIncremental()== Incremental.ADDITION || field.getIncremental()== Incremental.SUBTRACT){
                // 解决更新SQL +-  addition +  / subtract -
                updateSetValues.append(
                        field.getDbName())//tableInfo.getColumn(fieldName)
                        .append("=")
                        .append(field.getDbName());
                updateSetValues.append(field.getIncremental().getCode()); // + - 符号
                BeanUtil.getSqlValue(fields.get(fieldName), fieldName, mapValues.get(fieldName), updateSetValues);
                updateSetValues.append(",");
            }else {
                updateSetValues.append(field.getDbName()).append("=");
                BeanUtil.getSqlValue(fields.get(fieldName), fieldName, mapValues.get(fieldName), updateSetValues);
                updateSetValues.append(",");
            }
        });
        updateSetValues.setLength(updateSetValues.length()-1);//去掉最后的逗号
        return updateSetValues;
    }
    public String getUpdateByIdSqlBuild(Map<String,Object> mapValues){
        saveSql.setLength(0);
        saveSql.append("UPDATE ")
                .append(tableInfo.getTableName())
                .append("\n\rSET ")
                .append(buildUpdateSetValues(mapValues));
        where().clean();

        //支持 多Id字段 where().eq(tableInfo.getIdColumn(), mapValues.get(tableInfo.getIdFieldName()));
        tableInfo.getFieldsByIds().forEach((key, field)->{
            where().eq(key, mapValues.get(key));
        });


        //版本号 字段值 为null 就不生成 version的条件
        String versionFieldName=tableInfo.getVersionFieldName();
        Object versionValue=mapValues.get(tableInfo.getVersionFieldName());
        if (versionFieldName!=null && versionValue!=null ){
            where().and(false).eq(tableInfo.getColumn(versionFieldName), versionValue);
        }
        saveSql.append(where().getWhereSql());
        String sql=saveSql.toString();
        clean();
        return sql;
    }

    protected void clean(){
        saveSql.setLength(0);
        updateSetValues.setLength(0);
        condition.clean();//清空上次的查询条件
    }
}
