package io.mango.infra.web.starter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.DisconnectedClientHelper;

/**
 * Resolves transport lifecycle exceptions before the JSON exception advice.
 *
 * <p>Async stream timeouts and client disconnects are lifecycle events rather than
 * application failures. They must not be serialized through the normal {@code R}
 * response contract, especially after an SSE response has been committed.</p>
 */
public final class AsyncLifecycleExceptionResolver implements HandlerExceptionResolver, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    @Nullable
    public ModelAndView resolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable Object handler,
            Exception exception) {
        if (exception instanceof AsyncRequestTimeoutException) {
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
            return new ModelAndView();
        }
        if (exception instanceof AsyncRequestNotUsableException
                || DisconnectedClientHelper.isClientDisconnectedException(exception)) {
            return new ModelAndView();
        }
        return null;
    }
}
