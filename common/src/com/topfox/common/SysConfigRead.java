package com.topfox.common;

import com.topfox.data.SqlUnderscore;

/**
 * 实现类: com.topfox.spring.SysConfigDefault
 */
public interface SysConfigRead {

    String getAppName();

    int getPageSize();

    int getMaxPageSize();

    int getUpdateMode();

    boolean isRedisSerializerJson();

    boolean isOpenRedis();

    boolean isCommitDataKeysIsUnderscore();

    SqlUnderscore getSqlCamelToUnderscore();

    String getLogStart();

    String getLogEnd();

    String getLogPrefix();

}
