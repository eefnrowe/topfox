package com.topfox.data;

/**
 * 生成SQL 是否驼峰转下划线 默认 OFF
 *  1. OFF 关闭
 *  2. ON-UPPER 打开并大写
 *  3. ON-LOWER 打开并小写
 * 配置 top.service.sql-camel-to-underscore
 */
public enum SqlUnderscore {
    OFF(0, "OFF"),
    ON_UPPER(1, "ON-UPPER"),
    ON_LOWER(2, "ON-LOWER");

    private Integer code;
    private String value;

    SqlUnderscore(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static String getValueByCode(Integer code) {
        if (code != null) {
            for (DataType current : DataType.values()) {
                if (current.getCode().equals(code)) {
                    return current.getValue();
                }
            }
        }
        return null;
    }

    public static SqlUnderscore getByValue(String value) {
        if (value != null) {
            for (SqlUnderscore current : SqlUnderscore.values()) {
                if (current.getValue().equals(value)) {
                    return current;
                }
            }
        }
        return null;
    }
}
