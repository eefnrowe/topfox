package com.topfox.data;

import com.topfox.annotation.*;
import com.topfox.common.*;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.Misc;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.beans.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

/**\
 * 本对象目的是 解析实体的结构
 */
public class TableInfo {
    public Class<?> clazzEntity;

    public SqlUnderscore sqlCamelToUnderscore=SqlUnderscore.OFF; //依据DTO生成SQL是否将驼峰命名转为下划线的大写名

    private String tableName;           //数据库库表名
    private String idFieldName;         //主键字段
    private String redisKey;            //缓存到redis数据库的表名, 开发者在DTO中自定义
    private String versionFieldName;    //版本号字段
    private String stateFieldName;      //不在数据库
    private String rowNoFieldName;      //不在数据库

    private Map<String, Field> fields;               //所有在数据库的字段集合
    private Map<String, Field> fillFields;           //所有注解填充的字段集合
    private Map<String, Field> fieldsForIncremental; //注解为 自增减的字段
    private Map<String, Field> fieldsByIds;          //多Id注解的field, 放入本map
    private Map<String, Method> mapSetter; //所有set方法对象集合
    private Map<String, Method> mapGetter; //所有get方法对象集合

    private boolean isDataDTO = false;     //是否DataDTO的子类, 紧紧用于检查是否标准的注解用, 如DataDTO 必须注解 表名, id字段
    //private static SysConfigRead sysConfigRead;//单实例读取值 全局一个实例

    /**
     * 获得标注 Incremental 的字段
     * @return
     */
    public Map<String,Field> getFieldsForIncremental(){
        return fieldsForIncremental;
    }

    private TableInfo(Class<?> clazz) {
        this.clazzEntity=clazz;
        //if(sysConfigRead==null) {
        //    sysConfigRead = ApplicationContextProvider.getBean("sysConfigDefault", SysConfigRead.class);
        //}
        //sqlCamelToUnderscore = sysConfigRead.isSqlCamelToUnderscore();

        if (Misc.isNull(tableName)) {
            Table table = clazz.getAnnotation(Table.class);
            if (table != null) {
                tableName = table.name();
                redisKey = table.redisKey();
            }
            if (table == null && isDataDTO) {
                mapTableInfo.remove(clazzEntity.getName());
                throw CommonException.newInstance(ResponseCode.ERROR)
                        .text(clazz.getName() + " 是DataDTO的子类, 必须设置 @Table(name = 表名)");
            }
        }
        if (Misc.isNull(idFieldName) && isDataDTO) {
            mapTableInfo.remove(clazzEntity.getName());
            throw CommonException.newInstance(ResponseCode.ERROR)
                    .text( clazzEntity.getName() + " 是DataDTO的子类, 必须设置  主键字段");
        }
    }

