package com.zhiwu.dynamicthreadpollermanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Execution(ExecutionMode.SAME_THREAD)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DynamicThreadPollerManagerApplicationTests {

    @Test
    void contextLoads() {
    }

}
