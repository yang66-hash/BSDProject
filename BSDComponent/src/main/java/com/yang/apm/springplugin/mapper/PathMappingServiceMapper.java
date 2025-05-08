package com.yang.apm.springplugin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yang.apm.springplugin.pojo.codemapping.PathMappingServiceItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PathMappingServiceMapper extends BaseMapper<PathMappingServiceItem> {
}