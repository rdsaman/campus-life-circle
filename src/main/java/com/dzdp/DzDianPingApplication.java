package com.dzdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.dzdp.mapper")
@SpringBootApplication
public class DzDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DzDianPingApplication.class, args);
    }

}
