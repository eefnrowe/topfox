package com.topfox.common;

import com.topfox.data.DataHelper;
import com.topfox.data.DataType;
import com.topfox.data.Field;
import com.topfox.misc.DateFormatter;
import com.topfox.misc.DateUtils;
import com.topfox.misc.Misc;

public class ChangeData {

    boolean result;
    DataObject currentValue;
    DataObject originValue;
    Field field;
    FormatConfig formatConfig;

    public ChangeData(boolean result, DataObject currentValue, DataObject originValue, Field field){
        this.result = result;
        this.originValue  = originValue;
        this.field = field;

        if (field.getIncremental() == Incremental.ADDITION || field.getIncremental() == Incremental.SUBTRACT){
            //增量 + - 字段的 处理
            Number temp = ChangeData.incrtl(field, currentValue.getDouble(), originValue.getDouble());
            currentValue.setValue(temp);
        }
        this.currentValue = currentValue;
    }

    /**
     * 根据字段类型 做增量 + - 运算
     * @param field
     * @param currentValue
     * @param originValue
     * @return
     */
    public static Number incrtl(Field field, Number currentValue, Number originValue){
        Number result = null;
        if (field.getDataType() == DataType.DOUBLE || field.getDataType() == DataType.DECIMAL){
            if (field.getIncremental() == Incremental.ADDITION) {
                result = originValue.doubleValue() + currentValue.doubleValue();//递增
            } else if (field.getIncremental() == Incremental.SUBTRACT) {
                result = originValue.doubleValue() - currentValue.doubleValue();//递减
            }
        }else{
            if (field.getIncremental() == Incremental.ADDITION) {
                result = originValue.longValue() + currentValue.longValue();//递增
            } else if (field.getIncremental() == Incremental.SUBTRACT) {
                result = originValue.longValue() - currentValue.longValue();//递减
            }
        }
        return result;
    }

    public Field getField(){
        return field;
    }

    public boolean result(){
        return result;
    }

    public DataObject current(){
        return currentValue;
    }

    public DataObject origin(){
        return originValue;
    }

    public ChangeData setFormatConfig(FormatConfig formatConfig){
        this.formatConfig = formatConfig;
        return this;
    }

    /**
     * 获得修改后的值
     * @return  null 将返回空字符
     */
    public String getCurrentToString() {
        return getString(field, current().getValue());
    }

    /**
     * 获得修改前的值, 旧值
     * @return  null 将返回空字符
     */
    public String getOriginToString(){
        return getString(field, origin().getValue());
    }

    /**
     * 获得Object的值, 根据类型 和 配置 都 转成 字符串
     * @param field
     * @param value
     * @return
     */
    private String getString(Field field, Object value){
        String format=null;
        DataType dataType = field.getDataType();
        if (dataType == DataType.DATE){
            if (formatConfig != null) {
                format = formatConfig.getFormatDataType().get(field.getName());
                if (Misc.isNull(format)) {
                    format = formatConfig.getFormatDataType().get(DataType.DATE);
                }
            }
            if (Misc.isNull(format)){
                format = DateFormatter.DATETIME_FORMAT;
            }
            return DateUtils.toDateStr(DataHelper.parseDate(value), format).replace(" 00:00","");
        }

        if (dataType == DataType.DOUBLE){
            if (formatConfig !=null) {
                format = formatConfig.getFormatFields().get(field.getName());
                if (Misc.isNull(format)) {
                    format = formatConfig.getFormatFields().get(DataType.DOUBLE);
                }
            }
            if (Misc.isNull(format)){
                format = "###0.00";
            }
            return DataHelper.roundToString(value, format);
        }

        if (dataType == DataType.BOOLEAN){
            return DataHelper.parseBoolean(value)?"true":"false";
        }

        return DataHelper.parseString(value);
    }
}
