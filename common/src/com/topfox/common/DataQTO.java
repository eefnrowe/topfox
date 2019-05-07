package com.topfox.common;

import com.topfox.misc.JsonUtil;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 基础QTO
 */
@Setter
@Getter
public class DataQTO implements IBean, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 允许返回null
     */
    private Boolean allowNull;// = true;

    /**
     * 记录偏移
     */
    private Integer pageIndex=0;

    /**
     * 每页条数
     */
    private Integer pageSize;


    /**
     * 类库专用字段,  不允许开发者 写值. 类库写值用
     */
    private String limit;

    /**
     * 排序字段, 多个字段用逗号串起来
     */
    private String orderBy;

    /**
     * 排序规则(ASC:升序, DESC:降序)
     */
    private String orderByRule;

    @Override
    public String toString(){
        return JsonUtil.toString(this);
    }

}
