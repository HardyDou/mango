package io.mango.infra.bootstrap.starter;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.core.Ordered;

final class RuntimeReceiptServletContextInitializer implements ServletContextInitializer, Ordered {

    private final ObjectProvider<RuntimeLeaseManager> leaseManagerProvider;

    RuntimeReceiptServletContextInitializer(ObjectProvider<RuntimeLeaseManager> leaseManagerProvider) {
        this.leaseManagerProvider = leaseManagerProvider;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        leaseManagerProvider.getObject().prepareRuntimeLease();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
