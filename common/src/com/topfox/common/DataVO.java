package com.topfox.common;

import com.topfox.misc.JsonUtil;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public abstract class DataVO implements IBean, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public String toString(){
        return JsonUtil.toString(this);
    }
}
