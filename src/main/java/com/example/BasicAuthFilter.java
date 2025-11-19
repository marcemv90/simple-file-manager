package com.example;

import java.io.IOException;
import java.util.Base64;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BasicAuthFilter implements Filter {
    
    // Example credentials
    private static final String USERNAME = "testadm";
    private static final String PASSWORD = "ePMnSaD83TB7USxFz7EL";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Basic ")) {
            // Extract the Base64-encoded username:password
            String base64Credentials = authHeader.substring(6);
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] values = credentials.split(":", 2);

            if (values.length == 2 && USERNAME.equals(values[0]) && PASSWORD.equals(values[1])) {
                // Credentials are correct, proceed with the request
                chain.doFilter(request, response);
                return;
            }
        }

        // If authentication fails, prompt for login
        httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"Restricted Area\"");
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }
}
