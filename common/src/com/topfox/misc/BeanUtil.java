package com.topfox.misc;

import com.topfox.common.*;
import com.topfox.data.DataHelper;
import com.topfox.data.DataType;
import com.topfox.data.Field;
import com.topfox.data.TableInfo;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BeanUtil {
    static Logger logger = LoggerFactory.getLogger("");

    public static Object getValue(Object bean, Field field) {
        return getValue(bean, field==null?null:field.getName());
    }
    public static Object getValue(Object bean, String fieldName) {
        TableInfo tableInfo=bean==null?null:TableInfo.get(bean.getClass());
        return getValue(tableInfo, bean, null, fieldName);
    }
    public static Object getValue(TableInfo tableInfo, Object bean, Field field){
        return getValue(tableInfo, bean, field,null);
    }
    public static Object getValue(TableInfo tableInfo, Object bean, String fieldName) {
        return getValue(tableInfo, bean, null,fieldName);
    }

    private static Object getValue(TableInfo tableInfo, Object bean, Field field, String fieldName){
        if (bean==null || (field==null && fieldName==null)) {
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("BeanUtil.getValue 出现参数为null");
        }
        if (field !=null && fieldName==null){
            fieldName=field.getName();
        }
        Method getter=tableInfo.getGetter(fieldName);
        Object value;
        try {
            value = getter.invoke(bean);
        } catch (InvocationTargetException e1) {
            if (e1.getMessage()==null && e1.getTargetException() instanceof CommonException){
                throw (CommonException)e1.getTargetException();
            }else {
                throw new RuntimeException("获取 " + bean.getClass() + "." + fieldName + " 报错:" + e1.getMessage());
            }
        } catch (Exception e2) {
            throw new RuntimeException("获取 "+bean.getClass()+"."+fieldName+" 报错:"+e2.getMessage());
        }
        //field.getRightName2();
        return value;
    }

    public static String getValue2String(TableInfo tableInfo, Object bean, Field field){
        Object valueObj = getValue(tableInfo,bean,field);
        String valueString;
        if (valueObj instanceof Date){
            valueString = DateUtils.toDateStr((Date)valueObj,
                    Misc.isNull(field.getFormat())? DateFormatter.DATE_FORMAT :field.getFormat());
        }else {
            valueString = DataHelper.parseString(valueObj);
        }
        if (valueString == null) valueString="";
        return valueString;
    }

    /**
     * 这个 方法 赋值, 会把  Integer Long Doulbe 的值 null赋值为0
     * 对实体 指定字段赋值
     * @param bean  被写值得对象
     * @param field 字段对象  要写的字段对象
     * @param value 要写的值
     */
    public static void setValue(Object bean, Field field, Object value) {
        if (bean==null || field==null) throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("BeanUtil.setValue 出现参数为null");
        TableInfo tableInfo=TableInfo.get(bean.getClass());
        setValue(tableInfo, bean, field, value);
    }

    public static void setValue(TableInfo tableInfo, Object bean, Field field, Object value){
        if (bean==null || field==null) throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("BeanUtil.setValue 出现参数为null");
        Method setter=tableInfo.getSetter(field.getName());
        DataType dateType=field.getDataType();

        if (setter==null){
            throw CommonException.newInstance(ResponseCode.NULLException)
                    .text(getErrMsg("POJO没有找到setter方法",tableInfo, bean, field, value, setter));
        }
        try {
            //根据不同类型, 对数据处理后 set; DataHelper对象模仿上海瑞道
            if (value instanceof Map || value instanceof IBean)
                setter.invoke(bean, value);
            else if (dateType==DataType.STRING)
                setter.invoke(bean, DataHelper.parseString(value));
            else if (dateType==DataType.LONG)
                setter.invoke(bean, DataHelper.parseLong(value));      //会把值null赋值为0
            else if (dateType==DataType.INTEGER)
                setter.invoke(bean, DataHelper.parseInt(value));       //会把值null赋值为0
            else if (dateType==DataType.DOUBLE)
                setter.invoke(bean, DataHelper.parseDouble(value));    //会把值null赋值为0
            else if (dateType==DataType.DECIMAL)
                setter.invoke(bean, DataHelper.parseBigDecimal(value));//会把值null赋值为0
            else if (dateType==DataType.DATE){
                setter.invoke(bean, DataHelper.parseDate(value));
            }else{
                setter.invoke(bean, value);
            }
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } catch (NullPointerException ex2) {
            throw CommonException.newInstance(ResponseCode.NULLException)
                    .text(getErrMsg("POJO setter错误",tableInfo, bean, field, value, setter));
        } catch (Exception e) {
            if (e.getMessage().indexOf("内容过长")>=0){
                throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID).text(e.getMessage());
            }else{
                throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID)
                        .text(getErrMsg("POJO setter错误",tableInfo, bean, field, value, setter));
            }
        }
    }

    public static String getErrMsg(String msg, TableInfo tableInfo, Object bean, Field field, Object value, Method setter){
        Object idValue=getValue(tableInfo,bean,tableInfo.getFields().get(tableInfo.getIdFieldName()));
        StringBuilder sb = new StringBuilder();
        sb.append(msg).append(" :id=").append(idValue.toString()).append(" ")
                .append(bean.getClass().getName()).append(".").append(setter.getName())
                .append("(").append(setter.getParameterTypes()[0].getName()).append(" ")
                .append(value.toString()).append(") ").append(field.toString());

        return sb.toString();
    }
    /**
     * 这个 方法 赋值,  不会把  Integer Long Doulbe 的值 null赋值为0
     * @param tableInfo
     * @param bean
     * @param fieldName
     * @param value
     */
    public static void setValue(TableInfo tableInfo, Object bean, String fieldName, Object value){
        if (bean==null || Misc.isNull(fieldName)) throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("BeanUtil.setValue 出现参数为null");

        Method setter=tableInfo.getSetter(fieldName);

        try {
            setter.invoke(bean, value);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } catch (Exception e) {
            if (e.getMessage().indexOf("内容过长")>=0){
                throw new RuntimeException(e.getMessage());
            }else{
                Object idValue=getValue(tableInfo,bean,tableInfo.getFields().get(tableInfo.getIdFieldName()));
                String idValueString= idValue==null?"":idValue.toString();
                String valueString= value==null?"":value.toString();
                throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID).text("数据转换错误:id="+
                        idValueString +" "+bean.getClass().getName()
                        +"."+setter.getName()+"("+setter.getParameterTypes()[0].getName()+" "+valueString+") ");
            }
        }
    }

    /**
     * 获得变化值 使用
     * 比较大小,相等返回0，大于返回>0的值   小于返回小于0
     * @return long
     */
    public static long compare(Field field,Object value1,Object value2) {
        if (value1==null && value2==null) {
            return 0;
        }
        if (value1==null && value2!=null ||value1!=null && value2==null){
            return 1;
        }

        DataType datatype = field==null?null:field.getDataType();
        if (datatype == DataType.STRING){
            return value1.equals(value2)==true?0:-1;
        }else if (datatype == DataType.LONG){
            return DataHelper.parseLong(value1) - DataHelper.parseLong(value2);
        }else if (datatype == DataType.INTEGER){
            return DataHelper.parseInt(value1)-DataHelper.parseInt(value2);
        }else if (datatype == DataType.DOUBLE){
            if (DataHelper.parseDouble(value1) - DataHelper.parseDouble(value2)>0){
                return 1;
            }else if (DataHelper.parseDouble(value1) - DataHelper.parseDouble(value2)<0){
                return -1;
            }else{
                return 0;
            }
        }else if (datatype == DataType.DECIMAL){
            return DataHelper.parseBigDecimal(value1).compareTo(DataHelper.parseBigDecimal(value2));
        }else if (datatype == DataType.DATE){
            return DataHelper.parseDate(value1).compareTo(DataHelper.parseDate(value2));
        }else if (datatype == DataType.BOOLEAN){
            return DataHelper.parseBoolean(value1)==DataHelper.parseBoolean(value2)?0:-1;
        }else{
            return value1.toString().equals(value2.toString())==true?0:1;
        }
    }

    //isUpdateSQL true 生成更新SQL用； false保存后将有修改的值返回到前台用


    public static Map<String, Object> getChangeRowData(DataDTO bean) {//,boolean isUpdateSQL
        return getChangeRowData(bean, 2);
    }
    /**
     *
     * @param bean
     *
     * @param updateMode 1/2/3
     * # 重要参数:更新时DTO序列化策略 和 更新SQL生成策略
     * # 1 时, service的DTO=提交的数据.               更新SQL 提交数据不等null 的字段 生成 set field=value
     * # 2 时, service的DTO=修改前的原始数据+提交的数据. 更新SQL (当前值 != 原始数据) 的字段 生成 set field=value
     * # 3 时, service的DTO=修改前的原始数据+提交的数据. 更新SQL (当前值 != 原始数据 + 提交数据的所有字段)生成 set field=value
     * #   值为3, 则始终保证了前台(调用方)提交的字段, 不管有没有修改, 都能生成更新SQL, 这是与2最本质的区别
     *
     * @return
     */
    public static Map<String, Object> getChangeRowData(DataDTO bean, int updateMode){//,boolean isUpdateSQL
        JSONObject rowNew = new JSONObject(logger.isDebugEnabled());//debug时 map字段有序
        if(bean == null) return rowNew;

        //if (updateMode == 1 || bean.mapModify()==null){//直接 new dto时bean.mapModify()==null的
        if (updateMode == 1 || bean.origin()==null){
            return BeanUtil.bean2Map(bean,false);
        }

        TableInfo tableInfo = TableInfo.get(bean.getClass());
//        Dataset dataset=bean.getDataset();
//        String afterSaveReturnFields=dataset.tool.afterSaveReturnFields;
//        String updateFields         =dataset.tool.updateFields;

        //Set<String> row1Fields =bean.keySet();
        DataDTO rowOriginal=bean.origin();
        Map<String, Object> mapModify = bean.mapModify();//调用方提交的数据
        for (Map.Entry<String, Field> entry : tableInfo.getFields().entrySet()) {
            Field field = entry.getValue();
            String fieldName = entry.getKey();//字段名
            Object valueCurrent=getValue(tableInfo, bean, fieldName);//当前DTO值

            //Id字段 和版本号 始终放入Map
            if (tableInfo.getFieldsByIds().keySet().contains(fieldName) || fieldName.equals(tableInfo.getVersionFieldName())){
                rowNew.put(fieldName, valueCurrent);
                continue;
            }

            // 标记为 增量 的字段, 只要 有值, 就 始终认为是 修改 字段
            if ((field.getIncremental()== Incremental.ADDITION || field.getIncremental()== Incremental.SUBTRACT)
                    && valueCurrent != null) {
                rowNew.put(fieldName, valueCurrent);
                continue;
            }

            if(rowOriginal==null)continue;
            Object valueOriginal=BeanUtil.getValue(tableInfo, rowOriginal, fieldName);//原始值, 修改之前的值

//            //该字段是否是强制传回到前台的字段
//            if (isUpdateSQL==false && afterSaveReturnFields.indexOf(fieldName1)>=0 || fieldName1.indexOf("ModifyDate")>=0) {
//                rowNew.put(fieldName1, row1.get(fieldName1));
//            }
//            //该字段是否是强制更新的字段
//            if (isUpdateSQL==true && updateFields.indexOf(fieldName1)>=0) {
//                rowNew.put(fieldName1, row1.get(fieldName1));
//            }

            if (updateMode == 3 && mapModify!=null
                    && ( mapModify.containsKey(fieldName)
                      || mapModify.containsKey(field.getDbName())  ) //带下划线的字段名 也要判断
            ){
                //提交的数据 始终认为 是有变化的数据(实际不一定)
                rowNew.put(fieldName, valueCurrent);
                continue;
            }
//            //与数据库一样的值始终不需要;前台传回的值 后台替换掉呢
//            //if (isUpdateSQL==false && BeanUtil.compare(field, value1, valueOriginal)==0){
//            if (isUpdateSQL==false && BeanUtil.compare(field, valueCurrent, valueOriginal)==0){
//                continue;
//            }

//            if (Misc.isNotNull(valueCurrent) && row2.keySet().contains(fieldName1)==false){//rowOld不存在该字段，则视为该字段的值有变化
//                rowNew.put(field.getName(), row1.get(fieldName1));
//                continue;
//            }

//            if (valueCurrent==null && valueOriginal==null) continue;
//            if ((valueCurrent==null && valueOriginal!=null) || (valueCurrent!=null && valueOriginal==null)){
//                rowNew.put(fieldName, valueCurrent);
//                continue;
//            }

            long result=0L;
            try {
                result = BeanUtil.compare(field, valueCurrent, valueOriginal);
            }catch(Exception e){
                throw new RuntimeException("转换报错 fieldName="+fieldName+" value1="+valueCurrent.toString()+" value2="+rowOriginal.toString());
            }
            if (result!=0){
                rowNew.put(fieldName, valueCurrent);
            }
            /////////////////////////////////////////////////////////////////////////
        }

        return rowNew;
    }
    public static <T> T map2Bean(Map<String, Object> mapData, Class<T> clazz) {
        if (clazz==null || mapData==null) return null;
        T newBean = getEntity(clazz);
        map2Bean(mapData, newBean);
        return newBean;
    }

    public static void map2Bean(Map<String, Object> mapData, Object bean) {
        if (bean==null || mapData==null) return;
        TableInfo tableInfo = TableInfo.get(bean.getClass());
        Map<String,Field> fields = tableInfo.getFields();
        mapData.forEach((fieldName, value)->{
            Field field = fields.get(fieldName);
            if (field == null) {
                //如果前台提交的数据是xx_org_id 则转为 xxOrgId去获取字段对象Field
                field = fields.get(BeanUtil.toCamelCase(fieldName));
            }
            if (field != null) {
                setValue(tableInfo, bean, field, value);
            }
        });
    }

    public static <T> T getEntity(Class<T> clazz){
        if (clazz==null){
            throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("getEntity clazz不能为空");
        }

        T bean;
        try {
            bean =clazz.newInstance();
        } catch (Exception e) {
            throw CommonException.newInstance(ResponseCode.ERROR).text(clazz.getName()+".newInstance()报错");
        }

        return bean;
    }

    /**
     * 获得Bean的数据
     * @param bean
     * @return Map<String,Object>
     */
    public static Map<String,Object> bean2Map(Object bean) {
        if(bean==null)throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("bean2Map不能传入值是null的Bean");
        return bean2Map(new HashMap(),TableInfo.get(bean.getClass()),bean,false,false);
    }

    /**
     * 获得Bean的数据
     * @param bean
     * @param isNullValue2map 是否要获得 值为null的 数据
     * @return
     */
    public static Map<String,Object> bean2Map(Object bean, Boolean isNullValue2map) {
        if(bean==null)throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("bean2Map不能传入值是null的Bean");
        return bean2Map(new HashMap(),TableInfo.get(bean.getClass()),bean,isNullValue2map,false);
    }

    /**
     * 获得Bean的数据
     * @param bean
     * @param isNullValue2map 是否要获得 值为null的 数据
     * @return Map<String,Object>
     */
    public static Map<String,Object> bean2LinkedHashMap(Object bean, Boolean isNullValue2map){
        if(bean==null)throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("bean2Map不能传入值是null的Bean");
        return bean2Map(new HashMap(),TableInfo.get(bean.getClass()),bean,isNullValue2map,false);
    }

    /**
     *
     * @param mapData
     * @param tableInfo
     * @param bean
     * @param isNullValue2map
     * @param isJsonFormat 是否分局 @JsonFormat 注解格式化日期类型 的值为 字符串
     * @return
     */
    public static Map<String,Object> bean2Map(Map<String,Object> mapData, TableInfo tableInfo,
                                               Object bean, Boolean isNullValue2map, Boolean isJsonFormat){
        if(tableInfo==null)throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("bean2Map不能传入值是null的TableInfo");
        if(bean==null)throw CommonException.newInstance(ResponseCode.PARAM_IS_NULL).text("bean2Map不能传入值是null的Bean");
        Map<String,Field> fields=tableInfo.getFields();

        for (String key : fields.keySet()){
            Field field=fields.get(key);
            try {
                Method getter = bean.getClass().getMethod("get"+field.getRightName2());
                Object value = getter.invoke(bean);
                if (isNullValue2map==true){
                    mapData.put(key,value);
                }else if (value!=null){
                    if (isJsonFormat && field.getDataType() == DataType.DATE){
                        //日期格式化为 字符串,  解决QTO + XXXmapper.xml查询时, 老是有00:00:00的问题
                        //格式来自于 @JsonMormat
                        String format = field.getFormat();
                        format = Misc.isNull(format)?DateFormatter.DATE_FORMAT:format;
                        mapData.put(key, DateUtils.toDateStr(DataHelper.parseDate(value),format));
                    }else {
                        mapData.put(key, value);
                    }
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
                throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID).text("获取Bean数据报错");
            }
        }
        return mapData;
    }

    /**
     * POJO对象拷贝,将"源对象与目标对象的交集字段在 源对象的值 拷贝到目标对象
     * @param source 源头对象
     * @param dest   目标对象
     */
    public static void copyBean(Object source, Object dest){
        if(source==null || dest==null) return;
        TableInfo tableInfoSource=TableInfo.get(source.getClass());
        TableInfo tableInfoDest=TableInfo.get(dest.getClass());

        Map<String, Field> fieldsSource = tableInfoSource.getFields();
        Map<String,Field> fieldsDest = tableInfoDest.getFields();


        List list =fieldsSource.keySet().stream()
                /**筛选源头对象有哪些字段是存在目的对象连得*/
                .filter((key)->fieldsDest.containsKey(key))
                .collect(Collectors.toList());
        list.forEach((fieldName)->
                setValue(tableInfoDest,
                        dest,//要写值得对象
                        fieldsSource.get(fieldName),//要写的字段对象
                        getValue(tableInfoSource,source,fieldsSource.get(fieldName))//值,来自于source
                )
        );
    }

    /**
     * 在不改变传入进来两个对象的情况下, 实现以dest为主的数据合并, 产生一个新的对象,返回
     * @param source 源头对象
     * @param dest   目标对象
     * @return 返回一个新对象
     */
    public static Object copyNewBean(Object source, Object dest){
        Object destNew= cloneBean(dest);//目标对象克隆一个新的
        copyBean(source,destNew);       //将source的数据克隆到 destNew
        return destNew;
    }

    /**
     * 克隆对象
     * @param bean
     * @return
     */
    public static Object cloneBean(Object bean){
        if (bean == null) return null;
        Object dest= getEntity(bean.getClass());//创建一个空白对象
        copyBean(bean,dest);//将dest的数据克隆到 destNew
        return dest;
    }

    /**
     *
     * @param field
     * @param value
     * @param stringBuilder
     */
    public static void getSqlValue(Field field, String fieldName, Object value,StringBuilder stringBuilder){
        /**
         * field==null 说明是计算列字段(length(name)),则就当字符串拼接查找条件
         */
        if (field==null) {
            String stringValue=DataHelper.parseString2(value);
            //数字类型 和 boolean不要引号
            if (value instanceof Number || value instanceof Boolean){
                    stringBuilder.append(stringValue);
            }else{
                stringBuilder.append("'").append(stringValue).append("'");
            }
            return;
        }

        DataType dateType=field.getDataType();

        try{
            if (dateType==DataType.DATE  ) {
                //日期格式类型
                if (value==null) {
                    stringBuilder.append("null");
                }else {
                    //生成SQL 日期格式化: 根据 DTO注解@JsonFormat( pattern = "yyyy-MM-dd HH:mm")获得, 如果没有, 默认 yyyy-MM-dd
                    String format = field.getFormat();
                    format = Misc.isNull(format)?DateFormatter.DATE_FORMAT:format;
                    stringBuilder.append("'")
                            .append(DateUtils.toDateStr(DataHelper.parseDate(value), format))
                            .append("'");
                }
            }else if (dateType==DataType.DOUBLE || dateType==DataType.DECIMAL) {
                stringBuilder.append(DataHelper.roundToString(value, field.getFormat()));
            }else if (dateType==DataType.LONG) {
                stringBuilder.append(DataHelper.parseLong(value));
            }else if (dateType==DataType.INTEGER) {
                stringBuilder.append(DataHelper.parseInt(value));
            }else if (dateType==DataType.BOOLEAN) {
                stringBuilder.append(value);
            }else {
                String stringValue=DataHelper.parseString2(value);
                stringBuilder.append("'").append(stringValue).append("'");
            }
        }catch (NumberFormatException e){
            throw CommonException.newInstance(ResponseCode.PARAM_IS_INVALID).text(fieldName + "的值 "+value.toString()+" 不能转换为nubmer类型");
        }
    }


    /**
     * 驼峰命名转换--> 加上下划线
     */
    private static final char SEPARATOR = '_';
    public static String toUnderlineName(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean upperCase = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            boolean nextUpperCase = true;

            if (i < (s.length() - 1)) {
                nextUpperCase = Character.isUpperCase(s.charAt(i + 1));
            }

            if ((i >= 0) && Character.isUpperCase(c)) {
                if (!upperCase || !nextUpperCase) {
                    if (i > 0)
                        sb.append(SEPARATOR);
                }
                upperCase = true;
            } else {
                upperCase = false;
            }

            sb.append(Character.toUpperCase(c));
        }

        return sb.toString();
    }

    /**
     * 驼峰命名转换--> 去掉下划线
     */
    public static String toCamelCase(String s) {
        if (s == null) {
            return null;
        }

        s = s.toLowerCase();

        StringBuilder sb = new StringBuilder(s.length());
        boolean upperCase = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == SEPARATOR) {
                upperCase = true;
            } else if (upperCase) {
                sb.append(Character.toUpperCase(c));
                upperCase = false;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
