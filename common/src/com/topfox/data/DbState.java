package com.topfox.data;

/**
 * 描述DO状态
 * i 新增
 * u 更新
 * d 删除
 * n 无状态
 */
public class DbState {
    /**
     * 新增
     */
    public static final String INSERT="i";
    /**
     * 删除
     */
    public static final String DELETE="d";
    /**
     * 修改
     */
    public static final String UPDATE="u";

    /**
     * 无状态
     */
    public static final String NONE  ="n";
}