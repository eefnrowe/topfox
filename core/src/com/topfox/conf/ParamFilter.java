package com.topfox.conf;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.topfox.misc.Misc;

import com.topfox.data.DataHelper;
import com.topfox.spring.ParameterRequestWrapper;
import com.topfox.common.SysConfigRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;


//@Component (value = "paramFilterStart")
//@ServletComponentScan
//@WebFilter(urlPatterns = "/*",filterName = "paramFilter")
public class ParamFilter implements Filter{
    Logger logger= LoggerFactory.getLogger(getClass());
    private javax.servlet.FilterConfig config;

    SysConfigRead sysConfigRead;
    public void setSysConfigRead(SysConfigRead sysConfigRead) {
        this.sysConfigRead =sysConfigRead;
    }

    @Override
    public void init(javax.servlet.FilterConfig filterConfig) {
        this.config = filterConfig;
        if (logger.isDebugEnabled()) {
            System.out.println();
        }

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        //HttpServletResponse response = (HttpServletResponse)servletResponse;

        if ("OPTIONS".equals(request.getMethod()) || request.getRequestURI().indexOf(".")>0){
            //跨域请求 发出的OPTIONS请求 或者 文件访问请求(如.html .js .css)
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }


        if (logger.isDebugEnabled()){
            System.out.println();
            logger.debug(sysConfigRead.getLogStart());
        }
        //logger.debug("获取 AbstractRestSession, 处理Request参数(去空格 OrEq AndNe OrLike AndNotLike)");

        /**
         * Eq:等   Ne:不等
         * 字段名后缀说明(加入该字段的值是 value1,value2)
         * fieldOrEq      则生成SQL: (field = valus1 OR field = value2)
         * fieldAndNe     则生成SQL: (field <> valus1 AND field = value2)
         * fieldOrLike    则生成SQL: (field LIKE CONCAT("%",valus1,"%") OR field LIKE CONCAT("%",valus2,"%"))
         * fieldAndNotLike 则生成SQL: (field NOT LIKE CONCAT("%",valus1,"%") AND field NOT LIKE CONCAT("%",valus2,"%"))
         *
         */

        JSONObject requestData = initRequestData(request);
        //ParameterRequestWrapper 获取 调用当 put post data(formData)的数据
        ParameterRequestWrapper requestWrapper = new ParameterRequestWrapper(request, sysConfigRead);

        //头信息批量处理
        Enumeration<String> headers = request.getHeaderNames();
        if(headers!=null){
            JSONObject headData = (JSONObject) request.getAttribute("___headData");
            if(headData==null){
                headData = new JSONObject();
            }
            String key,value;
            while (headers.hasMoreElements()) {
                key = headers.nextElement();
                value =request.getHeader(key);
                if(Misc.isNull(value) || key.equals("_")) continue;
                if(key.matches("x-.*.-.*")){
                    try {
                        value= URLDecoder.decode(value,"UTF-8");//转码，解决前端 中文的问题
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    headData.put(key, value);
                    requestWrapper.putHeader(key, value);

                    //version传递
                    if(key.matches("x-.*.-version")){
                        headData.put("version", value);
                        requestWrapper.putHeader("version", value);//转换为标准格式
                    }

                    //业务级头信息传递
                    if(key.matches("x-.*.-business-headers")){
                        String[] businessKeys = value.split("[,]");
                        if(0 < businessKeys.length){
                            for (String businessKey : businessKeys) {
                                if(Misc.isNotNull(request.getHeader(businessKey))){
                                    headData.put(businessKey, request.getHeader(businessKey));
                                    requestWrapper.putHeader(businessKey, request.getHeader(businessKey));
                                }
                            }
                        }
                    }
                }
            }
            request.setAttribute("___headData", headData);
        }

        if (request.getMethod().equals("GET")){
            Set<String> deleteKey=new HashSet();//记录值为空的key,便于移除key
            for (Map.Entry<String, Object> entry : requestData.entrySet()) {
                String key=entry.getKey();
                Object value=entry.getValue();
                if (key==null || "pageSize".equals(key)) continue;
                if (Misc.isNull(value)){
                    deleteKey.add(key);
                    continue;
                }

//                String fieldName;
                String valueString=value==null?"":value.toString().trim();//去掉参数值 左右的空格
//                //if (valueString !=null && valueString.indexOf("(")>=0) continue;
//                ArrayList<String> listValues=Misc.string2Array(valueString,",");
//                if (key.endsWith("OrEq")){
//                    fieldName=key.substring(0,key.length()-4);
//                    requestWrapper.setParameter(key,Misc.getWhereOrSql(valueString, fieldName));
//                }else if (key.endsWith("AndNe")) {
//                    fieldName=key.substring(0,key.length()-5);
//                    requestWrapper.setParameter(key,Misc.getWhereNotInSql(listValues, fieldName));
//                }else if (key.endsWith("OrLike")) {
//                    fieldName=key.substring(0,key.length()-6);
//                    requestWrapper.setParameter(key,Misc.getWhereLikeOrSql(listValues, fieldName));
//                }else if (key.endsWith("AndNotLike")) {
//                    fieldName=key.substring(0,key.length()-10);
//                    requestWrapper.setParameter(key,Misc.getWhereNotLikeSql(listValues, fieldName));
//                }else if (key.endsWith("pageIndex") && DataHelper.parseInt(value)>0) {
//                    //第1页, SQL语句是0
//                    requestWrapper.setParameter(key,String.valueOf(DataHelper.parseInt(value)-1));
//                }else if (key.endsWith("pageSize")) {
//
//                }else{
                    requestWrapper.setParameter(key,valueString);
//                }
            }

            /////////////////////////////////////////////////////////////////////////////////////////////////
            String pageSizeString=requestData.getString("pageSize");
            pageSizeString=Misc.isNull(pageSizeString)?"":pageSizeString.trim();
            int pageSize = sysConfigRead.getPageSize();//每页返回条数
            int maxPageSize = sysConfigRead.getMaxPageSize();//每页返回最大值条数
            if (pageSizeString.length()>0) {///用户传入有值时
                requestWrapper.setParameter("pageSize",
                        DataHelper.parseInt(pageSizeString)<=0?String.valueOf(maxPageSize):pageSizeString);
            }else{
                //参数没有值, 配置文件也没有,则默认100
                requestWrapper.setParameter("pageSize", String.valueOf(pageSize));
            }
            if (pageSizeString.length()>0 && DataHelper.parseInt(pageSizeString)<=0) {
                //pageSize<=0时,表示不分页,  则默认从第一页查
                requestWrapper.setParameter("pageIndex", "0");
            }
            ////////////////////////////////////////////////////////////////////////////////////////////////

            //删除值为空的key
            for (String key:deleteKey){
                requestData.remove(key);
            }
        }

        requestWrapper.setAttribute("___requestData", requestData);
        requestWrapper.setAttribute("___headData", request.getAttribute("___headData"));
        filterChain.doFilter(requestWrapper, servletResponse);

        logger.debug(sysConfigRead.getLogEnd());


////        restSession.setRequestData(requestData);
//        restSession.setRequest(request);
//        restSession.beforeInit();
//        if (logger.isDebugEnabled()) {
//            //Debug 开发模式下, 同时有多个请求时控制日志打印顺序不乱.  性能低下...
//            synchronized (this) {
//                filterChain.doFilter(requestWrapper==null?request:requestWrapper, servletResponse);
//            }
//        }else {
//            filterChain.doFilter(requestWrapper==null?request:requestWrapper, servletResponse);
//        }



    }

    @Override
    public void destroy() {
        this.config = null;
        logger.debug("ParamFilter.destroy");
    }



    final public JSONObject initRequestData(HttpServletRequest request) throws IOException {
        JSONObject requestData = new JSONObject();
        requestData.put("pageURL",request.getHeader("referer"));

        //头中传入信息的处理
        String headDataStr=request.getHeader("head");
        JSONObject headJSON;
        if (Misc.isNotNull(headDataStr)) {
            try {
                headDataStr= URLDecoder.decode(headDataStr,"UTF-8");//转码，解决前端 中文的问题

                //兼容两种格式
                try {
                    headJSON = JSON.parseObject(headDataStr);    //服务调用服务的格式
                }catch (Exception e){
                    headJSON=Misc.urlParameter2Map(headDataStr); //前端传入的格式
                }
                headJSON.forEach((key,value)->{
                    if(Misc.isNull(value) || key.equals("_")) return;
                    requestData.put(key,value);
                });
                //头信息 打印处理
                logger.debug("{}{} {} head={}",sysConfigRead.getLogPrefix(),request.getMethod(), request.getRequestURI(),headDataStr);
                request.setAttribute("___headData", headJSON);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        ////头信息 打印处理
        //StringBuilder sbHeadData = new StringBuilder();
        //sbHeadData.append(sysConfig.getLogPrefix()).append(request.getMethod()).append(" ").append(request.getRequestURI());
        //if (logger.isDebugEnabled() && headJSON!=null) {
        //    //处理排序问题, sessionId, executeId 排到前面
        //    JSONObject jsonPint = new JSONObject(true);
        //    if (headJSON.containsKey("sessionId")) jsonPint.put("sessionId", headJSON.get("sessionId"));
        //    if (headJSON.containsKey("executeId")) jsonPint.put("executeId", headJSON.get("executeId"));
        //    headJSON.forEach((key, Value) -> jsonPint.put(key, Value));
        //    sbHeadData.append(" head=").append(jsonPint.toJSONString());
        //}
        //logger.debug(sbHeadData.toString());

        String key,value;
        //前台ajax请求，会执行这里遍历request中的参数
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            key = params.nextElement();
            value =request.getParameter(key);
            if(Misc.isNull(value) || key.equals("_")) continue;

            // 下划线 转驼峰命名  request 中的值
            if (sysConfigRead.isCommitDataKeysIsUnderscore() && key.contains("_")) {
                key = CamelHelper.toCamel(key);
            }
            requestData.put(key,value);
        }

        return requestData;
    }
}
