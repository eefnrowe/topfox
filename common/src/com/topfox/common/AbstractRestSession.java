package com.topfox.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.topfox.annotation.Id;
import com.topfox.annotation.Ignore;
import com.topfox.misc.*;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.*;

@Setter
@Getter
@Accessors(chain = true)
public abstract class AbstractRestSession extends DataDTO {
    private static final long serialVersionUID = 1L;

    @Id private String sessionId;
    @JsonIgnore transient private String executeId;       //分布式事务-主事务号
    @JsonIgnore transient private String subExecuteId;    //分布式事务-子事务号
    @JsonIgnore transient private String routeExecuteIds=""; //分布式事务-事务号调用链路/路径
    @JsonIgnore transient private String routeAppName="";

    @JsonIgnore transient private String appName=""; //当前的 spring.application.name

    @JsonIgnore @Ignore transient private String sysUserId="*";
    @JsonIgnore @Ignore transient private String sysUserName="*";
    @JsonIgnore @Ignore transient private String sysUserCode="";
    @JsonIgnore @Ignore transient private String sysUserMobile="";

    /** 第一次创建Session 的时间，即登陆系统的时间 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date sysCreateDate;

    /** 活动时间，登陆后，任何一次请求都会更新此时间*/
    @JsonIgnore @Ignore transient private long sysActiveDate=0;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonIgnore @Ignore transient private Date sysNowTime=null;

    /**
     * 自定义容器 Map, 用于临时放入的数据
     */
    @JsonIgnore @Ignore transient JSONObject attributes=new JSONObject();

    /****************************************
     * @JsonIgnore  //不需要转json的属性用这个 注解
     * @JsonIgnoreProperties(ignoreUnknown = true)
     * ******************************/
    //@JsonIgnore @Ignore transient static public long maxId=0;
    @JsonIgnore @Ignore transient private JSONObject requestData;
    @JsonIgnore @Ignore transient private JSONObject headers;
    @JsonIgnore @Ignore transient private JSONArray bodyData;

    /**********************************************************************/
    public AbstractRestSession(){
        requestData = new JSONObject();
    }

    @Deprecated
    public void init(){}

    @Override
    public String toString(){
        return JsonUtil.toString(this);
    }

}
