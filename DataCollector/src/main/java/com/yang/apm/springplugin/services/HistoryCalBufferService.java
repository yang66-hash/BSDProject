package com.yang.apm.springplugin.services;

import com.yang.apm.springplugin.pojo.result.SvcRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@Service
@Slf4j
public class HistoryCalBufferService {

    private final BlockingDeque<SvcRes> buffer;

    @Autowired
    public HistoryCalBufferService() {
        this.buffer = new LinkedBlockingDeque<>(8);
    }

    // 存储计算的历史数据结果 以待发送到ES中 若是相应的存储索引不存在，那么直接接口创建



    //发送数据到ES中
}
