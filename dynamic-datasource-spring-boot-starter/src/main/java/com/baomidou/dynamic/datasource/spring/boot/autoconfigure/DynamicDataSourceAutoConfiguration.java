/*
 * Copyright © 2018 organization baomidou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.dynamic.datasource.spring.boot.autoconfigure;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.strategy.DynamicDataSourceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.List;

/**
 * 动态数据源核心自动配置类
 *
 * @author TaoYu Kanyuxia
 * @see DynamicDataSourceProvider
 * @see DynamicDataSourceStrategy
 * @see DynamicRoutingDataSource
 * @since 1.0.0
 */
@Slf4j
@Configuration
// 需要在 DataSourceAutoConfiguration 自动配置前，注入我们的配置类 DynamicDataSourceAutoConfiguration
@AutoConfigureBefore(value = DataSourceAutoConfiguration.class, name = "com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure")
// 自动引入相关配置
@Import({DruidDynamicDataSourceConfiguration.class, DynamicDataSourceCreatorAutoConfiguration.class,
        DynamicDataSourceAopConfiguration.class, DynamicDataSourceAssistConfiguration.class})
// 当含有 spring.datasource.dynamic 配置时，这个自动配置类才生效
@ConditionalOnProperty(prefix = DynamicDataSourceProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class DynamicDataSourceAutoConfiguration implements InitializingBean {

    private final DynamicDataSourceProperties properties;

    private final List<DynamicDataSourcePropertiesCustomizer> dataSourcePropertiesCustomizers;

    public DynamicDataSourceAutoConfiguration(
            DynamicDataSourceProperties properties,
            ObjectProvider<List<DynamicDataSourcePropertiesCustomizer>> dataSourcePropertiesCustomizers) {
        this.properties = properties;
        this.dataSourcePropertiesCustomizers = dataSourcePropertiesCustomizers.getIfAvailable();
    }

    /**
     * 注册自己的动态多数据源 DataSource
     * @param providers
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(List<DynamicDataSourceProvider> providers) {
        // DynamicRoutingDataSource 是一个动态路由的数据源，内部维护的 dataMap 存放各个数据源信息
        DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource(providers);
        dataSource.setPrimary(properties.getPrimary());
        dataSource.setStrict(properties.getStrict());
        dataSource.setStrategy(properties.getStrategy());
        dataSource.setP6spy(properties.getP6spy());
        dataSource.setSeata(properties.getSeata());
        dataSource.setGraceDestroy(properties.getGraceDestroy());
        return dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        if (!CollectionUtils.isEmpty(dataSourcePropertiesCustomizers)) {
            for (DynamicDataSourcePropertiesCustomizer customizer : dataSourcePropertiesCustomizers) {
                customizer.customize(properties);
            }
        }
    }

}