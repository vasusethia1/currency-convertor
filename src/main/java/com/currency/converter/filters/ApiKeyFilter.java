package com.currency.converter.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

  @Value("${app.security.api-key}")
  private String configuredApiKey;

  private static final List<String> WHITELISTED_PATHS = Arrays.asList(
    "/v3/api-docs",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
  );

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String incomingApiKey = request.getHeader("X-API-KEY");

    String requestPath = request.getRequestURI();

    boolean isWhitelisted = WHITELISTED_PATHS.stream().anyMatch(path ->
      path.endsWith("/**") ? requestPath.startsWith(path.replace("/**", "")) : path.equals(requestPath)
    );

    if (isWhitelisted) {
      filterChain.doFilter(request, response);
      return;
    }
    if (configuredApiKey.equals(incomingApiKey)) {
      filterChain.doFilter(request, response);
    } else {
      logger.warn("Unauthorized request with invalid or missing API key");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
    }
  }
}
