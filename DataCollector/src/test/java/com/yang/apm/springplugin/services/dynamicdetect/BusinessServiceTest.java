package services.dynamicdetect;

import com.yang.apm.springplugin.constant.ResType;
import com.yang.apm.springplugin.pojo.result.business.BusinessMetricsRes;
import com.yang.apm.springplugin.services.RedisAsyncService;
import com.yang.apm.springplugin.services.dynamicdetect.BusinessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class BusinessServiceTest {

    @Autowired
    private BusinessService businessService;

    @Autowired
    RedisAsyncService redisAsyncService;

    @Test
    public void testBusinessService() {

        List<BusinessMetricsRes> businessMetrics = businessService.getBusinessMetrics("2025-04-29 16:05:00", 5);
        System.out.println(businessMetrics);
        redisAsyncService.saveT2Redis(businessMetrics, ResType.BUSINESS_METRICS.name(),"2025-04-29 16:05:00");
    }

}