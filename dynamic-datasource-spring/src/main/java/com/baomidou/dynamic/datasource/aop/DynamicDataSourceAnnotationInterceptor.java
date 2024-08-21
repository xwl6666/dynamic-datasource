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
package com.baomidou.dynamic.datasource.aop;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.processor.DsProcessor;
import com.baomidou.dynamic.datasource.support.DataSourceClassResolver;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Core Interceptor of Dynamic Datasource
 * 拦截器，拦截 @DS 注解
 *
 * @author TaoYu
 * @since 1.2.0
 */
public class DynamicDataSourceAnnotationInterceptor implements MethodInterceptor {

    /**
     * The identification of SPEL.
     */
    private static final String DYNAMIC_PREFIX = "#";

    private final DataSourceClassResolver dataSourceClassResolver;
    private final DsProcessor dsProcessor;

    /**
     * init
     *
     * @param allowedPublicOnly allowedPublicOnly
     * @param dsProcessor       dsProcessor
     */
    public DynamicDataSourceAnnotationInterceptor(Boolean allowedPublicOnly, DsProcessor dsProcessor) {
        dataSourceClassResolver = new DataSourceClassResolver(allowedPublicOnly);
        this.dsProcessor = dsProcessor;
    }

    // 通过 AOP 实现方法级别的动态数据源切换功能
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 拦截 @DS 注解，将注解中的值数据源名称，存入 holder（ThreadLocal），由线程携带
        String dsKey = determineDatasourceKey(invocation);
        // 这个 ContextHolder 是一个基于 ThreadLocal 的栈结构，用于线程级别管理当前使用的数据源

        // 也就是说每个线程都有一个 ThreadLocal，里面用栈存了数据源名称，
        // 当方法深层次调用，数据源名称就往栈里 push，执行时从栈里取出当前对应的数据源名称，
        // 从而实现方法级别对应线程的动态数据源切换
        DynamicDataSourceContextHolder.push(dsKey);
        try {
            // 然后代理执行方法
            return invocation.proceed();
        } finally {
            // 然后出栈，以免影响后续操作
            DynamicDataSourceContextHolder.poll();
        }
    }

    /**
     * Determine the key of the datasource
     *
     * @param invocation MethodInvocation
     * @return dsKey
     */
    private String determineDatasourceKey(MethodInvocation invocation) {
        String key = dataSourceClassResolver.findKey(invocation.getMethod(), invocation.getThis(), DS.class);
        // 如果 @DS 注解的值以 # 开头，则动态解析值；否则直接返回
        return key.startsWith(DYNAMIC_PREFIX) ? dsProcessor.determineDatasource(invocation, key) : key;
    }
}