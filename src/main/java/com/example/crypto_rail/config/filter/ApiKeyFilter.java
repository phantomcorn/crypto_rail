package com.example.crypto_rail.config.filter;

import com.example.crypto_rail.dto.ApiKeyDto;
import com.example.crypto_rail.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Autowired
    ApiKeyService apiKeyService;

    private final String role = "API_USER";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String value = request.getHeader("X-API-Key");
        if (value == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String[] items = value.split(":");

        if (items.length != 2) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = items[0];
        String key = items[1];

        ApiKeyDto keyDto = apiKeyService.verify(username, key);

        if (keyDto != null) {
            setCredentials(request, keyDto.merchant());
        }

        filterChain.doFilter(request,response);

    }

    private void setCredentials(HttpServletRequest request, UserDetails userDetails) {

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

    }
}
