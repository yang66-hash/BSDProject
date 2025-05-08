package services;

import com.yang.apm.springplugin.services.CalculateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CalculateServiceTest {

    @Autowired
    private CalculateService calculateService;

    @Test
    public void test10MinCalculate() {
        calculateService.calculateAvg4ExternalMetrics();
    }

}