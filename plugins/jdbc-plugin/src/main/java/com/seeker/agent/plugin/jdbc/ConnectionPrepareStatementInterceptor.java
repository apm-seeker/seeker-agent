package com.seeker.agent.plugin.jdbc;

import com.seeker.agent.core.context.SqlMetadataAccessor;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;

/**
 * Connection.prepareStatement 호출을 가로채서 SQL 문장을 PreparedStatement 객체에 저장합니다.
 */
public class ConnectionPrepareStatementInterceptor implements AroundInterceptor {

    @Override
    public void before(Object target, String className, String methodName, Object[] args) {
        // SQL 문장은 보통 첫 번째 인자로 전달됩니다.
    }

    @Override
    public void after(Object target, String className, String methodName, Object[] args, Object result,
            Throwable throwable) {
        if (throwable == null && result instanceof SqlMetadataAccessor && args != null && args.length > 0
                && args[0] instanceof String) {
            String sql = (String) args[0];
            ((SqlMetadataAccessor) result)._$seeker$setSql(sql);
        }
    }
}
