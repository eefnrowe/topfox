package com.topfox.spring;

import com.topfox.common.AbstractRestSession;
import com.topfox.common.IRestSessionHandler;
import com.topfox.common.SysConfigRead;
import com.topfox.conf.CustomRedisTemplate;
import com.topfox.misc.BeanUtil;
import com.topfox.misc.ReflectUtils;
import com.topfox.sql.EntitySql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public abstract class RestSessionHandler<Session extends AbstractRestSession> implements IRestSessionHandler <Session>{
    protected Logger logger= LoggerFactory.getLogger(getClass());

    private ThreadLocal<Session> threadLocal = new ThreadLocal();
    private ThreadLocal<DataCache> threadLocalDataCache = new ThreadLocal();
    private ThreadLocal<Map<String,EntitySql>> threadLocalEntitySql = new ThreadLocal();

    @Autowired
    @Qualifier("sysRedisTemplateDTO")
    protected CustomRedisTemplate sysRedisTemplateDTO;

    @Autowired
    @Qualifier("sysStringRedisTemplate") //这个实例 已经 关闭事务了的
    protected CustomRedisTemplate sysStringRedisTemplate;

    @Autowired
    @Qualifier("sysConfigDefault")
    private SysConfigRead sysConfigRead;//单实例读取值 全局一个实例

    /**
     * 每个线程的开始,应该条用此方法;
     * 通一个线程禁止多次调用
     */
    @Override
    final public Session create(){
        //Thread.currentThread().getId();//获得当前线程的Id
        threadLocalEntitySql.set(new HashMap());//新建 EntitySql 处理对象
        threadLocalDataCache.set(new DataCache(sysRedisTemplateDTO, sysConfigRead));//新建一二级缓存处理对象

        Class<Session> clazz = ReflectUtils.getClassGenricType(getClass(), 0); //获得当前DTO的Class
        Session session = BeanUtil.getEntity(clazz);
        threadLocal.set(session);
        return session;
    }

    /**
     * 线程结束后释放,否则可能会造成 内存泄漏
     */
    @Override
    final public void dispose() {
        if (get() != null) {
            threadLocal.set(null);
            threadLocal.remove();
        }

        if (getDataCache() != null) {
            threadLocalDataCache.set(null);
            threadLocalDataCache.remove();
        }

        if (threadLocalEntitySql.get() != null) {
            threadLocalEntitySql.set(null);
            threadLocalEntitySql.remove();
        }
    }

    public abstract void initRestSession(Session session, Method method);


    @Override
    final public Session get() {
        return threadLocal.get();
    }

    public abstract void save(Session restSession);


    final public DataCache getDataCache() {
        DataCache dataCache = threadLocalDataCache.get();
        if (dataCache == null){
            dataCache = new DataCache(sysRedisTemplateDTO, sysConfigRead);
            threadLocalDataCache.set(dataCache);
        }
        return dataCache;
    }

    final public EntitySql getEntitySql(Class clazz) {
        Map<String,EntitySql> map = threadLocalEntitySql.get();
        if (map == null){
            map =new HashMap();
            threadLocalEntitySql.set(map);
        }

        EntitySql entitySql = map.get(clazz.getName());
        if (entitySql == null) {
            map.put(clazz.getName(), new EntitySql(clazz));
        }
        return map.get(clazz.getName());
    }
}
