package com.topfox.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.topfox.data.Field;
import com.topfox.data.TableInfo;
import com.topfox.misc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChangeManager {
    Map<String, ChangeData> mapChangeData;
    StringBuilder outStringBuilder;
    FormatConfig formatConfig;//格式化对象
    List<Field> fields;
    TableInfo tableInfo;
    DataDTO dto;
    AbstractRestSession restSession;

    Map<String, String> mapCnNames;
    Logger logger = LoggerFactory.getLogger(getClass());

    public ChangeManager(DataDTO dto, FormatConfig formatConfig, AbstractRestSession restSession){
        this.dto = dto;
        this.formatConfig = formatConfig;
        this.outStringBuilder = new StringBuilder();
        this.restSession = restSession;
        tableInfo = TableInfo.get(dto.getClass());

        Map<String, Object> mapChange = BeanUtil.getChangeRowData(dto);
        mapChangeData = new LinkedHashMap<>(mapChange.size());
        fields = new ArrayList<>();

        //addField(tableInfo.getIdFieldName());
        mapChange.forEach((fieldName, current)->{
            if (fieldName.indexOf("updateDate")==0) return;
            if (fieldName.equals(tableInfo.getVersionFieldName())) return;
            mapChangeData.put(fieldName, dto.newChangeData(fieldName, formatConfig));
            fields.add(tableInfo.getField(fieldName)); //有变化的字段
        });
    }

    public ChangeManager setFormatConfig(FormatConfig formatConfig){
        this.formatConfig = formatConfig;
        return this;
    }

    public ChangeManager addFieldLabel(String fieldName, String cnName){
        if (mapCnNames == null) mapCnNames = new HashMap<>();
        mapCnNames.put(fieldName.trim(), cnName);
        return this;
    }

    /**
     * 获得修改对象
     * @param field
     * @return
     */
    public ChangeData getChangeData(Field field){
        return getChangeData(field.getName());
    }

    public ChangeData getChangeData(String fieldName){
        if (formatConfig != null){
            return mapChangeData.get(fieldName).setFormatConfig(formatConfig);
        }
        return mapChangeData.get(fieldName);
    }

    /**
     * 获得所有修改的字段对象
     * @return
     */
    public List<Field> getFields(){
        return fields;
    }

    /**
     * 获得修改后的值
     * @param field
     * @return  null 将返回空字符
     */
    public String getStringByCurrent(Field field) {
        return mapChangeData.get(field.getName()).getCurrentToString();
    }

    /**
     * 获得修改前的值, 旧值
     * @param field
     * @return  null 将返回空字符
     */
    public String getStringByOrigin(Field field){
        return mapChangeData.get(field.getName()).getOriginToString();
    }

    public final  void beforeOut(StringBuilder outStringBuilder,  Map<String, Object> map){
    }

    public void initUpdateLog(StringBuilder outStringBuilder, Field field, String cnName, String currentValue, String originValue){
        if (outStringBuilder.length()>0){
            outStringBuilder.append(",");
        }
        outStringBuilder.append(cnName).append(":").append(originValue).append("->").append(currentValue);
        //outStringBuilder.append("修改时间").append(":").append(dto.);
    }



    public final  StringBuilder output(){
        beforeOut(outStringBuilder, null);

        getFields().forEach(field->{
            String cnName = null;
            if (mapCnNames!=null)
                cnName = mapCnNames.get(field.getName());
            cnName = cnName==null?field.getName():cnName;
            initUpdateLog(outStringBuilder, field, cnName, getStringByCurrent(field), getStringByOrigin(field));
        });
        return outStringBuilder;
    }

    /**
     * 分布式 日志用
     * @return
     */
    public final Map<String, Object> outputJOSN(){
        Map<String, Object> root = logger.isDebugEnabled()?new LinkedHashMap<>():new HashMap<>();
        Map<String, Object> outJSON = logger.isDebugEnabled()?new LinkedHashMap<>():new HashMap<>();
        beforeOut(null, outJSON);

        //Id字段 修改日志
        root.put("appName",restSession.getAppName());
        root.put("executeId",restSession.getExecuteId());
        root.put(tableInfo.getIdFieldName(), dto.dataId());

        //版本号修改信息/////////////////////////////////////////////////////////////
        ChangeData changeData = dto.newChangeData(tableInfo.getVersionFieldName());
        JSONObject jsonFieldVersion = new JSONObject();
        jsonFieldVersion.put("c",changeData.getCurrentToString());
        jsonFieldVersion.put("o",changeData.origin().getInt());
        outJSON.put(changeData.getField().getName(), jsonFieldVersion);
        ///////////////////////////////////////////////////////////////////////////


        getFields().forEach(field->{
            JSONObject temp = new JSONObject();
            temp.put("c",getStringByCurrent(field));
            temp.put("o",getStringByOrigin(field));
            outJSON.put(field.getName(), temp);

        });
        root.put("data",outJSON);
        return root;
    }

    public final String outJSONString(){
        return JSON.toJSONString(outputJOSN());
    }
}
