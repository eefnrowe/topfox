package com.topfox.conf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topfox.common.SysConfigRead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisConfig {
//	@Value("${top.redis.serializer-json}")
//	private Boolean hashValueSerializerIsJson;

	@Autowired
	@Qualifier("sysConfigDefault")
	protected SysConfigRead sysConfigRead;//单实例读取值 全局一个实例

	Jackson2JsonRedisSerializer jackson2JsonRedisSerializer2=null;
	private Jackson2JsonRedisSerializer getJacksonSerializer(){
		if (jackson2JsonRedisSerializer2==null) {
			jackson2JsonRedisSerializer2 = new Jackson2JsonRedisSerializer(Object.class);
		}

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer2.setObjectMapper(objectMapper);
		return jackson2JsonRedisSerializer2;
	}

	/**
	 * 开启事务的
	 * @param redisConnectionFactory
	 * @return
	 */
	@Bean("sysRedisTemplateDTO")//开启事务的
	public CustomRedisTemplate<String, Object> sysRedisTemplateDTO(LettuceConnectionFactory redisConnectionFactory) {
		CustomRedisTemplate<String, Object> sysRedisTemplateDTO = new CustomRedisTemplate();
		sysRedisTemplateDTO.setConnectionFactory(redisConnectionFactory);
		sysRedisTemplateDTO.setEnableTransactionSupport(false);//打开事务支持

//		sysRedisTemplateDTO.setDefaultSerializer(new StringRedisSerializer());
		sysRedisTemplateDTO.setHashKeySerializer(RedisSerializer.string());//key值始终用纯字符串序列化
		sysRedisTemplateDTO.setHashValueSerializer(sysConfigRead.isRedisSerializerJson()?getJacksonSerializer():new JdkSerializationRedisSerializer());
//		sysRedisTemplateDTO.setHashValueSerializer(getJacksonSerializer());

		sysRedisTemplateDTO.setKeySerializer(RedisSerializer.string());//key值始终用纯字符串序列化
//		sysRedisTemplateDTO.setValueSerializer(getJacksonSerializer());

		sysRedisTemplateDTO.setValueSerializer    (sysConfigRead.isRedisSerializerJson()?getJacksonSerializer():new JdkSerializationRedisSerializer());
		sysRedisTemplateDTO.afterPropertiesSet();//初始化操作）加载配置后执行

		return sysRedisTemplateDTO;//redisTemplateDTO
	}

	/**
	 * 注意,这个是没有事务的
	 */
	@Bean("sysStringRedisTemplate")
	public CustomRedisTemplate sysStringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
		CustomRedisTemplate<String, Object> sysStringRedisTemplate = new CustomRedisTemplate();
		sysStringRedisTemplate.setConnectionFactory(redisConnectionFactory);
		sysStringRedisTemplate.setEnableTransactionSupport(false);//打开事务支持

		sysStringRedisTemplate.setKeySerializer(RedisSerializer.string());
		sysStringRedisTemplate.setValueSerializer(RedisSerializer.string());
		sysStringRedisTemplate.setHashKeySerializer(RedisSerializer.string());
		sysStringRedisTemplate.setHashValueSerializer(RedisSerializer.string());

		sysStringRedisTemplate.afterPropertiesSet();//初始化操作）加载配置后执行


//		CustomRedisTemplate sysStringRedisTemplate = new CustomRedisTemplate();
//		sysStringRedisTemplate.setConnectionFactory(redisConnectionFactory);
//		sysStringRedisTemplate.setEnableTransactionSupport(true);//打开事务支持
//
////		RedisSerializer<String> stringSerializer = new StringRedisSerializer();
////		redisTemplateString.setKeySerializer(stringSerializer);
////		redisTemplateString.setValueSerializer(stringSerializer);
//////		redisTemplateString.setValueSerializer(hashValueSerializerIsJson?getJacksonSerializer():new JdkSerializationRedisSerializer());
////		redisTemplateString.afterPropertiesSet();//初始化操作）加载配置后执行
//
//		sysStringRedisTemplate.setEnableTransactionSupport(false);//打开事务支持
//
		return sysStringRedisTemplate; //stringRedisTemplate
	}



//	@Bean
//	CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
//		//本文来自 yingziisme 的CSDN 博客
//		// 全文地址请点击：https://blog.csdn.net/yingziisme/article/details/81463391?utm_source=copy
//
//		/* 默认配置， 默认超时时间为30s */
//		RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
//				.entryTtl(Duration.ofSeconds(30L))
//				.disableCachingNullValues();
//
//		/* 配置test的超时时间为120s*/
//		RedisCacheManager cacheManager = RedisCacheManager.builder(
//				RedisCacheWriter.lockingRedisCacheWriter(connectionFactory))
//				.cacheDefaults(defaultCacheConfig)
//				.withInitialCacheConfigurations(
//						singletonMap("test",
//								RedisCacheConfiguration.defaultCacheConfig()
//										.entryTtl(Duration.ofSeconds(120L)).disableCachingNullValues()
//						)
//				)
//				.transactionAware().build();
//
//		return cacheManager;
//	}
}
