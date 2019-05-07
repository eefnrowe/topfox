package com.topfox.misc;

import com.alibaba.fastjson.JSONObject;
import com.topfox.common.CommonException;
import com.topfox.common.ResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Misc {
    static Logger logger = LoggerFactory.getLogger("");

    /**
     * @method : 判断Object 是否存在 与 值是否为空 支持Object, String, List.size(), Map.key
     */
    public static boolean isNull(Object obj) {
        if(obj==null){
            return true;
        }
        if(obj instanceof String){
            String str = obj.toString().trim();
            if(str.equals("") || str.equals("null") || str.equals("undefined")){
                return true;
            }
        }
        if(obj instanceof String[]){
            String[] str = (String[]) obj;
            if(str.length==0){
                return true;
            }
        }
        if(obj instanceof List){
            List list = (List) obj;
            if(list.size()==0){
                return true;
            }
        }
        if(obj instanceof Map){
            Map map = (Map) obj;
            return map.isEmpty();
        }
        return false;
    }

    /**
     * @method : 判断参数值 是否不存在 与 是否不为空 支持Object, String, List.size(), Map.key
     */
    public static Boolean isNotNull(Object obj){
        return !isNull(obj);
    }

    public static String fillStr(Object as_str, int ai_len,  String as_fill_str){
        for (int i=0; i<ai_len; i++){
            if (as_str.toString().length() < ai_len) as_str = as_fill_str+as_str;
        }

        return as_str.toString();
    }

    public static String fillStr2(Object as_str, int ai_len,  String as_fill_str){
        for (int i=0; i<ai_len; i++){
            if (as_str.toString().getBytes().length < ai_len) as_str = as_str+as_fill_str;
        }

        return as_str.toString();
    }

    //将字符按照指定的 分割符切分成数据ArrayList
    public static ArrayList<String> string2Array(String string, String split){
        ArrayList<String> arrayList = new ArrayList();
        if (string==null) {
            return arrayList;
        }
        StringTokenizer st=new StringTokenizer(string,split);
        while(st.hasMoreTokens()) {
            String _value=st.nextToken().trim();
            if (!isNull(_value) && !arrayList.contains(_value)) {
                arrayList.add(_value);
            }
        }

        return arrayList;
    }

    public static Set<String> string2Set(String values, String split){
        Set<String> set = new HashSet();
        if (isNull(values)) return set;

        values=values.replace("，",",");
        StringTokenizer st=new StringTokenizer(values,split);
        while(st.hasMoreTokens()) {
            String _value=st.nextToken().trim();
            if (!isNull(_value)) {
                set.add(_value);
            }
        }

        return set;
    }

    /**
     * 传入 array2Set("01,02","03,01","02")是 返回 set 01,02,03
     * @param values
     * @return
     */
    public static Set<String> array2Set(Object... values){
        Set<String> set=new HashSet<>();
        for (Object value : values){
            if (value == null) continue;

            if (value instanceof String) {
                set.addAll(string2Set(String.valueOf(value), ","));
            }else if (value instanceof Number){//数字类型
                set.add(String.valueOf(value));
            }else{
                throw CommonException.newInstance(ResponseCode.PARAM_IS_INVALID)
                        .text("参数类型("+value.getClass().getName()+")不对, 目前仅支持 String和Number的子类");
            }
        }
        return set;
    }

    public static Map<String, JSONObject> toKeysJSONObject (List<Map<String,Object>> data, String fieldName){
        Map<String,JSONObject> rowsOriginal =new HashMap<String,JSONObject>();
        for(Map<String,Object> rowMap:data){
            JSONObject row=new JSONObject(rowMap);
            rowsOriginal.put(row.getString(fieldName),row);
        }
        return rowsOriginal;
    }

    //将字符按照指定的 分割符切分成数据ArrayList
    public static HashMap<String,String> stringToHashMap(String string, String split){
        if (string == null || string.equals("")) return null;

        ArrayList<String> arrayList = new ArrayList();
        arrayList = string2Array(string, ",");

        HashMap<String, String> hashMap = new HashMap();
        for (String str : arrayList){
            String[] pos = str.toString().split("=");
            hashMap.put(pos[0],pos.length==1?"":pos[1]);
        }
        return hashMap;
    }
    public static HashMap<String,String> arryToHashMap(String[] str){
        HashMap<String, String>map=new HashMap<String, String>();
        if(isNull(str)||str.length==0)return map;
        for(String s:str){
            map.put(s, s);
        }
        return map;
    }






    /**
     * @method : obj 非空校验
     */
    public static void checkObjNotNull(Object object, String errorName) {
        if(isNull(object)){ throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("参数值["+errorName+"]不能为空！");}
    }

    /**
     * @method : javaBean 非空校验
     */
    public static <T> T checkBeansNotNull(T t, String... keys) {
        if(isNull(t)){
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("参数[T t]不存在！");
        }
        for (String key : keys) {
            try {
                Field reflectIds = t.getClass().getDeclaredField(key);
                reflectIds.setAccessible(true);
                if (isNull(reflectIds.get(t))) {
                    throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("参数值["+key+"]不能为空！");
                }
            } catch (Exception e) {
                if (e instanceof NoSuchFieldException) {
                    throw CommonException.newInstance(ResponseCode.PARAM_IS_INEXISTENCE).text("参数["+key+"] 不存在！");
                }
                throw CommonException.newInstance(ResponseCode.NULLException).text(e.getMessage());
            }
        }
        return t;
    }

    /**
     * @method: Map 非空校验
     */
    public static Boolean checkMapsNotNull(Map<String, Object> map, String... keys) {
        if(isNull(map)){
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("参数[Map<String, Object> objMap]不能为null！");
        }
        StringBuilder stringBuilderKeys=new StringBuilder("参数[");
        StringBuilder stringBuildevValues=new StringBuilder("参数值[");

        for (String key : keys) {
            if(map.containsKey(key)==false){
                stringBuilderKeys.append(key).append(",");
            }
            if(isNull(map.get(key))){
                stringBuildevValues.append(key).append(",");
            }
        }
        if (stringBuilderKeys.length()>0){
            stringBuilderKeys.setLength(stringBuilderKeys.length()-1);//去掉最后的逗号和空格
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text(stringBuilderKeys.append("]不存在！").toString());
        }
        if (stringBuildevValues.length()>0){
            stringBuildevValues.setLength(stringBuildevValues.length()-1);//去掉最后的逗号和空格
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text(stringBuildevValues.append("]不能为空！").toString());
        }

        return true;
    }

    //配置文件 根据Key读取Value
    public static String getValueByKey(String filePath, String key) {
        Properties pps = new Properties();
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(filePath));
            pps.load(in);
            String value = pps.getProperty(key);
            return value;
        }catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String set2String(Set<String> setValues){
        return setValues.toString().substring(1).replace("]","");
    }


    public static String getWhereOrSql(List<JSONObject> rows, String valueField, String whereField){
        Set<String> ids=new HashSet<>();
        for (JSONObject row : rows) {
            ids.add(row.getString(valueField));
        }
        return getWhereOrSql(ids,whereField);
    }

    /**
     * @param values（传入多个值，英文逗号隔开的,如 a,b,c）
     * @param field
     * @return 返回  (field='a' OR field='b' OR field='b')
     */
    public static String getWhereOrSql(String values, String field){
        return getWhereOrSql(Misc.string2Set(values,","),field);
    }


    /**
     * 返回  (field='value1' OR field='value2' ...)
     * value1、value2...是第一个参数 setValues中的值
     * @param setValues
     * @param field
     * @return
     */
    public static String getWhereOrSql(Set<String> setValues, String field){
        if (setValues==null ||setValues.size()==0) return "(1=2)";
        StringBuilder whereOrStringBuilder = new StringBuilder("(");
        for (String key:setValues){
            if (Misc.isNull(key)) continue;
            whereOrStringBuilder.append(field).append("='").append(key).append("' OR ");
        }
        whereOrStringBuilder.append(")");
        String temp=whereOrStringBuilder.toString();
        temp=temp.replace(" OR )",")");
        return Misc.isNull(temp)||temp.equals("()")?"(1=2)":temp;
    }

    public static String getWhereLikeOrSql(List<String> arrayListValues,String field){
        StringBuilder whereOrStringBuilder = new StringBuilder("");
        if (arrayListValues.size()>0){
            whereOrStringBuilder.append("(");
            for(int i=0;i<arrayListValues.size();i++){
                if (i==arrayListValues.size()-1)
                    whereOrStringBuilder.append(field).append(" LIKE '").append("%").append(arrayListValues.get(i)).append("%").append("'");
                else
                    whereOrStringBuilder.append(field).append(" LIKE '").append("%").append(arrayListValues.get(i)).append("%").append("' OR ");
            }
            whereOrStringBuilder.append(")");

        }
        String temp=whereOrStringBuilder.toString();
        return Misc.isNull(temp)||temp.equals("()")?"(1=2)":temp;
    }

    public static String getWhereNotLikeSql(List<String> arrayListValues,String field){
        StringBuilder whereOrStringBuilder = new StringBuilder("");
        if (arrayListValues.size()>0){
            whereOrStringBuilder.append("(");
            for(int i=0;i<arrayListValues.size();i++){
                if (i==arrayListValues.size()-1)
                    whereOrStringBuilder.append(field).append(" NOT LIKE '").append("%").append(arrayListValues.get(i)).append("%").append("'");
                else
                    whereOrStringBuilder.append(field).append(" NOT LIKE '").append("%").append(arrayListValues.get(i)).append("%").append("' OR ");
            }
            whereOrStringBuilder.append(")");

        }
        String temp=whereOrStringBuilder.toString();
        return Misc.isNull(temp)||temp.equals("()")?"(1=2)":temp;
    }

    public static String getWhereNotInSql(List<String> arrayValue,String field){
        StringBuilder whereOrStringBuilder = new StringBuilder("");
        arrayValue.remove(null);arrayValue.remove("");
        if (arrayValue.size()>0){
            whereOrStringBuilder.append("(");
            for(int i=0;i<arrayValue.size();i++){
                if (i==arrayValue.size()-1)
                    whereOrStringBuilder.append(field).append("<>'").append(arrayValue.get(i)).append("'");
                else
                    whereOrStringBuilder.append(field).append("<>'").append(arrayValue.get(i)).append("' AND ");
            }
            whereOrStringBuilder.append(")");

        }

        String temp=whereOrStringBuilder.toString();
        return Misc.isNull(temp)||temp.equals("()")?"(1=1)":temp;
    }

    /**
     * 返回的值是 逗号串起来的 A,B,C
     * @param list
     * @param cols 多个字段名
     * @return
     * @throws Exception
     */
    public static String getValueStringByCols(List<JSONObject> list, String cols){
        return getValueStringByCols(list,cols,"#");
    }
    /**
     * @param list 数据集合
     * @param cols 多个字段名
     * @param splitString 返回多个字段值的分隔符号
     * @return 返回的值是 都好串起来的 如果cols是两个字段，list里面有两行数据，则返回value1#value2,value3#value4
     * @throws Exception
     */
    public static String getValueStringByCols(List<JSONObject> list, String cols,String splitString){
        String colsArray[] = cols.split(",");
        if (list==null || colsArray==null || list.size()==0||colsArray.length==0)return "";

        ArrayList<String> arrayList = new ArrayList<String>();

        for (JSONObject row :list) {
            String oneValuesString="";
            for (int i = 0; i < colsArray.length; i++) {
                String column=colsArray[i];
                if (Misc.isNull(column))continue;
                column=column.trim();
                String oneValue = row.getString(column);
                if (oneValue == null) oneValue = "";
                if (i==0)oneValuesString = oneValuesString + oneValue;
                else     oneValuesString = oneValuesString +splitString+ oneValue;
            }

            if (arrayList.contains(oneValuesString)==false) {
                arrayList.add(oneValuesString);
            }

        }

        String tempString = arrayList.toString();
        String retValue = tempString.substring(1, tempString.length() - 1);
        return retValue;
    }

    /**
     * 返回的值是 都好串起来的 A,B,C
     * @param list
     * @param column
     * @return
     * @throws Exception
     */
    public static String getValueString(List<JSONObject> list, String column){
        if(list.size() == 0) return "";
        ArrayList<String> arrayList = new ArrayList<String>();
        String valueString;
        for(JSONObject row : list){
            valueString = row.getString(column);
            if (arrayList.contains(valueString)==false)
                arrayList.add(row.getString(column));
        }
        String tempString = arrayList.toString();
        valueString = tempString.substring(1, tempString.length() - 1);
        return valueString;
    }

    public static String getValueString(List list, String keyField,String keyFieldValue, String column){
        return getValueString(list, new String[] { keyField },
                new String[] { keyFieldValue }, column);
    }

    public static String getValueString(List<JSONObject> list, String[] keyFields,
                                        String[] keyFieldsValue, String column) {
        JSONObject row = null;
        String valueString = "";
        String keyFieldsValueStringCurrent = "";
        String keyFieldsValueString = "";// 传进来的值 串起来的

        for (int j = 0; j < keyFieldsValue.length; j++) {
            keyFieldsValueString += keyFieldsValue[j];
        }
        for (int k = 0; k < list.size(); k++) {
            row = list.get(k);
            keyFieldsValueStringCurrent = "";
            for (int i = 0; i < keyFields.length; i++) {
                keyFieldsValueStringCurrent += row.getString(keyFields[i]);
            }

            if (keyFieldsValueStringCurrent.equals(keyFieldsValueString)) {
                String tempString = (String) row.getString(column);
                if (tempString == null) {
                    tempString = "";
                }
                if (valueString == "") {
                    valueString = tempString;
                } else {
                    valueString = valueString + "," + tempString;
                }
            }
        }
        String array[] = valueString.split(",");
        List<String> listString = new ArrayList<String>();
        for (int i = 0; i < array.length; i++) {
            if (!listString.contains(array[i]) && !array[i].equals("*")) {
                listString.add(array[i]);
            }
        }
        String tempString = listString.toString();
        valueString = tempString.substring(1, tempString.length() - 1);
        return valueString;
    }
    //将字符按照指定的 分割符切分成数据ArrayList
    public static JSONObject urlParameter2Map(String string){
        if (string == null || string.equals("")) return null;

        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList = string2Array(string, "&");

        JSONObject jsonObject = new JSONObject(logger.isDebugEnabled());
        for (String str : arrayList){
            String[] pos = str.split("=");
            jsonObject.put(pos[0],pos.length==1?"":pos[1]);
        }
        return jsonObject;
    }


    /**
     * 正则表达式匹配两个指定字符串中间的内容
     * @param soap
     * @return
     */
    public static List<String> getSubUtil(String soap,String rgex){
        List<String> list = new ArrayList<String>();
        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            int i = 1;
            list.add(m.group(i));
            i++;
        }
        return list;
    }

    /**
     * 返回单个字符串，若匹配到多个的话就返回第一个，方法与getSubUtil一样
     * @param soap
     * @param rgex
     * @return
     */
    public static String getSubUtilSimple(String soap,String rgex){
        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while(m.find()){
            return m.group(1);
        }
        return "";
    }

    /**
     * 正则表达式匹配两个指定字符串中间的内容
     * @param string
     * @return
     */
    public static String getSubLeft(String string,String find){
        int pos = string.lastIndexOf(find);
        return string.substring(pos+find.length());
    }

