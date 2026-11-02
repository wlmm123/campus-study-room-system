package com.campus.studyroom.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 统一身份权限子系统启动类（节点A / 8081）。
 *
 * @author 成员1
 */
@SpringBootApplication
@MapperScan("com.campus.studyroom.auth.mapper")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
