package cn.keking.web.filter;

import cn.keking.config.OfficeXlsxWebButtonsConfig;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/** Exposes the XLSX Web button switch to FreeMarker. */
public class OfficeXlsxWebButtonsAttributeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setAttribute("officeXlsxWebButtonsEnabled", OfficeXlsxWebButtonsConfig.isEnabled());
        chain.doFilter(request, response);
    }
}
