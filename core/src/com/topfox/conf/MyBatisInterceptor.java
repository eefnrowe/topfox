package com.topfox.conf;

/**
 * mybaties拦截器  MyBatisInterceptor
 */

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

//@Intercepts({
//		@Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
//		@Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
//		@Signature(type = StatementHandler.class, method = "batch", args = { Statement.class })})
//@Component
//@Configuration

@Intercepts({
		@Signature(type = Executor.class, method = "update",args = {MappedStatement.class, Object.class }),
		@Signature(type = Executor.class, method = "query" ,
				args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class })
})
public class MyBatisInterceptor implements Interceptor {
	Properties properties;
	Logger logger= LoggerFactory.getLogger(getClass());

	public Object intercept(Invocation invocation) throws Throwable {
		//当前环境 MappedStatement，BoundSql，及sql取得
	    MappedStatement mappedStatement=(MappedStatement)invocation.getArgs()[0];


		String sqlId = mappedStatement.getId();
//		Object parameter = invocation.getArgs()[1];
//		BoundSql boundSql = mappedStatement.getBoundSql(parameter);
//		String namespace = sqlId.substring(0, sqlId.indexOf('.'));
//		Executor exe = (Executor) invocation.getTarget();
//		String methodName = invocation.getMethod().getName();
//		String originalSql = boundSql.getSql().trim();
//		Object parameterObject = boundSql.getParameterObject();
		if (logger.isDebugEnabled()) {
            //System.out.println();
            logger.debug(sqlId);
        }

		return invocation.proceed();

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ibatis.plugin.Interceptor#plugin(java.lang.Object)
	 */
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.ibatis.plugin.Interceptor#setProperties(java.util.Properties)
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}
