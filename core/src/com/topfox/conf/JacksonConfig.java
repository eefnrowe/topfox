//package com.topfox.conf;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.List;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.databind.PropertyNamingStrategy;
//import com.topfox.misc.Misc;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.env.Environment;
//import org.springframework.http.MediaType;
//
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//
//@Configuration
//public class JacksonConfig {
//    Logger logger= LoggerFactory.getLogger(getClass());
//    @Autowired protected Environment environment;
//    String SNAKE_CASE = "";
//
//    @Bean
//    public MappingJackson2HttpMessageConverter getMappingJackson2HttpMessageConverter() {
//        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        //解决为 null的字段 不返回到控制层
//        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
//
//        //生成json字符串不加双引号 false,   需要  true
//        objectMapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
//
//        //驼峰命名 输出 驼峰转下划线
//        SNAKE_CASE = environment.getProperty("top.json.out.camel-to-underscore");
//        if (Misc.isNotNull(SNAKE_CASE) && SNAKE_CASE.equals("true")) {
//            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
//        }
//
//        //json传入的key在实体中不存在时, 忽略不报错
//        //objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//        //日期格式化 Bean 里面注解了日期的格式化, 因此这里 不需要 指定输出格式化了
//        //SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        //objectMapper.setDateFormat(dataFormat);
//        //objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));//解决时区差8小时问题
//
//        if (logger.isDebugEnabled()) {
//            //输出JSON格式化, 美观
//            mappingJackson2HttpMessageConverter.setPrettyPrint(true);
//        }
//
//        mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper);//设置中文编码格式
//        List<MediaType> list = new ArrayList();
//        list.add(MediaType.APPLICATION_JSON_UTF8);
//        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(list);
//        return mappingJackson2HttpMessageConverter;
//    }
//}
