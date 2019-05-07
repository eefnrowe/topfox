package com.topfox.db;

import com.topfox.common.DataDTO;
import com.topfox.sql.EntitySql;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class DbEntity<T extends DataDTO> {
    private EntitySql entitySql;
    public DbEntity (){
//        Class<T> clazz= ReflectUtils.getClassGenricType(getClass(),0);

//        System.out.println(clazz);
    }

    public T getT() {
        Type sType = getClass().getGenericSuperclass();
        Type[] generics = ((ParameterizedType) sType).getActualTypeArguments();
        Class<T> mTClass = (Class<T>) (generics[0]);
        try {
            return mTClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
