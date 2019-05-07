package com.topfox.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    Logger logger=LoggerFactory.getLogger(getClass());

    /**
     * 自己定义的拦截器类
     * @return
     */
    @Bean
    WebMvcInterceptor webMvcInterceptor() {
        return new WebMvcInterceptor();
    }

    /**
     * 跨域支持  CORS
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
//        // 设置了可以被跨域访问的路径和可以被哪些主机跨域访问
//        registry.addMapping("/**")
//                .allowedOrigins("*");
//                //.allowedOrigins("http://localhost:8085");

        //设置允许跨域的路径
        registry.addMapping("/**")
                //设置允许跨域请求的域名
                .allowedOrigins("*")
                //是否允许证书 不再默认开启
                .allowCredentials(true)
                //设置允许的方法
                .allowedMethods("*");

//        registry.addMapping("**")
//        .allowedOrigins("*")
//        .allowedMethods("PUT”,"DELETE",”GET","POST")
//        .allowedHeaders("*").
//        exposedsedHeaders("access-control-allow-headers",
//        "access-control-allow-methods",
//        "access-control-allow-origin",
//        "access-control-max-age”,
//        "X-Frame-Options”）
//      .allowCredentials(false）.maxAge（3600）;


    }

    /**
     * 添加拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webMvcInterceptor());//.addPathPatterns("/api/**");
    }
//
//
//    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
//                "classpath:/META-INF/resources/",
//                "classpath:/resources/",
//                "classpath:/static/",
//                "classpath:/public/" };
//
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        if (!registry.hasMappingForPattern("/static/**")) {
//            registry.addResourceHandler("/static/**").addResourceLocations(
//                    "classpath:/META-INF/resources/static/");
//        }
//
//        if (!registry.hasMappingForPattern("/**")) {
//            registry.addResourceHandler("/**").addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);
//        }
//    }

//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        //这里配置无效
//    }




//    @Bean
//    public ServletRegistrationBean dispatcherRegistration(DispatcherServlet dispatcherServlet) {
//        ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet);
//        registration.getUrlMappings().clear();
//        registration.addUrlMappings("/rest/");
//        return registration;
//    }

//    @Bean
//    public ServletRegistrationBean apiServletBean(WebApplicationContext webapp) {
//        DispatcherServlet ds = new DispatcherServlet(webapp);
//        ServletRegistrationBean bean = new ServletRegistrationBean(ds, "/api/*");
//        bean.setName("api");
//        return bean;
//    }
}
