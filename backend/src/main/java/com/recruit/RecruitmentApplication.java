package com.recruit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 职业院校校企招聘与就业管理平台启动类
 */
@SpringBootApplication
@MapperScan("com.recruit.mapper")
public class RecruitmentApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RecruitmentApplication.class, args);
        System.out.println("=========================================");
        System.out.println("招聘就业管理平台启动成功！");
        System.out.println("API文档: http://localhost:8080/api/doc.html");
        System.out.println("=========================================");
    }
}
