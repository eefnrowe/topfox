package com.topfox.spring;

import com.topfox.common.*;
import com.topfox.data.*;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.DateFormatter;
import com.topfox.misc.DateUtils;
import com.topfox.misc.Misc;
import com.topfox.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CheckData<B extends DataDTO> {
    private TableInfo tableInfo;   //得到表结构
    private SimpleService service;
    private Logger logger=LoggerFactory.getLogger(getClass());
    private List<B> list;
    private Condition where;
    private String errText="";
    ;
    public CheckData(List<B> list, TableInfo tableInfo, SimpleService service){
        this.list=list;
        this.tableInfo=tableInfo;
        this.service=service;
    }
    /**
     * 创建一个新的条件对象Condition
     * @see Condition
     * @return
     */
    public Condition where(){
        return Condition.create(tableInfo.clazzEntity);
    }

    Map<String,String> mapFields=null;
    public CheckData setFields(String... fields){
        Set<String> setFields = Misc.array2Set(fields);//传入 array2Set("01,02","03,01","02")是 返回 set 01,02,03
        for (String field : setFields){
            addField(field, field);
        }
        return this;
    }
    public CheckData addField(String name,String label){
        if (list==null) {
            throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID)
                    .text("请先设置检查的数据,例: checkData().setData(bean/list)");
        }
        mapFields=mapFields==null?mapFields=new LinkedHashMap<>():mapFields;
        mapFields.put(name,label);
        return this;
    }


    public CheckData setErrText(String errText){
        this.errText=errText;
        return this;
    }


    public CheckData setWhere(Condition where){
        if (mapFields==null) {
            throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID).text("请先设置检查的字段,例: checkData().setField(name,label)");
        }
        this.where=where;
        return this;
    }


    public String excute() {
        return excute(true);
    }

    /**
     * 检查每行数据 是否有 修改
     * @param dto
     * @return
     */
    private boolean checkChange(B dto){
        boolean isChange=false;
        for (String field : mapFields.keySet()){
            if (dto.checkChange(field)==true){
                isChange=true;
                break;
            }
        }

        return isChange;
    }
    /**
     *
     * @param isThrowNewException 是否抛异常
     * @return 返回错误信息
     */
    public String excute(boolean isThrowNewException){
        if (mapFields==null) {
            throw CommonException.newInstance(ResponseCode.DATA_IS_INVALID)
                    .text("请先置检查的字段,例: checkData().setField(name,label)");
        }

        if (!list.isEmpty())
        if (list.get(0) instanceof DataDTO ==false){
            return "";
        }

        String idField=tableInfo.getIdFieldName();
        ArrayList<String> arrayIds=new ArrayList<>();
        ArrayList<String> arrayOldIds=new ArrayList<>();

//        Integer count;
        Map<String,Integer> valueCount =new LinkedHashMap();//检查前台传回来的值是否是否有重复 Map<值,出现次数>
        String uniqueCols=mapFields.keySet().toString().replace("[","").replace("]","");
        String check_cols2=uniqueCols.replaceAll(",",",'-',");

        list.forEach((bean)->{
            String state =bean.dataState();
            //循环前台传回的数据
            if (bean == null) return;
            if (state == null) return;
            if(!state.equals(DbState.UPDATE) && !state.equals(DbState.INSERT)) return;//非更新和新增，则跳过
            if (checkChange(bean)==false) return;//检查的字段 值 都 没有修改,则return

            if(state.equals(DbState.UPDATE)) {
                arrayOldIds.add(bean.dataId());//旧值
            }

            String ls_value = getValueByCols(bean, uniqueCols, "-");
            Integer count=valueCount.get(ls_value);
            valueCount.put(ls_value,(count==null?0:count) +1);
            if (Misc.isNull(bean.dataId())==false) arrayIds.add(bean.dataId());

        });
        if (valueCount.size() == 0) return null;//检查的字段没有修改时, 则 return

        if (where==null) where=where();
        where.eq("concat("+check_cols2+")",valueCount.keySet().toArray());

        if (arrayOldIds.size()>0 && idField.equals(uniqueCols)){//只有一个主键字段，检查重复
            where.ne(idField,arrayOldIds);
        }else if (arrayIds.size()>0) {//非主键的一个或者多个字段检查重复
            where.ne(idField,arrayIds.toArray());
        }

        List<Map<String,Object>> list2=service.selectForListMap("concat("+check_cols2+") result", where);
        list2.forEach((map)->{
            if (uniqueCols !=null){
                String ls_value = map.get("result").toString();
                Integer count=valueCount.get(ls_value);
                valueCount.put(ls_value,(count==null?0:count) +1);
            }
        });

        String err="";
        if (errText==null || errText.length()<=0) {
            for (String valueString : valueCount.keySet()) {
                if ("*".equals(valueString) || valueCount.get(valueString) <= 1) continue;
                err = err + "{" + valueString + "}";
            }
        }
        if (err.length()>2) {//检查提交行数是否存在重复值
            if (!isThrowNewException){
                return err;
            }else {
                logger.debug(uniqueCols + " " + err);
                if (errText != null && errText.length() > 2) {
                    logger.debug(errText + " 详细信息：" + getFieldLabel(tableInfo, uniqueCols, true) + "的值" + err);
                    throw CommonException.newInstance(ResponseCode.DATA_IS_DUPLICATE)
                            .text(errText);
                } else {
                    throw CommonException.newInstance(ResponseCode.DATA_IS_DUPLICATE)
                            .text("提交数据", getFieldLabel(tableInfo, uniqueCols, true), "的值", err, "不可重复");
                }
            }
        }else{
            return null;//无错误
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 根据Dataset 的field名字返回fieldLable
     *
     * @param tableInfo
     * @param fieldName field名字
     * @return  field标题 fieldLable
     * @throws Exception
     */
    private String getFieldLabel(TableInfo tableInfo, String fieldName){
        if (tableInfo.clazzEntity==null){
            System.out.println("tableInfo.clazzEntity==null, 无法获取字段"+fieldName+"的中文名");
            return fieldName;
        }
        Field field =tableInfo.getField(fieldName);
        if (field==null)return fieldName;

        String label=mapFields.get(fieldName);
        if (fieldName.equals(label)){
            label=field.getLabel();
        }
        return Misc.isNull(label)?fieldName:label;
    }
    /**
     * 根据Dataset 的field名字返回fieldLable
     *
     * @param tableInfo
     * @param cols field名字
     * @param isMark 是否加上标签
     * @return  field标题 fieldLable 如isMark＝true则返回 「金额」...isMark＝false则返回金额
     */
    private String getFieldLabel(TableInfo tableInfo, String cols, boolean isMark){
        String colsArray[] = cols.split(",");
        String returnLabels="";
        for (int i=0; i<colsArray.length;i++){
            if (isMark) returnLabels = returnLabels + "{"+getFieldLabel(tableInfo, colsArray[i])+"}";
            else returnLabels = returnLabels + " " + getFieldLabel(tableInfo, colsArray[i]);
        }
        return returnLabels;
    }

    private String getValueByCols(IBean bean, String cols, String splitString){
        String colsArray[] = cols.split(",");
        String retValue = "";
        String value;
        Map<String,Field> fields=tableInfo.getFields();
        for (int i=0; i<colsArray.length;i++){
            Field field=fields.get(colsArray[i].trim());
            value = BeanUtil.getValue2String(tableInfo, bean, field);
//luojp 2019-01-10
//            if (field!=null && value.length()>10 && (field.getDataType()== DataType.DATE)){
//                value=value.substring(0,10);
//            }

            if (i<colsArray.length-1) value=value+splitString;
            retValue = retValue + value;
        }

        return retValue;
    }


    public String checkNotNull() throws CommonException {
        return checkNotNull(true);
    }
    public String checkNotNull(boolean isThrowNewException) throws CommonException {
        if (mapFields==null) CommonException.newInstance(ResponseCode.DATA_IS_INVALID).text("请先设置检查的字段,例: checkData().setField(name,label)");
        Set<String> setFields=new HashSet();
        StringBuilder stringBuilder=new StringBuilder();
        for(int i=0; i<list.size();i++){
            Object bean=list.get(i);
            mapFields.keySet().forEach((fieldName) -> {
                if (Misc.isNull(BeanUtil.getValue(bean, fieldName))) {
                    setFields.add(fieldName);
                    //stringBuilder.append("{").append(mapFields.get(fieldName)).append("},");
                }
            });
        }

        setFields.forEach((fieldName)->{
            stringBuilder.append("{").append(mapFields.get(fieldName)).append("},");
        });
        if (setFields.size()>0){
            //String notFields=setFields.toString().replace("[","").replace("}","");
            //notFields=notFields.replace(",","},{");
            stringBuilder.setLength(stringBuilder.length()-1);//去掉最后的逗号和空格
            if (isThrowNewException) {
                throw CommonException.newInstance(ResponseCode.DATA_IS_NULL).text(stringBuilder.append("不能为空！").toString());
            }else{
                return stringBuilder.append("不能为空！").toString();
            }

        }
        return null;
    }
}
