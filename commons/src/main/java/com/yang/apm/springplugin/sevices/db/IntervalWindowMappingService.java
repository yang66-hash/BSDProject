package com.yang.apm.springplugin.sevices.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yang.apm.springplugin.mapper.IIntervalWindowMappingMapper;
import com.yang.apm.springplugin.pojo.IntervalWindowMapping;
import org.springframework.stereotype.Service;


@Service
public class IntervalWindowMappingService extends ServiceImpl<IIntervalWindowMappingMapper, IntervalWindowMapping> implements IIntervalWindowMappingService {


    public Integer getValueByName(String name){
        QueryWrapper<IntervalWindowMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        IntervalWindowMapping one = this.getOne(queryWrapper);
        return one!=null ?one.getValue():null;
    }

    public boolean setValueByName(String name,Integer value){
        // 检查是否已经存在记录
        IntervalWindowMapping mapping = this.getOne(
                new LambdaQueryWrapper<IntervalWindowMapping>().eq(IntervalWindowMapping::getName, name)
        );

        if (mapping != null) {
            // 如果存在记录，更新 value
            mapping.setValue(value);
            return this.updateById(mapping);  // 更新现有记录
        } else {
            // 如果记录不存在，插入新记录
            mapping = new IntervalWindowMapping();
            mapping.setName(name);
            mapping.setValue(value);
            return this.save(mapping);  // 插入新记录
        }

    }
}
