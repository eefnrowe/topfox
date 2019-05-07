package com.topfox.misc;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DateUtils {

    // 用于存放不同模板的日期
    private static final ThreadLocal<Map<String, SimpleDateFormat>> LOCAL = new ThreadLocal<Map<String, SimpleDateFormat>>() {
        @Override
        protected Map<String, SimpleDateFormat> initialValue() {
            return new HashMap<>();
        }
    };

    /**
     * 返回一个SimpleDateFormat,每个线程只会new一次pattern对应的sdf
     *
     * @param dateFormat
     * @return
     */
    public static SimpleDateFormat getDateFormat(String dateFormat) {
        Map<String, SimpleDateFormat> map = LOCAL.get();
        SimpleDateFormat sdf = map.get(dateFormat);
        if (sdf == null) {
            sdf = new SimpleDateFormat(dateFormat);
            map.put(dateFormat, sdf);
        }
        return sdf;
    }

    /**
     * 获取昨天的日期
     *
     * @return
     */
    public static Date getYesterDay() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DAY_OF_MONTH, -1);
        return c.getTime();
    }

    /**
     * 获取从当前日期指定分钟以前的日期
     *
     * @return
     */
    public static Date getDateBeforeMinute(int beforeMinute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int minute = calendar.get(Calendar.MINUTE);
        calendar.set(Calendar.MINUTE, minute - beforeMinute);
        return calendar.getTime();
    }

