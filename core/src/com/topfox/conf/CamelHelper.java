package com.topfox.conf;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.topfox.common.CommonException;
import com.topfox.common.ResponseCode;

import java.util.*;

/**
 * 驼峰命名处理工具包
 */
public class CamelHelper {

    /**
     * 下划线的 key 转 驼峰命名
     * @param source
     * @return
     */
    public static String toCamel(String source) {
        StringBuffer result = new StringBuffer();
        String a[] = source.split("_");
        for (String s : a) {
            if (result.length() == 0) {
                result.append(s.toLowerCase());
            } else {
                result.append(s.substring(0, 1).toUpperCase());
                result.append(s.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    public static String jsonDataToCamel(String jsonString) {
        String firstString = jsonString.substring(0,1);
        if (firstString.equals(" ")){
            jsonString = jsonString.trim();
            firstString = jsonString.substring(0,1);
        }
        //下划线的keys转为驼峰命名
        if (jsonString != null) {
            try {
                if (firstString.equals("[")) {
                    //传入的是 数组格式[{},{},{}...]
                    JSONArray jsonArrayNew=new JSONArray();
                    JSONArray jsonArray = JSON.parseArray(jsonString);//服务调用服务的格式
                    if (jsonArray == null) return jsonString;
                    for(int i=0;i<jsonArray.size();i++){
                        JSONObject row = jsonArray.getJSONObject(i);  // 遍历 jsonarray 数组，把每一个对象转成 json 对象
                        jsonArrayNew.add(mapToCamel(row));
                    }
                    return jsonArrayNew.toJSONString();
                }else if (firstString.equals("{")) {
                    //传入的是 对象格式 {}
                    JSONObject row = JSON.parseObject(jsonString);//服务调用服务的格式
                    if (row == null) return jsonString;
                    return mapToCamel(row).toJSONString();
                }
            } catch (Exception e) {
                throw CommonException.newInstance2(ResponseCode.STRING_TO_JSONMAP);
            }
        }

        return jsonString;
    }


    public static JSONObject mapToCamel(JSONObject row) {
        JSONObject rowNew = new JSONObject();
        row.forEach((key, value) -> {
            if (key.contains("_")) {
                rowNew.put(toCamel(key), value);//包含"_"的key 处理为驼峰命名
            } else {
                rowNew.put(key, value);
            }

//            if (value instanceof DataDTO){
//
//            }
        });
        return rowNew;
    }

    public static Map mapToCamel(Map<String, Object> row) {
        //将要处理的 带 下划线的 keys 和值放入到 mapTempData 容器中
        Map<String, Object> mapTempData = new HashMap<>();
        for(Map.Entry<String, Object> entry : row.entrySet()){
            String key = entry.getKey();
            if (key.contains("_")) {
                mapTempData.put(entry.getKey(),entry.getValue());
            }
        }


        for(Map.Entry<String, Object> entry : mapTempData.entrySet()){
            String key = entry.getKey();
            row.put(toCamel(key), entry.getValue()); //包含"_"的key 处理为驼峰命名
            row.remove(key);
        }

        return row;
    }
}
