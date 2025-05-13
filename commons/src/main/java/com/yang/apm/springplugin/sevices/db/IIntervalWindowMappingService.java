package com.yang.apm.springplugin.sevices.db;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yang.apm.springplugin.pojo.IntervalWindowMapping;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IIntervalWindowMappingService extends IService<IntervalWindowMapping> {
}
