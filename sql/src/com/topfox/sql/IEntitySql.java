package com.topfox.sql;

import com.topfox.data.TableInfo;

/**
 *
 */
public abstract class IEntitySql{
    protected TableInfo tableInfo;
    protected Condition condition;

    public IEntitySql(Class<?> clazz){
        tableInfo = TableInfo.get(clazz);
        condition=new Condition(this,tableInfo);
    }

    public IEntitySql(TableInfo tableInfo) {
        this.tableInfo=tableInfo;
        condition = new Condition(this, tableInfo);
    }

    /**
     * 返回条件对象
     *
     * @return
     */
    public Condition where() {
        return condition;
    }

    public Condition setWhere(Condition where){
        condition=where;
        condition.setEntitySql(this);
        return condition;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

//    public abstract T setPage(Integer pageIndex, Integer pageSize);

    public abstract String getSql();
    protected abstract void clean();
    
}