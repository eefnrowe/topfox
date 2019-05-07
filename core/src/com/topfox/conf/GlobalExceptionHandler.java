package com.topfox.conf;

import com.topfox.common.*;
import com.topfox.data.DataHelper;
import com.topfox.common.SysConfigRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.beans.PropertyEditorSupport;
import java.util.*;

//@EnableWebMvc
@RestControllerAdvice
public class GlobalExceptionHandler {
    static Logger logger= LoggerFactory.getLogger("com.topfox.conf.GlobalExceptionHandler");

    @Autowired
    @Qualifier("sysConfigDefault")
    protected SysConfigRead sysConfigRead;//单实例读取值 全局一个实例

    //日期转换 子类
    public class DateEditor extends PropertyEditorSupport {
        @Override
        public void setAsText(@Nullable String text) throws IllegalArgumentException {
            this.setValue(DataHelper.parseDate(text));
        }
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        //##################################################################
        //提交的数据 有下划线时,  转为驼峰//////////
        if (sysConfigRead.isCommitDataKeysIsUnderscore()) {
            Object data = binder.getTarget();
            if (data instanceof List) {
                List list = (List) data;
                list.forEach(row -> {
                    if (!list.isEmpty() && list.get(0) instanceof Map) {
                        CamelHelper.mapToCamel((Map) list.get(0));//处理下划线 转驼峰
                    }
                });
            }
            if (data instanceof Map) {
                CamelHelper.mapToCamel((Map) data);//处理下划线 转驼峰
            }
        }
        //##################################################################


        //解决前台 查询 GET 传入日期 2018/12/12 报错的问题
        binder.registerCustomEditor(Date.class, new DateEditor());
    }

    /**
     * 全局异常捕捉处理 : CommonException
     */
    @ExceptionHandler(value = CommonException.class)
    public Response commonException(CommonException ex) {
        if (ex.getFeignResponse() != null ){
            //解决 服务1调用服务2, 服务2调用失败, 服务1 接收到 respone 直接报出异常
            if (logger.isDebugEnabled()){
                //开发环境下,打印失败的信息
                ex.printStackTrace();
            }else{
                //生产环境下,  将日志用logback输出,  便于结合 ELK分布式日志系统 接收日志处理
                logger.error(ex.getFeignResponse().toString());
            }
            return ex.getFeignResponse();//服务1调用服务2, 服务2调用失败, 返回服务2的错误信息
        }else {
            return new Response(ex);
        }
    }

//    @ExceptionHandler(value = RuntimeException.class)
//    public Response commonException(RuntimeException ex) {
//        return new Response(ResponseCode.SYSTEM_ERROR," 禁止使用 RuntimeException 抛出异常.  请使用 throw CommonException.newInstance(ResponseCode.xxx).text(异常信息1,异常信息2)");
//    }


//    @ExceptionHandler(value = WebException.class)
//    public Response webExceptionHandler(WebException ex) {
//        return new Response(ex);
//    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public Response throwableHandler(HttpRequestMethodNotSupportedException ex) {
        return new Response<>(ex);
    }

    @ExceptionHandler(value = NullPointerException.class)
    public Response nullExceptionHandler(NullPointerException ex) {
        return new Response(ex, ResponseCode.NULLException);
    }

    /**
     * 全局异常捕捉处理 : Throwable
     */
    @ExceptionHandler(value = Throwable.class)
    public Response throwableHandler(Throwable ex) {
        String message =ex.getMessage();
        message=message==null?"":message;
        String find="Mapped Statements collection does not contain value for";

        int pos =message.lastIndexOf(find);
        if (pos>=0) {
            return new Response<>(ex,ResponseCode.PARAM_IS_INVALID, "可能是*Mapper.xml不存在对应的SQL的id:" + message.substring(pos + find.length()));
        }

        //服务1 调用服务2,  当服务2失败时,  服务器1自动抛出服务2的原始信息
        FeignResponse feignResponse = FeignResponse.getResponse();
        if (feignResponse != null){
            feignResponse.destroy();
            Response.getMethodString(ex);
            //                                 服务1的           |  服务2的
            feignResponse.setMethod(Response.getMethodString(ex) + " | " +feignResponse.getMethod());
            ex.printStackTrace();
            return feignResponse;
        }

        if (message.contains("TooManyResultsException")){
            //TooManyResultsException: Expected one result (or null) to be returned by selectOne()
            return new Response<>(ex,ResponseCode.DB_SELECT_ERROR, "selectOne查询不能返回多条记录");
        }

        if (message.contains("MySQLIntegrityConstraintViolationException")
                || message.contains("SQLIntegrityConstraintViolationException")){
            if(message.contains("Duplicate entry")){
                return new Response<>(ex,ResponseCode.DATA_IS_INVALID, "数据重复");
            }else if (message.contains("foreign key constraint fails")) {
                return new Response<>(ex,ResponseCode.DATA_IS_INVALID, "非法操作,不符合外键约束");
            }
            return new Response<>(ex);
        }
        if (message.contains("Data truncated for column")
                || message.contains("Out of range value for column")
                || message.contains("Data truncation: Incorrect datetime value") //日期超长
        ){
            return new Response<>(ex,ResponseCode.DATA_IS_INVALID, "输入的内容过长，超出设计限制");
        }else if (message.contains("Data too long for column")//2019.1.29 add 普通varchar字段 输入值过长
        ){
            return new Response<>(ex,ResponseCode.DATA_IS_INVALID, "输入的内容过长，超出设计限制");
        }else if (message.contains("error code [1366]")){
            return new Response<>(ex,ResponseCode.DATA_IS_INVALID, "输入的内容不合法，如表情符");
        }else if (message.contains("MySQLSyntaxErrorException")){
            return new Response<>(ex,ResponseCode.DB_SQL_ERROR, "SQL语句语法错误");
        }
        if (message.contains("MysqlDataTruncation")
                || message.contains("BIGINT UNSIGNED value is out of range in")
                    || message.contains("executeByUpdateSql-Inline")){
            return new Response<>(ex,ResponseCode.DB_UPDATE_ERROR, "相关库存不足");
        }
        return new Response<>(ex,ResponseCode.SYSTEM_ERROR, ex.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    public Response requestNotReadable(HttpMessageNotReadableException ex){
        String message =ex.getMessage();
        message=message==null?"":message;
        if (message.contains("com.fasterxml.jackson.core.JsonParseException")
                && (message.contains("JSON parse error") || message.contains("Unexpected character"))){
            return new Response<>(ex,ResponseCode.PARAM_IS_INVALID, "入参json格式错误!");
        }else if (message.contains("com.fasterxml.jackson.databind.exc.InvalidFormatException")
                && (message.contains("JSON parse error") || message.contains("Cannot deserialize value of type"))){
            return new Response<>(ex,ResponseCode.PARAM_IS_INEXISTENCE, "入参json参数value值类型错误!");
        }else if (message.contains("Required request body is missing")){
            return new Response<>(ex,ResponseCode.PARAM_IS_INEXISTENCE, "入参json格式为空!");
        }
        return new Response<>(ex);
    }
}
