package com.topfox.common;

import java.lang.reflect.Method;

public interface IRestSessionHandler<Session extends AbstractRestSession> {

    void initRestSession(Session session, Method method);
    Session create();
    Session get() ;
//    void set(Session session);
    void dispose();
}
