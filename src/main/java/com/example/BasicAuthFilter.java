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
    
    private static final String USERNAME = initUsername();
    private static final String PASSWORD = initPassword();
    private static final boolean AUTH_ENABLED = isAuthEnabled();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
    }

    private static String initUsername() {
        String propUser = System.getProperty("sfm_basic_auth_user");
        if (propUser != null && !propUser.isEmpty()) {
            return propUser;
        }

        String envUser = System.getenv("BASIC_AUTH_USER");
        return (envUser == null || envUser.isEmpty()) ? null : envUser;
    }

    private static String initPassword() {
        String propPass = System.getProperty("sfm_basic_auth_password");
        if (propPass != null && !propPass.isEmpty()) {
            return propPass;
        }

        String envPass = System.getenv("BASIC_AUTH_PASSWORD");
        return (envPass == null || envPass.isEmpty()) ? null : envPass;
    }

    private static boolean isAuthEnabled() {
        return USERNAME != null && PASSWORD != null;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!AUTH_ENABLED) {
            // Basic Auth not configured; allow all requests through
            chain.doFilter(request, response);
            return;
        }

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
