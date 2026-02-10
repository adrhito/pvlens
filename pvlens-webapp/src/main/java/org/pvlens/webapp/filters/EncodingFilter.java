package org.pvlens.webapp.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

/**
 * Filter to ensure UTF-8 encoding for all requests and responses.
 */
public class EncodingFilter implements Filter {

    private String encoding = "UTF-8";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String configuredEncoding = filterConfig.getInitParameter("encoding");
        if (configuredEncoding != null && !configuredEncoding.isEmpty()) {
            this.encoding = configuredEncoding;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Set request encoding if not already set
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(encoding);
        }

        // Set response encoding
        response.setCharacterEncoding(encoding);

        // Continue with the filter chain
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Nothing to clean up
    }
}
