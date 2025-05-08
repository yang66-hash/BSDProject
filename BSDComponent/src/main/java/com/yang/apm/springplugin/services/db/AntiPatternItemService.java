package com.yang.apm.springplugin.services.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yang.apm.springplugin.base.item.AntiPatternItem;
import com.yang.apm.springplugin.mapper.AntiPatternItemMapper;
import com.yang.apm.springplugin.services.dbimpl.IAntiPatternItemService;
import org.springframework.stereotype.Service;

@Service
public class AntiPatternItemService extends ServiceImpl<AntiPatternItemMapper, AntiPatternItem> implements IAntiPatternItemService {
}