//    /**
//     * 获取两个日期之间的天数
//     *
//     * @param before
//     * @param after
//     * @return
//     */
//    public static long getDistanceTwoDate(Date before, Date after) {
//        long beforeTime = before.getTime();
//        long afterTime = after.getTime();
//        return (afterTime - beforeTime) / (1000 * 60 * 60 * 24);
//    }


    /**
     * 获取某个date的年份
     * @param date
     * @return
     */
    public static int getYear(Date date){
        Calendar c=Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.YEAR);
    }
    /**
     * 获取某个date的月份
     * @param date
     * @return
     */
    public static int getMonth(Date date){
        Calendar c=Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.MONTH)+1;
    }
    /**
     * 获取某个date的day
     * @param date
     * @return
     */
    public static int getDayOfMonth(Date date){
        Calendar c=Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.DAY_OF_MONTH);
    }
    /**
     * 判断字符串是否是指定的格式
     * @param date
     * @param dateFormat
     * @return
     */
    public static boolean isDate(String date, String dateFormat) {
        SimpleDateFormat sdf = getDateFormat(dateFormat);
        try {
            sdf.setLenient(false);
            sdf.parse(date);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 获取两个时间的时间差
     * @param before
     * @param after
     * @param flag 0秒，1分，2时，3天
     * @return
     */
    public static int getDifferentNum(Date before,Date after,int flag){
        if(before==null||after==null){
            return 0;
        }
        long timeInterval=after.getTime()-before.getTime();
        switch (flag) {
            case 0://秒
                return (int) (timeInterval/1000);
            case 1://分
                return (int) (timeInterval/(60*1000));
            case 2://时
                return (int) timeInterval/(60*60*1000);
            case 3://天
                return (int) timeInterval/(60*60*1000*24);
        }
        return 0;
    }


    /**获得毫秒*/
    public static String getMilliSecond() {
        Calendar calendar = Calendar.getInstance();
        DecimalFormat df = new DecimalFormat("00");
        return new DecimalFormat("000").format(calendar.get(Calendar.MILLISECOND));
    }
    public static String long2Time(long millSec){
        Date date= new Date(millSec);
        return getDateFormat(DateFormatter.TIME_SECOND_FORMAT).format(date);
    }
    public static String long2DateTime(long millSec){
        Date date= new Date(millSec);
        return getDateFormat(DateFormatter.DATETIME_SECOND_FORMAT).format(date);
    }
    //
    public static Date long2Date(long millSec){
        Date date=stringToDate(long2DateTime(millSec));
        return date;
    }

    public static int currentYear() {
        //return currentDate().getYear() + 1900;
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR);

    }

    /**
     * string转化为指定模板的date
     *
     * @param dateString 日期字符串
     * @param dateFormat 日期格式
     * @return
     */
    public static Date toDate(String dateString, String dateFormat) {
        try {
            SimpleDateFormat sdf = getDateFormat(dateFormat);
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date toDate(String dateStr){
        dateStr=dateStr.replace("年", "-");
        dateStr=dateStr.replace("月", "-");
        dateStr=dateStr.replace("日", "-");
        return stringToDate(dateStr.replace("/", "-"));
    }
    public static java.sql.Date getSqlDate(String dateStr){
        dateStr=dateStr.replace("年", "-");
        dateStr=dateStr.replace("月", "-");
        dateStr=dateStr.replace("日", "-");
        return java.sql.Date.valueOf(dateStr.replace("/", "-"));
    }

    /**
     * 获得 Date 的当前时间
     *
     * @return  Date
     */
    public static Date getCurrent() {
        return new Date(System.currentTimeMillis());
    }

    /**
     * yyyy-MM-dd
     * @return
     */
    public static String getDate(){
        return getDate2String(DateFormatter.DATE_FORMAT);
    }
    /**
     * yyyy-MM-dd HH:mm
     * @return
     */
    public static String getDateByHHmm(){
        return getDate2String(DateFormatter.DATETIME_FORMAT);
    }
    /**
     * yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static String getDateByHHmmss(){
        return getDate2String(DateFormatter.DATETIME_SECOND_FORMAT);
    }
    /**
     * yyyy-MM-dd HH:mm:ss SSS
     * @return
     */
    public static String getDateByHHmmssSSS(){
        return getDate2String(DateFormatter.DATETIME_MILLSECOND_FORMAT);
    }
    public static String getDate2String(String format){
        SimpleDateFormat fmt = getDateFormat(format);
        Calendar calendar = Calendar.getInstance();
        Date date=calendar.getTime();
        String ts=fmt.format(date);
        return ts;
    }
    public static java.sql.Date getSqlDate() {
        return new java.sql.Date(System.currentTimeMillis());
    }

    public static Timestamp getTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    public static String getTimeByHHmmssSSS(){
        return getDate2String(DateFormatter.TIME_MILLSECOND_FORMAT);
    }
    public static String getTimeByHHmm(){
        return getDate2String(DateFormatter.TIME_FORMAT);
    }
    public static String getTimeByHHmmss(){
        return getDate2String(DateFormatter.TIME_SECOND_FORMAT);
    }


    /**
     * date类型转化为指定模板的string
     * @param date
     * @param dateFormat
     * @return
     */
    public static String toDateStr(Date date,String dateFormat){
        if (date==null) return "";
        SimpleDateFormat sdf = getDateFormat(dateFormat);
        return sdf.format(date);
    }

    public static String toDateStr(Date date) {
        if (date==null) return "";
        return getDateFormat(DateFormatter.DATE_FORMAT).format(date);
    }

    public static String toDateCnStr(Date date) {
        if (date==null) return "";
        return getDateFormat(DateFormatter.DATE_FORMAT_CN).format(date);
    }
    public static String toDateTimeCnStr(Date date) {
        if (date==null) return "";
        return getDateFormat(DateFormatter.DATETIME_FORMAT_CN).format(date);
    }

    public static String toDateTimeStr(Date date) {
        if (date==null) return "";
        return getDateFormat(DateFormatter.DATETIME_FORMAT).format(date);
    }

    public static String toDateTimeSecond(Date date) {
        if (date==null) return "";
        return getDateFormat(DateFormatter.DATETIME_SECOND_FORMAT).format(date);
    }

    public static String toDateTimeMillis(Date date) {
        if (date==null) return "";
        return getDateFormat(DateFormatter.DATETIME_MILLSECOND_FORMAT).format(date);
    }


    // 请使用 toDateStr(Date date, String dateFormat)
    @Deprecated
    public static String toDateTimeStr(Date date, String dateFormat) {
        if (date==null) return "";
        SimpleDateFormat sdf = getDateFormat(dateFormat);
        return sdf.format(date);
    }

    public static Date getDefaultDate(){
        try{
            return stringToDate("2000-01-01");
        }catch (Exception e){
        }
        return null;
    }


    public static long getDifferDays2(String beginDateStr,String endDateStr){
        //格式化日期，yyyy-MM-dd 是计算相隔天数，yyyy-MM-dd HH:mm:ss 是计算相差天数
        java.text.SimpleDateFormat sf = getDateFormat(DateFormatter.DATETIME_SECOND_FORMAT);
        long betweenDate = 0l;
        try {
            Date beginDate = sf.parse(beginDateStr);
            Date endDate = sf.parse(endDateStr);
            betweenDate = (endDate.getTime()-beginDate.getTime())/(24 * 60 * 60 * 1000);
        } catch (ParseException e) {
            throw new RuntimeException("计算相差天数出错！",e);
        }
        return betweenDate;
    }

    //计算相差天数（开始和结束日期）（相差和相隔的区别，1日 11:59，和2日 00:01，相差0天，相隔1天 ）
    public static long getDifferDays(String beginDateStr,String endDateStr){
        //格式化日期，yyyy-MM-dd 是计算相隔天数，yyyy-MM-dd HH:mm:ss 是计算相差天数
        java.text.SimpleDateFormat sf = getDateFormat(DateFormatter.DATETIME_SECOND_FORMAT);
        long betweenDate = 0l;
        try {
            Date beginDate = sf.parse(beginDateStr);
            Date endDate = sf.parse(endDateStr);
            betweenDate = (endDate.getTime()-beginDate.getTime())/(24 * 60 * 60 * 1000);
        } catch (ParseException e) {
            throw new RuntimeException("计算相差天数出错！",e);
        }
        return betweenDate;
    }

    //计算相差天数，开始日期默认是当前日期（相差和相隔的区别，1日11:59，和2日的00:01，相差0天，相隔1天 ）
    public static long getDifferDays(String endDateStr){
        //格式化日期，yyyy-MM-dd 是计算相隔天数，yyyy-MM-dd HH:mm:ss 是计算相差天数
        java.text.SimpleDateFormat sf = getDateFormat(DateFormatter.DATETIME_SECOND_FORMAT);
        String beginDateStr = toDateStr(new Date(System.currentTimeMillis()));
        long betweenDate = 0l;
        try {
            Date beginDate = sf.parse(beginDateStr);
            Date endDate = sf.parse(endDateStr);
            betweenDate = (endDate.getTime()-beginDate.getTime())/(24 * 60 * 60 * 1000);
        } catch (ParseException e) {
            throw new RuntimeException("计算相差天数出错！",e);
        }
        return betweenDate;
    }

    //计算相隔天数（开始和结束日期）（相差和相隔的区别，1日11:59，和2日的00:01，相差0天，相隔1天 ）
    public static long getSeparationDays(String beginDateStr,String endDateStr){
        //格式化日期，yyyy-MM-dd 是计算相隔天数，yyyy-MM-dd HH:mm:ss 是计算相差天数
        java.text.SimpleDateFormat sf = getDateFormat(DateFormatter.DATE_FORMAT);
        long betweenDate = 0l;
        try {
            Date beginDate = sf.parse(beginDateStr);
            Date endDate = sf.parse(endDateStr);
            betweenDate = (endDate.getTime()-beginDate.getTime())/(24 * 60 * 60 * 1000);
        } catch (ParseException e) {
            throw new RuntimeException("计算相隔天数出错！",e);
        }
        return betweenDate;
    }

    //计算相隔天数，开始日期默认是当前日期（相差和相隔的区别，1日11:59，和2日的00:01，相差0天，相隔1天 ）
    public static long getSeparationDays(String endDateStr){
        //格式化日期，yyyy-MM-dd 是计算相隔天数，yyyy-MM-dd HH:mm:ss 是计算相差天数
        java.text.SimpleDateFormat sf = getDateFormat(DateFormatter.DATE_FORMAT);
        String beginDateStr = toDateStr(new Date(System.currentTimeMillis()));
        long betweenDate = 0l;
        try {
            Date beginDate = sf.parse(beginDateStr);
            Date endDate = sf.parse(endDateStr);
            betweenDate = (endDate.getTime()-beginDate.getTime())/(24 * 60 * 60 * 1000);
        } catch (ParseException e) {
            throw new RuntimeException("计算相隔天数出错！",e);
        }
        return betweenDate;
    }

    public static Date getChangeDate(Date date,String type,int changeNo){
        Calendar calendar = Calendar.getInstance();
        if(date==null)date=calendar.getTime();
        else calendar.setTime(date);
        if(changeNo==0)return date;
        if("year".equals(type))calendar.add(Calendar.YEAR, changeNo);
        else if("month".equals(type))calendar.add(Calendar.MONTH, changeNo);
        else if("day".equals(type))calendar.add(Calendar.DAY_OF_MONTH, changeNo);
        else if("hour".equals(type))calendar.add(Calendar.HOUR, changeNo);
        else if("minute".equals(type))calendar.add(Calendar.MINUTE, changeNo);
        else if("second".equals(type))calendar.add(Calendar.SECOND, changeNo);
        return calendar.getTime();
    }

    public static Calendar getChangeDate(Integer day, Integer hour, Integer minute, Integer second, Integer millisecond){
        return getChangeDate(Calendar.getInstance(), day, hour, minute, second, millisecond);
    }

    public static Calendar getChangeDate(Calendar calendar, Integer day, Integer hour, Integer minute, Integer second, Integer millisecond){
        if(calendar==null){
            calendar = Calendar.getInstance();
        }
        if(day!=null){
            calendar.add(Calendar.DAY_OF_YEAR, day);
        }
        if(hour!=null){
            if(hour==0){
                calendar.set(Calendar.HOUR_OF_DAY, hour);
            }else{
                calendar.add(Calendar.HOUR_OF_DAY, hour);
            }
        }
        if(minute!=null){
            calendar.set(Calendar.MINUTE, minute);
        }
        if(second!=null){
            if(second==0){
                calendar.set(Calendar.SECOND, second);
            }else{
                calendar.add(Calendar.SECOND, second);
            }
        }
        if(millisecond!=null){
            calendar.set(Calendar.MILLISECOND, millisecond);
        }
        return calendar;
    }

    public static Date stringToDate(String dateStr) {
        Calendar calendar = Calendar.getInstance();
        if(dateStr.length()<10){
            String[] values1=dateStr.split("-");
            String[] values2=new String[]{"00","00","00"};
            if(values1.length!=3)return null;
            else calendar.set(Integer.parseInt(values1[0]), Integer.parseInt(values1[1])-1, Integer.parseInt(values1[2]), Integer.parseInt(values2[0]), Integer.parseInt(values2[1]), Integer.parseInt(values2[2]));
        }else if(dateStr.length()==10){
            String[] values1=dateStr.split(" ")[0].split("\\-");
            String[] values2=new String[]{"00","00","00"};
            calendar.set(Integer.parseInt(values1[0]), Integer.parseInt(values1[1])-1, Integer.parseInt(values1[2]), Integer.parseInt(values2[0]), Integer.parseInt(values2[1]), Integer.parseInt(values2[2]));
        }else if(dateStr.length()==16){
            String[] values1=dateStr.split(" ")[0].split("\\-");
            String[] values2=dateStr.split(" ")[1].split(":");
            calendar.set(Integer.parseInt(values1[0]), Integer.parseInt(values1[1])-1, Integer.parseInt(values1[2]), Integer.parseInt(values2[0]), Integer.parseInt(values2[1]));
        }else if(dateStr.length()>=19){
            String[] values1=dateStr.split(" ")[0].split("\\-");
            String[] values2=dateStr.split(" ")[1].split(":");
            calendar.set(Integer.parseInt(values1[0]), Integer.parseInt(values1[1])-1, Integer.parseInt(values1[2]), Integer.parseInt(values2[0]), Integer.parseInt(values2[1]), Integer.parseInt(values2[2]));
        }else
            return null;
        return calendar.getTime();
    }

}
