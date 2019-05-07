package com.topfox.spring;

import com.topfox.misc.BeanUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 对象source私有属性复制给destination对象的同名私有属性工具
 * 注意：只支持属性名完全相同的属性复制，支持忽略一些属性，其他需要手动get set
 * @author 明明如月 w1251314@sohu.com
 */
public class PropertyUtils {

    /**
     * 将source对象的属性填充到destination对象对应属性中
     * @param source 原始对象
     * @param destination 目标对象
     */
    public static <S,D> void copyProperties(S source, D destination)  {
        Class clsDestination;
        try {
            clsDestination = Class.forName(destination.getClass().getName());

            Class clsSource = Class.forName(source.getClass().getName());

            Field[] declaredFields = clsDestination.getDeclaredFields();

            for (Field field : declaredFields){
                field.setAccessible(true);
                String fieldName = field.getName();
                try{
                    //跳过serialVersionUID
                    if("serialVersionUID".equals(fieldName)){
                        continue;
                    }
                    Field sourceField = clsSource.getDeclaredField(fieldName);
                    sourceField.setAccessible(true);
                    field.set(destination,sourceField.get(source));

                }catch (NoSuchFieldException e){
                    // continue;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("PropertyUtils 属性转换错误");
        }

    }

    /**
     * 将source对象的属性填充到destination对象对应属性中
     * @param source 原始对象
     * @param destination 目标对象
     * @param ignoreProperties 不转换的属性
     */
    public static  <S,D> void copyProperties(S source, D destination,String... ignoreProperties)  {
        Class clsDestination ;
        try {
            clsDestination = Class.forName(destination.getClass().getName());

            Class clsSource = Class.forName(source.getClass().getName());

            Field[] declaredFields = clsDestination.getDeclaredFields();

            for (Field field : declaredFields){

                String fieldName = field.getName();

                Set<String> collect = Stream.of(ignoreProperties).collect(Collectors.toSet());
                //跳过serialVersionUID
                collect.add("serialVersionUID");

                if(collect.contains(fieldName)){
                    continue;
                }
                try{
                    field.setAccessible(true);
                    Field sourceField = clsSource.getDeclaredField(fieldName);
                    sourceField.setAccessible(true);
                    field.set(destination,sourceField.get(source));

                }catch (NoSuchFieldException e){
                    // 没有对应属性跳过;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("PropertyUtils 属性转换错误");
        }

    }

    public static <T> List<T> toList(Collection sourceList, Class<T> destinationClass) {
        List<T> destinationList = new ArrayList<>(sourceList.size());

        sourceList.forEach(sourceBean->{
            try {
                T desBean = destinationClass.newInstance();
                BeanUtil.copyBean(sourceBean,desBean);
                destinationList.add(desBean);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        });
        return destinationList;
    }
}


