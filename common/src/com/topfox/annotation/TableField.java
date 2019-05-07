package com.topfox.annotation;

import com.topfox.common.Incremental;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TableField {
//    /**
//     * 字段标题,即中文名
//     * @return
//     */
//    String label() default "";

    Incremental incremental() default Incremental.NONE;//生成更新SQL 加减  值:addition +  / subtract -

    /**
     * 是否数据库存在的字段
     * @return
     */
    boolean exist() default true;


    /**
     * 数据库的字段名注解
     * 注解后, 生成插入更新的SQL语句的字段名用这个属性的值
     * 使用此注解, 意味着DTO QTO定义的名字不是数据库的字段名(废话)
     * 此注解解析到 com.topfox.data.Field 对象的 DbName属性
     * @return
     */
    String name() default "";

//    /**
//     * 长度
//     * @return
//     */
//    int length() default 255;
//
//    /**
//     * 小数位数
//     * @return
//     */
//    int precision() default 0;
//
//    int scale() default 0;

//    /**
//     * 填充类型
//     * @return
//     */
//    FieldFill fill() default FieldFill.DEFAULT;

    /**
     * 更新填充
     * @return
     */
    boolean fillUpdate() default false;

    /**
     * 插入填充
     * @return
     */
    boolean fillInsert() default false;
}
