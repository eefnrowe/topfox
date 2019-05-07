package com.topfox.conf;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.topfox.common.AbstractRestSession;
import com.topfox.spring.RestSessionHandler;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.Misc;
import com.topfox.spring.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.*;
import java.lang.reflect.Method;
import java.util.Date;

public class WebMvcInterceptor implements HandlerInterceptor {
    Logger logger= LoggerFactory.getLogger(getClass());
    @Autowired
    private SysConfig sysConfigSource;//单实例读取值

    @Autowired
    RestSessionHandler restSessionHandler;

    @Autowired
    @Qualifier("sysRedisTemplateDTO")
    CustomRedisTemplate sysRedisTemplateDTO;

    @Autowired
    private SysConfig sysConfig;//单实例读取值

    public boolean checkStop(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod()) || request.getRequestURI().indexOf(".")>0){
            //跨域请求 发出的OPTIONS请求 或者 文件访问请求(如.html .js .css 等)
            return true;
        }

        if (request.getRequestURI().indexOf("/error")>=0){
            return true;//错误页面,哪里 发出的请求??
        }

        if (handler instanceof HandlerMethod == false) {
            return true;
        }
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        boolean checkStop = checkStop(request, response, handler);
        if (checkStop) return true;

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        logger.debug("{}{} {} {}", sysConfig.getLogPrefix(), request.getMethod(), request.getRequestURI(),handlerMethod.getMethod().toString());

        AbstractRestSession restSession = restSessionHandler.create();//创建 restSession
        Object ___requestData = request.getAttribute("___requestData");
        if (___requestData !=null && ___requestData instanceof  JSONObject) {
            restSession.setRequestData((JSONObject) ___requestData);//restSession.setRequestData(JSON.parseObject(___requestData.toString()));
            request.removeAttribute("___requestData");
        }

        Object ___headData = request.getAttribute("___headData");
        if (___headData !=null && ___headData instanceof  JSONObject) {
            restSession.setHeaders((JSONObject) ___headData);
            request.removeAttribute("___headData");
        }

        //restSession.beforeInit();

        ////////////////////////////////////////////////////////////////////////////
        BeanUtil.map2Bean(restSession.getRequestData(),restSession);
        //没有ExecuteId 执行Id/主事务号  自动生成一个
        restSession.setExecuteId(Misc.isNull(restSession.getExecuteId())?KeyBuild.getKeyID():restSession.getExecuteId());

        //restSessionHandler.set(restSession);
        restSessionHandler.initRestSession(restSession, method);

        //始终生成一个新的 SubExecuteId 子事务号
        restSession.setSubExecuteId(KeyBuild.getKeyID());
        restSession.setSysActiveDate(System.currentTimeMillis());
        restSession.setSysNowTime(new Date());
        //restSession.setSysNowTimeString(DateUtils.toDateStr(restSession.getSysNowTime(),"yyyy-MM-DD hh:mm:ss SSS"));
        restSession.setAppName(sysConfigSource.getAppName());


        //原始数据
        Object ___formData = request.getAttribute("___formData");
        if (___formData != null) {
            request.removeAttribute("___formData");
            JSONArray bodyDataArray = null;
            JSONObject bodyData=null;

            try {
                bodyDataArray=JSONObject.parseArray(___formData.toString());
            }catch(Exception e1){
                try {
                    bodyData = JSONObject.parseObject(___formData.toString());
                    if (bodyDataArray == null){
                        bodyDataArray = new JSONArray(1);
                    }
                    bodyDataArray.add(bodyData);
                }catch(Exception e2){
                }
            }
            restSession.setBodyData(bodyDataArray); //put post提交的formData数据
            if (bodyDataArray != null) {
                logger.debug("{}提交的formData数据: {}", sysConfig.getLogPrefix(), bodyDataArray.toString());
            }
        }

        /////////////////////////////////////////////////////////////////////////////

        //restSessionHandler.setDataCache(new DataCache(sysRedisTemplateDTO, sysConfig));//新建一二级缓存处理对象
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
        boolean checkStop = checkStop(request, response, handler);
        if (checkStop) return;

        restSessionHandler.getDataCache().commitRedis();//将增删改的DTO同步保存到redis中
        AbstractRestSession restSession=restSessionHandler.get();

        long end = System.currentTimeMillis();
        long activeDate=restSession==null?0:restSession.getSysActiveDate();
        logger.debug( "{}运行结束 共耗时{}毫秒",sysConfig.getLogPrefix(), Misc.fillStr((end - activeDate) + "", 4, " "));
    }

    /**
     * 判断浏览器是否支持 gzip 压缩
     * @param req
     * @return boolean 值
     */
    public boolean isGzipSupport(HttpServletRequest req) {
        String headEncoding = req.getHeader("accept-encoding");
        return headEncoding != null && (headEncoding.indexOf("gzip") != -1);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) throws Exception {
        boolean checkStop = checkStop(request, response, handler);
        if (checkStop) return;

        restSessionHandler.dispose();//释放 RestSession
    }
}
