package com.topfox.spring;

import com.topfox.common.DataDTO;
import com.topfox.data.Field;

import java.util.List;
import java.util.Map;

public interface FillDataHandler {
    void fillData(Map<String,Field> fillFields, List<DataDTO> list);
}
