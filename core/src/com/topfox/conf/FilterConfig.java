package com.topfox.conf;

import com.topfox.spring.SysConfig;
import com.topfox.common.SysConfigRead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FilterConfig {

    @Autowired
    @Qualifier("sysConfigDefault")
    protected SysConfigRead sysConfigRead;//单实例读取值 全局一个实例
    @Bean
    public FilterRegistrationBean registFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();

        ParamFilter paramFilter = new ParamFilter();
        paramFilter.setSysConfigRead(sysConfigRead);
        registration.setFilter(paramFilter);
        registration.addUrlPatterns("/*");
        registration.setName("paramFilter");
        registration.setOrder(1);
        return registration;
    }

}