package com.topfox.data;

public enum DataType {
    STRING(1, "STRING"),
    INTEGER(4, "INTEGER"),
    LONG(5, "LONG"),
    DOUBLE(7, "DOUBLE"),
    DECIMAL(8, "DECIMAL"),
    BOOLEAN(9, "BOOLEAN"),
    DATE(10, "DATE"),
    OBJECT(99, "OBJECT");//DTO 中包含 DTO的情况

    private Integer code;
    private String value;

    DataType(Integer code, String value) {
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

    public static DataType getByValue(String value) {
        if (value != null) {
            for (DataType current : DataType.values()) {
                if (current.getValue().equals(value)) {
                    return current;
                }
            }
        }
        return null;
    }

    public static DataType getDataType(Class<?> type) {
        String dataType2 = type.getName();

        if (dataType2.equals("java.util.Date"))
            return DataType.DATE;
        if (dataType2.equals("java.math.BigDecimal"))
            return DataType.DECIMAL;
        if (dataType2.equals("double") || dataType2.equals("java.math.Double") || dataType2.equals("java.lang.Double"))
            return DataType.DOUBLE;
        if (dataType2.equals("int") ||
                dataType2.equals("java.lang.Integer") || dataType2.equals("integer"))
            return DataType.INTEGER;
        if (dataType2.equals("long") || dataType2.equals("java.lang.Long"))
            return DataType.LONG;
        if (dataType2.equals("boolean") || dataType2.equals("java.lang.Boolean"))
            return DataType.BOOLEAN;
        if (dataType2.equals("java.lang.String"))
            return DataType.STRING;

        //throw new CommonException(ResponseCode.SYSTEM_ERROR,"设计上不支持类型"+dataType2);
        return DataType.OBJECT;
    }

    public static boolean isNumber(DataType dataType) {
        return dataType==DataType.DOUBLE || dataType==DataType.INTEGER || dataType==DataType.DECIMAL || dataType==DataType.LONG;
    }
}
