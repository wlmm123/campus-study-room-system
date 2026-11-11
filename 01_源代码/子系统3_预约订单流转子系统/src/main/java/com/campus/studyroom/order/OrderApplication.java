package com.campus.studyroom.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campus.studyroom.order.mapper")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
        System.out.println("===================================================");
        System.out.println("🚀 校园自习室 - 订单流转子系统 (8083) 启动成功！");
        System.out.println("===================================================");
    }
}