package com.topfox.misc;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.topfox.common.CommonException;
import com.topfox.common.ResponseCode;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class JsonUtil {
//    private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper objectMapper;
    static {
        objectMapper = new ObjectMapper();
        //去掉默认的时间戳格式
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        //设置为中国上海时区
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
//        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        //空值不序列化
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        //反序列化时，属性不存在的兼容处理
        objectMapper.getDeserializationConfig().withoutFeatures(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//        //序列化时，日期的统一格式
//        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm"));

        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //单引号处理
        objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);


        //objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);//设置字段可以不用双引号包括

    }

    public static <T> T toObject(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw CommonException.newInstance(ResponseCode.JSON_TO_OBJECT).text(e.getMessage());
        }
    }

    public static <T> T toCollection(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw CommonException.newInstance(ResponseCode.JSON_TO_OBJECT).text(e.getMessage());
        }
    }

    public static <T> String toJson(T entity) {
        return toJson(entity,"yyyy-MM-dd HH:mm");
    }

    public static <T> String toJson(T entity,String dataFormat) {
        try {
            objectMapper.setDateFormat(DateUtils.getDateFormat(dataFormat));
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            throw CommonException.newInstance(ResponseCode.OBJECT_TO_JSON).text(e.getMessage());
        }
    }

    public static <T> String toString(T entity) {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);//设置字段可以不用双引号包括
        return entity.getClass().getName() + " "+toJson(entity,"yyyy-MM-dd HH:mm:ss SSS");
    }
}