package com.topfox.sql;


public enum SqlOperator {

//    ne	不等于<>
//    gt	大于>
//    ge	大于等于>=
//    lt	小于<
//    le	小于等于<=
//    like	模糊查询 LIKE

    LIKE(" LIKE ", "模糊查询like"),
    NOTLIKE(" NOT LIKE ", "模糊查询非 not like"),
    EQ(" = ", "等于="),
    GE(" >= ", "大于等于>="),
    GT(" > ", "大于>"),
    LT(" < ", "小于<"),
    LE(" <= ", "小于等于<="),
    NE(" <> ", "不等于<>"),
    BETWEEN(" BETWEEN ", "BETWEEN"),
    ;

    private String code;
    private String value;

    SqlOperator(String code, String value) {
        this.code = code;
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static String getValueByCode(String code) {
        if (code != null) {
            for (SqlOperator current : SqlOperator.values()) {
                if (current.getCode().equals(code)) {
                    return current.getValue();
                }
            }
        }
        return null;
    }

    public static SqlOperator getByValue(String value) {
        if (value != null) {
            for (SqlOperator current : SqlOperator.values()) {
                if (current.getValue().equals(value)) {
                    return current;
                }
            }
        }
        return null;
    }
}