    /**
     * 获得对象所有的字段信息
     */
    private void initFields() {
        //获得所有层次类的Field
        List<java.lang.reflect.Field> listJavafield = new ArrayList<>(Arrays.asList(clazzEntity.getDeclaredFields()));
        Class<?> clazzParent=clazzEntity.getSuperclass();//得到父类
        while (clazzParent != null && !clazzParent.getName().toLowerCase().equals("java.lang.object")){
            if (isDataDTO == false && clazzParent.isAssignableFrom(DataDTO.class)){
                isDataDTO = true;
            }
            listJavafield.addAll(Arrays.asList(clazzParent.getDeclaredFields()));
            clazzParent = clazzParent.getSuperclass(); //得到父类,然后赋给自己
        }

//        Map<String, java.lang.reflect.Field> map = listJavafield.stream().collect(
//                Collectors.toMap(java.lang.reflect.Field::getName, Field->Field, (key1, key2) -> key1, LinkedHashMap::new));

        //过滤出 Id字段,增加到fields中, 目的是将Id字段排列到第一
        listJavafield.stream().filter((field) -> field.getName().equals("id")  || field.isAnnotationPresent(Id.class))
                .collect(toList()).forEach((jdkField)-> {
            idFieldName=jdkField.getName();
            Field field = new Field(null, idFieldName, DataType.getDataType(jdkField.getType()),sqlCamelToUnderscore);
            fields.put(idFieldName,     field);
            fieldsByIds.put(idFieldName,field);
        });
        //过滤出 version字段,增加到fields中, 目的是将version字段排列到第二
        listJavafield.stream().filter((field) -> field.getName().equals("version") || field.isAnnotationPresent(Version.class))
                .collect(toList()).forEach((jdkField)-> {
            versionFieldName=jdkField.getName();
            fields.put(versionFieldName, new Field(null, versionFieldName, DataType.getDataType(jdkField.getType()), sqlCamelToUnderscore));
        });

//        String fieldClass;
        //初始化 字段表结构
        listJavafield.forEach((jdkField)->{
            String name = jdkField.getName();
            if(name.equals("serialVersionUID")){return;}
            if(Modifier.isStatic(jdkField.getModifiers())){return;} //静态变量的字段剔除
            if (jdkField.getDeclaredAnnotation(Ignore.class)!=null){
                return;// 等同普通遍历的 continue
            }

            String fieldClass= jdkField.getType().getName();
            if (fieldClass.equals(List.class.getName()) || fieldClass.equals(ArrayList.class.getName())) {
                return;//明细容器的Field不能算作字段,否则将生成SQL(错误的)
            }

            TableField tableField;
            //识别出注解的字段
            if(jdkField.isAnnotationPresent(State.class)){
                stateFieldName=name;
                return;
            }else if(jdkField.isAnnotationPresent(RowId.class)) {
                rowNoFieldName=name;
                return;
            }else {
                tableField = jdkField.getDeclaredAnnotation(TableField.class);
                if (tableField != null) { //数据库不存在的指端  return
                    if (tableField.exist() == false) {
                        return;// 等同普通遍历的 continue
                    }
                }
            }

            if (name.indexOf("_")>0) {
                mapTableInfo.remove(clazzEntity.getName());
                throw CommonException.newInstance(ResponseCode.ERROR).text( clazzEntity.getName() + "."+name+" 不支持包含下划线, 请标准书写");
            }

            if (fields.containsKey(name)) return;
            Field field=new Field(tableField, name, DataType.getDataType(jdkField.getType()),sqlCamelToUnderscore);
            if (tableField != null ) {
                /**这里获得要填充的字段*/
                field.setFillInsert(tableField.fillInsert());
                field.setFillUpdate(tableField.fillUpdate());
                if (tableField.fillInsert() || tableField.fillUpdate()) {
                    fillFields.put(name, field);
                }

                /** 注解为 自增减的字段 **/
                if (tableField.incremental() == Incremental.ADDITION || tableField.incremental() == Incremental.SUBTRACT) {
                    if (!DataType.isNumber(field.getDataType())){
                        mapTableInfo.remove(clazzEntity.getName());
                        throw CommonException.newInstance(ResponseCode.ERROR).text( clazzEntity.getName() + "."+name+"类型不对,不能注解为自增减(必须是Long/Integer/Double/Decimal)");
                    }
                    field.setIncremental(tableField.incremental());
                    fieldsForIncremental.put(name, field);
                }
            }

            //获取注解的 格式化信息,  主要是 日期类型  小数精确位数的 格式化信息
            JsonFormat jsonFormat = jdkField.getDeclaredAnnotation(JsonFormat.class);
            if (jsonFormat != null) {
                field.setFormat(jsonFormat.pattern());
            }

            //实体的字段集合
            fields.put(name, field);
        });
    }


    /**
     * 获得 对象的所有 get set方法
     */
    private void initGetSetMethod() {
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(clazzEntity);
        } catch (IntrospectionException e1) {
            e1.printStackTrace();
        }

