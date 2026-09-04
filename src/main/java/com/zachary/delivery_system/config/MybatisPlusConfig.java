package com.zachary.delivery_system.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor =
                new MybatisPlusInterceptor();

        // Supports @Version optimistic locking.
        interceptor.addInnerInterceptor(
                new OptimisticLockerInnerInterceptor()
        );

        PaginationInnerInterceptor paginationInterceptor =
                new PaginationInnerInterceptor(DbType.POSTGRE_SQL);

        // Prevent clients from requesting an excessively large page.
        paginationInterceptor.setMaxLimit(100L);

        // Pagination should normally be the last interceptor.
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}