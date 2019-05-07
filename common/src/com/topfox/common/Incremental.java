package com.topfox.common;

public enum Incremental {

    //生成更新SQL 加减  值:addition +  / subtract -

    NONE("NONE", "无"),
    ADDITION("+", "递增"),
    SUBTRACT("-", "递减");

    private String code;
    private String name;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    Incremental(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
