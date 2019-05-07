package com.topfox.spring;

import com.topfox.common.SysConfigRead;
import com.topfox.data.SqlUnderscore;

/**
 * 实现类
 * @see  com.topfox.spring.SysConfigDefault
 */
public interface SysConfig extends SysConfigRead {
    
    void setPageSize(Integer value);

    void setMaxPageSize(Integer value);

    void setUpdateMode(Integer value);

    void setRedisSerializerJson(Boolean value);

    void setOpenRedis(Boolean value);

    void setSqlCamelToUnderscore(SqlUnderscore value);
}
