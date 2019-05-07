package com.topfox.conf;


import com.topfox.common.*;
import com.topfox.data.DbState;
import com.topfox.spring.RestSessionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@ControllerAdvice
public class ResponseBodyAdviceConfig implements ResponseBodyAdvice {
    protected Logger logger= LoggerFactory.getLogger(getClass());

    @Autowired
    RestSessionHandler restSessionHandler;

    //处理返回的对象
    private void initResponse(Response response){
        //List list=new ArrayList(0);
        if (response.getData() == null) response.setData("");

        AbstractRestSession restSession=restSessionHandler.get();
        if (restSession == null) {
            logger.warn("restSession is null");
        } else {
            response.setExecuteId(restSession.getExecuteId());
        }

        if (response.attributes != null && response.attributes.size() == 0){
            response.attributes = null;
        }
    }

    /**
     * Closing non transactional SqlSession 事务结束之后才会执行
     * @return
     */
    @Override
    public Object beforeBodyWrite(Object result,
                                  MethodParameter methodParameter,
                                  MediaType mediaType,
                                  Class clazz,
                                  ServerHttpRequest httpRequest, ServerHttpResponse httpResponse) {
        //拦截控Cotroller层 返回的对象
        //Method method=methodParameter.getMethod();
        //Object value=method.getDefaultValue();

        //logger.debug("beforeBodyWrite");
        
        //不拦截swagger controller path地址
        if(httpRequest.getURI().getPath().startsWith("/swagger")){
            return result;
        }

        Response response;
        if(result instanceof Response || result instanceof List || result instanceof IBean) {
            if(result instanceof Response){
                //实现修改保存返回更新过的字段功能
                response=checkSave((Response)result);
            }else if(result instanceof List){
                response=new Response(result);
            }else{
                // POJO的DO BO VO QTO DTO 都要实现这个接口
                response=new Response(result);
            }
            initResponse(response);
            //logger.debug(response.toString());
            return response;

        }

        if(result instanceof LinkedHashMap){
            /**  如打印一下错误
                 {
                     timestamp=Mon Oct 22 10:28:04 CST 2018,
                     status=404,
                     error=Not Found,
                     message=No message available,
                     path=/static/bootstrap/bootstrap.min.css.map
                 }
             */
            logger.debug("");
            ((LinkedHashMap)result).forEach((key,value)->{
                logger.debug("{}",value);
            });
        }
        return result;
    }

    //实现修改保存返回更新过的字段功能
    private Response checkSave(Response response){
        Object dataCheckSave = response.getData();
        //判断DTO被更新过时
        AtomicBoolean isSaveDTO= new AtomicBoolean(false);
        List list = null;
        if (dataCheckSave instanceof List) {
            list = ((List) dataCheckSave);//当前页的行数
            if (list.isEmpty()) return response;
            list.forEach(row->{
                if (row instanceof DataDTO) {
                    DataDTO dto = (DataDTO)row;
                    if ( isSaveDTO.get() == false && dto.mapSave()!=null){ // 有 提交的数据的时
                        isSaveDTO.set(true);
                        return;
                    }

                    if (dto.mapSave() != null && DbState.UPDATE.equals(dto.dataState())) {
                        isSaveDTO.set(true);
                        return;
                    }
                }
            });
//            if (list.get(0) instanceof DataDTO && list.get(0).mapSave()!=null){
//                isSaveDTO.set(true);
//            }
        } else if (dataCheckSave instanceof DataDTO && ((DataDTO) dataCheckSave).mapSave()!=null) {
            isSaveDTO.set(true);
            list = new ArrayList(1);
            list.add(dataCheckSave);
        }

        if (list ==null || isSaveDTO.get() == false) {
            return response;
        }

        //把保存的数据 转成 Map, 以便只返回有 更新的字段给 前端(或调用者)
        List<Map<String, Object>> listOutSave = new ArrayList<>();
        list.forEach(row ->{
            if (row instanceof DataDTO) {
                DataDTO dto = (DataDTO)row;
                listOutSave.add(dto.mapSave());
            }
        });

        response.setData(listOutSave);
//        return new Response(response.isSuccess(),response.getCode(),
//                response.getMsg(),response.getExecuteId(), response.getTotalCount(), response.getPageCount(),
//                listOutSave, response.getException(), response.getMethod());

        return response;
    }


    @Override
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        return true;
    }
}
