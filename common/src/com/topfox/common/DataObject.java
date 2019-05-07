package com.topfox.common;


import com.topfox.data.DataHelper;
import com.topfox.misc.DateUtils;

import java.util.Date;

public class DataObject {
    Object value;

    public static DataObject newInstance(){
        return new DataObject();
    }

    public DataObject setValue(Object value){
        this.value = value;
        return this;
    }
    public Object getValue(){
        return value;
    }


    public String getString(){
        return DataHelper.parseString(value);
    }

    public Double getDouble(){
        return DataHelper.parseDouble(value);
    }

    /**
     *
     * @param format  例: "###0.000" 精确到 3位小数,不带千分位逗号
     * @return
     */
    public String getDoubleToString(String format){
        return DataHelper.roundToString(getDouble(), format);
    }

    public Date getDate(){
        return DataHelper.parseDate(value);
    }
    public String getDateToString(){
        return DateUtils.toDateStr(getDate());
    }
    public String getDateToString(String format){
        return DateUtils.toDateStr(getDate(), format);
    }

    public int getInt(){
        return DataHelper.parseInt(value);
    }

    public long getLong(){
        return DataHelper.parseLong(value);
    }

    public boolean getBoolean(){
        return DataHelper.parseBoolean(value);
    }
}
