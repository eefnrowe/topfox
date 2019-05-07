package com.topfox.common;

public enum ResponseCode {

    SUCCESS("200", "请求成功"),

    /** system exception */
    SYSTEM_ERROR("500", "系统异常"),
    ERROR("500", "发生异常"),

    /** param_key is inexistence */
    PARAM_IS_INEXISTENCE("400", "参数不存在"),

    /** param_value is null */
    PARAM_IS_NULL("400", "参数值为空"),

    /** param invalid */
    PARAM_IS_INVALID("400", "参数无效"),

    /** user_token is null/invalid */
    USER_TOKEN_ERROR("401", "用户未验证"),

    /** ###################################################### */

    JSON_TO_OBJECT("30020", "JSON字符串转对象报错"),
    OBJECT_TO_JSON("30022", "对象转JSON字符串报错"),
    STRING_TO_JSONMAP("30022", "字符串转JSON MAP报错"),
    DATA_IS_INVALID("30024","数据无效"),
    DATA_IS_NULL("30026","数据不能空白"),
    DATA_IS_DUPLICATE("30028","数据不能重复"),

    /**
     * dbsql exception error
     */
    DB_SELECT_ERROR("40002", "数据库查询错误"),
    DB_INSERT_ERROR("40004", "数据库插入出错"),
    DB_UPDATE_ERROR("40006", "数据库更新出错"),
    DB_DELETE_ERROR("40008", "数据库删除出错"),
    DB_SQL_ERROR("40010", "SQL语法错误"),

    /**
     * redis_lock exception error
     */
    ERROR_LOCK_KEY_IS_NULL("60002","获取锁，key不能为空"),
    ERROR_LOCK_TAKEN("60004","获取锁失败，已经被其他线程获取"),
    ERROR_UNLOCK_KEY_IS_NULL("60006","解锁，key不能为空"),
    ERROR_LOCK_EXCEPTION("60008","操作redis锁异常,请检查redis服务"),

    /**
     * topfox exception error
     */
    EX_RUNTIME_ERROR("90000", "RuntimeException异常"),
    EX_INTERRUPTED_ERROR("90010", "InterruptedException异常"),
    EX_IO_ERROR("90020", "IOException异常"),
    EX_ERROR("90030", "Exception异常"),
    NULLException("90050", "空指针异常"),

    /**
     * cloud hystrix exception error, 80000开头让给网关相关等
     */
    HYSTRIX_CODE("80000", "服务器繁忙, 请稍后再试"),
    HYSTRIX_GATEWAY("80002", "服务器繁忙, 请稍后再试"),
    HYSTRIX_SERVICE("80004", "服务器繁忙, 请稍后再试");

    private String code;
    private String msg;

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    ResponseCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