        MethodDescriptor[] methodDescriptors = beanInfo.getMethodDescriptors();
        for (MethodDescriptor methodDescriptor : methodDescriptors) {
            String fieldName;
            Method method = methodDescriptor.getMethod();
            String methodName = methodDescriptor.getName();

            // 把get set去掉, 并将剩下的第一个字母转为小写, 从而推算出字段名, 与DTO的字段名一致
            // boolean类型时, get 方法 lombok 会 生成isFieldName
            if (methodName.startsWith("is")){
                fieldName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
            }else if (methodName.startsWith("set") || methodName.startsWith("get")){
                fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            }else{
                continue;
            }
            Field field = getFields().get(fieldName);
//            if (field==null && !fieldName.equals(getRowNoFieldName()) && !fieldName.equals(getStateFieldName())) {
//                //解决字段名 叫isAdmin 不会自动生成 getIsAdmin的问题.  get方法就是 isAdmin();
//                fieldName = "is"+fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
//                field = fields.get(fieldName);
//            }
            if (fieldName.equals(getStateFieldName())
                    || fieldName.equals(getVersionFieldName())
                    || fieldName.equals(getRowNoFieldName())) {
                //系统定义的 3个字段, 不能执行 continue; 不然那 找不到get set方法,要报错. System.out.println();
            } else if (field == null) {
                continue;//说明不是 POJO定义的字段, 跳过.
            }

            if (methodName.startsWith("set")){
                mapSetter.put(fieldName,method);
            }else if(methodName.startsWith("get") || methodName.startsWith("is")){
                mapGetter.put(fieldName,method);
            }
        }
    }

    public Method getSetter(String fieldName){
        init();
        return mapSetter.get(fieldName);
    }
    public Method getGetter(String fieldName){
        init();
        return mapGetter.get(fieldName);
    }
    public String getIdFieldName(){
        init();
        return this.idFieldName;
    }

    public String getVersionFieldName(){
        init();
        return this.versionFieldName;
    }

    public String getStateFieldName(){
        init();
        return this.stateFieldName;
    }

    public String getRowNoFieldName(){
        init();
        return this.rowNoFieldName;
    }

    public String getTableName(){
        return this.tableName;
    }
    public String getRedisKey(){
        return Misc.isNull(this.redisKey)?clazzEntity.getName():redisKey;
    }

    private void init(){
        if (fields == null) {
            fields = new LinkedHashMap<>();
            fieldsForIncremental = new HashMap<>();
            fillFields = new HashMap<>();
            fieldsByIds= new LinkedHashMap<>();
            mapSetter = new HashMap<>();
            mapGetter = new HashMap<>();

            initFields();
            initGetSetMethod();
        }
    }
    public Map<String,Field> getFields(){
        init();
        return fields;
    }

    /**
     * 获得 主键字段,  考虑 多个的情况  增加
     * @return
     */
    public Map<String,Field> getFieldsByIds(){
        return fieldsByIds;
    }


    public Field getField(String fieldName){
        if (fieldName==null) return null;
        fieldName=fieldName.trim();
        return getFields().get(fieldName);
    }

    /**
     * 获得所有有新增 修改 填充的字段
     * @return
     */
    public Map<String,Field> getFillFields(){
        return fillFields;
    }

    //双锁
    private static ConcurrentHashMap<String,TableInfo> mapTableInfo;//缓存
    public static TableInfo get(Class<?> clazz){
        if (mapTableInfo==null){
            synchronized(TableInfo.class){
                if (mapTableInfo==null){
                    mapTableInfo=new ConcurrentHashMap();
                }
            }
        }

        TableInfo tableInfo=mapTableInfo.get(clazz.getName());
        if (tableInfo==null){
            synchronized (clazz){
                if (tableInfo==null) {
                    tableInfo=new TableInfo(clazz);
                    mapTableInfo.put(clazz.getName(),tableInfo);
                }
            }
        }
        return tableInfo;
    }

    public static TableInfo get(String clazzName){
        //拦截器 保存修改的DTO到redis使用
        return mapTableInfo.get(clazzName);
    }

    /**
     * 驼峰命名转换 为 有下划线的字段名
     * @param fieldName
     * @return
     */
    public String getColumn(String fieldName){
        Field field = getFields().get(fieldName);
        if (field !=null ){
            return field.getDbName();
        }else if (!fieldName.contains("_") && sqlCamelToUnderscore == SqlUnderscore.ON_UPPER){
            //开启 字段名依据驼峰命名转下划线, 并全部 大写
            return BeanUtil.toUnderlineName(fieldName).toUpperCase();
        }else if (!fieldName.contains("_") && sqlCamelToUnderscore == SqlUnderscore.ON_LOWER){
            //开启 字段名依据驼峰命名转下划线, 并全部 小写
            return BeanUtil.toUnderlineName(fieldName).toLowerCase();
        }else{
            return fieldName;
        }
    }


//    /**
//     * 主键字段 驼峰命名转换 为 有下划线的字段名
//     * @return
//     */
//    public String getIdColumn(){
//        if (sqlCamelToUnderscore){
//            return BeanUtil.toUnderlineName(getIdFieldName());
//        }
//        return getIdFieldName();
//    }


    //是否开启redis缓存
    boolean openRedis;
    public boolean openRedis(){
        return openRedis;
    }

    public void openRedis(boolean value){
        this.openRedis = value;
    }
}
