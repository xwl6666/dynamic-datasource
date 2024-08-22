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
package com.baomidou.dynamic.datasource.tx;

import com.baomidou.dynamic.datasource.toolkit.DsStrUtils;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 本地事务工具类
 *
 * @author TaoYu
 * @since 3.5.0
 */
@Slf4j
public final class LocalTxUtil {

    /**
     * SecureRandom instance used to generate UUIDs.
     */
    private static final ThreadLocal<SecureRandom> SECURE_RANDOM_HOLDER = ThreadLocal.withInitial(SecureRandom::new);

    /**
     * 随机生成UUID
     *
     * @return UUID
     */
    public static UUID randomUUID() {
        SecureRandom ng = SECURE_RANDOM_HOLDER.get();
        byte[] randomBytes = new byte[16];
        ng.nextBytes(randomBytes);
        // clear version
        randomBytes[6] &= 0x0f;
        // set to version 4
        randomBytes[6] |= 0x40;
        // clear variant
        randomBytes[8] &= 0x3f;
        // set to IETF variant
        randomBytes[8] |= 0x80;
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (randomBytes[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (randomBytes[i] & 0xff);
        }
        return new UUID(msb, lsb);
    }

    /**
     * 手动开启事务
     *
     * @return 事务ID
     */
    public static String startTransaction() {
        String xid = TransactionContext.getXID();
        if (!DsStrUtils.isEmpty(xid)) {
            log.debug("dynamic-datasource exist local tx [{}]", xid);
        } else {
            xid = randomUUID().toString();
            // 开启事务时，将事务 id xid 存入 ThreadLocal 的 holder，从而实现开启本地事务
            // 拦截器的 invoke 方法中会校验 holder 中 xid 是否为空。如果已存在事务 id，就直接执行目标方法
            TransactionContext.bind(xid);
            log.debug("dynamic-datasource start local tx [{}]", xid);
        }
        return xid;
    }

    /**
     * 手动提交事务
     *
     * @param xid 事务ID
     */
    public static void commit(String xid) throws Exception {
        // 提交和回滚由 ConnectionFactory 实现，
        // ConnectionFactory 中 CONNECTION_HOLDER 存放数据源名称和 ConnectionProxy 的对应关系
        // 而 ConnectionProxy 是真正实现提交和回滚的类
        boolean hasSavepoint = ConnectionFactory.hasSavepoint(xid);
        try {
            ConnectionFactory.notify(xid, true);
        } finally {
            if (!hasSavepoint) {
                log.debug("dynamic-datasource commit local tx [{}]", TransactionContext.getXID());
                TransactionContext.remove();
            }
        }
    }

    /**
     * 手动回滚事务
     *
     * @param xid 事务ID
     */
    public static void rollback(String xid) throws Exception {
        boolean hasSavepoint = ConnectionFactory.hasSavepoint(xid);
        try {
            ConnectionFactory.notify(xid, false);
        } finally {
            if (!hasSavepoint) {
                log.debug("dynamic-datasource rollback local tx [{}]", TransactionContext.getXID());
                TransactionContext.remove();
            }
        }
    }
}