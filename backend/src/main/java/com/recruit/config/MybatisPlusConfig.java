package com.recruit.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * 注册分页插件：JobController / DeliveryController / ResumeController 已使用 .page()/IPage 语法，
 * 此前缺插件导致"内存假分页"（SQL 不带 LIMIT，全量塞进 records）。
 * 加上该插件后自动升级为 SQL 级 LIMIT + COUNT 真分页，无需改动任何已有接口。
 *
 * maxLimit 500：防止调用方一次性拉取过大（既有前端代码以 size=200/500 模拟全量，需在此上限内）。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
