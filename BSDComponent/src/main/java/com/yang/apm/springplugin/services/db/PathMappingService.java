package com.yang.apm.springplugin.services.db;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yang.apm.springplugin.mapper.PathMappingServiceMapper;
import com.yang.apm.springplugin.pojo.PathMappingServiceItem;
import com.yang.apm.springplugin.services.dbimpl.IPathMappingService;
import org.springframework.stereotype.Service;

@Service
public class PathMappingService extends ServiceImpl<PathMappingServiceMapper, PathMappingServiceItem> implements IPathMappingService {

}
