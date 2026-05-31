package com.zhiwu.dynamicthreadpollermanager;

import org.springframework.boot.SpringApplication;

public class TestDynamicThreadPollerManagerApplication {

    public static void main(String[] args) {
        SpringApplication.from(DynamicThreadPollerManagerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
