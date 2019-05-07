package com.topfox.common;

import java.io.Serializable;

public interface ResponseDO<T> extends Serializable {

    Boolean isSuccess();

    String getCode();

    String getMsg();

    String getError();

    Integer getPageCount();

    Integer getTotalCount();

    T getData();

}
