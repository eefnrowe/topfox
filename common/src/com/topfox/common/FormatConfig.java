package com.topfox.common;

import com.topfox.data.DataType;

import java.util.HashMap;
import java.util.Map;

/**
 * 格式化类
 * 日志 输出  所有类型 都 转 字符串的 通用 格式化类
 *
 */
public class FormatConfig {
    Map<String, String> mapFormatFields;
    Map<DataType, String> mapFormatDataType;

    public FormatConfig(){
        mapFormatDataType = new HashMap<>();
        mapFormatFields = new HashMap<>();

    }


    public Map<String, String> getFormatFields(){
        return mapFormatFields;
    }

    public Map<DataType, String> getFormatDataType(){
        return mapFormatDataType;
    }

    /**
     * 设置 所有类型为Date的输出格式. 默认yyyy-MM-dd
     * @param format
     * @return
     */
    public FormatConfig setDateFormat(String format){
        mapFormatDataType.put(DataType.DATE, format);
        return this;
    }

    /**
     * (日期类型字段)指定一个字段名的输出格式. 默认yyyy-MM-dd
     * @param format
     * @return
     */
    public FormatConfig setDateFormat(String fieldName, String format){
        mapFormatFields.put(fieldName,format);
        return this;
    }

    /**
     * 设置 所有类型为Double的输出格式. 默认格式: ###0.00 (2位小数,无千分位的逗号)
     * @param format
     * @return
     */
    public FormatConfig setDoubleFormat(String format){
        mapFormatDataType.put(DataType.DOUBLE, format);
        return this;
    }

    /**
     * (Double类型字段)指定一个字段名的输出格式. 默认格式: ###0.00 (2位小数,无千分位的逗号)
     * @param format
     * @return
     */
    public FormatConfig setDoubleFormat(String fieldName, String format){
        mapFormatFields.put(fieldName,format);
        return this;
    }

}