//    /**
//     * 测试
//     * @param args
//     */
//    public static void main(String[] args) {
//        String str = "abc3443abcfgjhg abcgfjabc";
//        String rgex = "abc(.*?)abc";
//        System.out.println(getSubUtil(str,rgex));
//        System.out.println(getSubUtilSimple(str, rgex));
//    }



    /** 字符串压缩为字节数组*/
    public static byte[] gzip2byte(String str) {
        return gzip2byte(str,null);
    }
    public static byte[] gzip2byte(String str,String encoding){
        if (str == null || str.length() == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip=null;
        try {
            gzip = new GZIPOutputStream(out);
            if(encoding==null)  gzip.write(str.getBytes());
            else gzip.write(str.getBytes(encoding));
            gzip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (gzip != null)try { gzip.close(); } catch (IOException e) { }
            if (out != null)   try { out.close();    } catch (IOException e) { }
        }
        return out.toByteArray();
    }


    /** 压缩为字符串 */
    public static String gzip2String(String primStr) {
        return  new sun.misc.BASE64Encoder().encode(gzip2byte(primStr));
    }
    public static String gzip2String(String primStr,String encoding) {
        return  new sun.misc.BASE64Encoder().encode(gzip2byte(primStr,encoding));
    }
    /* 字节数组解压缩后返回字符串     */
    public static String gunzip(byte[] b) {
        return gunzip(b,null);
    }
    public static String gunzip(byte[] b,String charsetName){
        if (b == null || b.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        GZIPInputStream gunzip=null;
        try {
            gunzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }

            if (charsetName==null || charsetName.equals("")) {
                return out.toString();
            }else{
                return out.toString(charsetName);
            }
        } catch (java.util.zip.ZipException e2) {
            e2.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (gunzip != null)try { gunzip.close(); } catch (IOException e) { }
            if (in != null)    try { in.close();     } catch (IOException e) { }
            if (out != null)   try { out.close();    } catch (IOException e) { }
        }
        return null;
    }

    /** 解压 */
    public static String gunzip(String compressedStr) {
        return gunzip(compressedStr,null);
    }
    /** 解压 */
    public static String gunzip(String compressedStr,String charsetName) {
        if (compressedStr == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = null;
        GZIPInputStream ginzip = null;
        byte[] compressed = null;
        String decompressed = null;
        try {
            compressed = new sun.misc.BASE64Decoder().decodeBuffer(compressedStr);
            in = new ByteArrayInputStream(compressed);
            ginzip = new GZIPInputStream(in);

            byte[] buffer = new byte[1024];
            int offset = -1;
            while ((offset = ginzip.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }
            if (charsetName==null || charsetName.equals("")){
                decompressed = out.toString();
            }else{
                decompressed = out.toString(charsetName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ginzip != null)try { ginzip.close(); } catch (IOException e) { }
            if (in != null)    try { in.close();     } catch (IOException e) { }
            if (out != null)   try { out.close();    } catch (IOException e) { }
        }
        return decompressed;
    }

    /** 判断字符串是否为数字 纯数字return:true 否则false*/
    public static boolean isNumeric(String str){
        if(isNull(str)){
            return false;
        }
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if(!isNum.matches()){
            return false;
        }
        return true;
    }

}
