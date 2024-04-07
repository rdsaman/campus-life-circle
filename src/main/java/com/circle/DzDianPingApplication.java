package com.circle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true) // 暴露代理对象, 默认是不暴露的
@MapperScan("com.circle.mapper")
@SpringBootApplication
public class DzDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DzDianPingApplication.class, args);
    }

}
