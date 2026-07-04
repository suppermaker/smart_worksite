package com.xd.smartworksite.datasource.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceInfraConfig {

    @Bean
    public ReadOnlySqlValidator readOnlySqlValidator() {
        return new ReadOnlySqlValidator();
    }
}
