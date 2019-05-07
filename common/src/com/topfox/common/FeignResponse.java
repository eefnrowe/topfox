package com.topfox.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FeignResponse<T> extends Response<T> {
    private static ThreadLocal<FeignResponse> threadLocal = new ThreadLocal();

    public FeignResponse(){
        super();
        threadLocal.set(this);
    }

    public static FeignResponse getResponse(){
        if (threadLocal == null ) return null;
        return threadLocal.get();
    }

    public static void destroy(){
        if(threadLocal !=null ) {
            threadLocal.set(null);
        }
    }


}
