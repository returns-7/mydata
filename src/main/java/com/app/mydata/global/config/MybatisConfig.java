package com.app.mydata.global.config;

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisConfig {

    @Bean
    public ConfigurationCustomizer postgresqlDatabaseIdCustomizer() {
        return configuration -> configuration.setDatabaseId("postgresql");
    }
}
