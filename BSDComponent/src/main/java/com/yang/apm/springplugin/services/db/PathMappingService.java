package com.yang.apm.springplugin.services.db;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yang.apm.springplugin.mapper.PathMappingServiceMapper;
import com.yang.apm.springplugin.pojo.codemapping.PathMappingServiceItem;
import com.yang.apm.springplugin.services.dbimpl.IPathMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PathMappingService extends ServiceImpl<PathMappingServiceMapper, PathMappingServiceItem> implements IPathMappingService {

}
