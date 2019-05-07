package com.topfox.sql;

import com.topfox.common.DataDTO;
import com.topfox.data.TableInfo;
import com.topfox.misc.BeanUtil;

public class EntityDelete extends IEntitySql {
    private StringBuilder saveSql;

    public EntityDelete(TableInfo tableInfo) {
        super(tableInfo);
        saveSql = new StringBuilder();
    }

    public String getSql() {
        saveSql.setLength(0);
        saveSql.append("DELETE FROM ").append(tableInfo.getTableName());
        saveSql.append(where().getWhereSql());
        String sql = saveSql.toString();
        //init();
        return sql;
    }

    public EntityDelete deleteBatch() {
        clean();
        return this;
    }


    public String getDeleteByIdSql(DataDTO dto){
        clean();
        saveSql.append("DELETE FROM ").append(tableInfo.getTableName());

        //单Id,多Id字段 的条件处理
        tableInfo.getFieldsByIds().forEach((key, field)->{
            where().eq(key, BeanUtil.getValue(tableInfo, dto, key));
        });

        //版本号 字段值 为null 就不生成 version的条件
        String versionFieldName = tableInfo.getVersionFieldName();
        if (versionFieldName != null && dto.dataVersion() != null ){
            where().and(false).eq(versionFieldName, dto.dataVersion());
        }
        saveSql.append(where().getWhereSql());
        String sql=saveSql.toString();
        clean();




        return sql;
    }


    public String getDeleteByIdSql(Integer versionValue, Object... idValues){
        clean();
        saveSql.append("DELETE FROM ").append(tableInfo.getTableName());
        where().eq(tableInfo.getIdFieldName(), idValues);

        //版本号 字段值 为null 就不生成 version的条件
        String versionFieldName=tableInfo.getVersionFieldName();
        if (versionFieldName!=null && versionValue!=null ){
            where().and(false).eq(versionFieldName, versionValue);
        }
        saveSql.append(where().getWhereSql());
        String sql=saveSql.toString();
        clean();
        return sql;
    }

    protected void clean(){
        saveSql.setLength(0);//清空
        condition.clean();   //清空上次的查询条件
    }

}
