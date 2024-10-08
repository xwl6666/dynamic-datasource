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
package com.baomidou.dynamic.datasource.annotation;

import com.baomidou.dynamic.datasource.tx.DsPropagation;

import java.lang.annotation.*;

/**
 * 多数据源事务
 * Spring 的 @Transactional() 不支持多数据源
 * multi data source transaction
 *
 * @author funkye
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DSTransactional {

    /**
     * 回滚异常
     *
     * @return Class[]
     */
    Class<? extends Throwable>[] rollbackFor() default {Exception.class};

    /**
     * 不回滚异常
     *
     * @return Class[]
     */
    Class<? extends Throwable>[] noRollbackFor() default {};

    /**
     * 事务传播行为
     *
     * @return DsPropagation
     */
    DsPropagation propagation() default DsPropagation.REQUIRED;
}