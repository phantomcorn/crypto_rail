package com.example.crypto_rail.config.filter;

import com.example.crypto_rail.service.JwtService;
import com.example.crypto_rail.service.MerchantService;
import com.example.crypto_rail.utils.CookieUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter{

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ApplicationContext context;

    public static final GrantedAuthority GRANTED_ROLE = new SimpleGrantedAuthority("TOKEN_USER");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {


        String accessToken = CookieUtils.getCookieValue(request, "access_token");
        String user = null;

        if (!accessToken.isEmpty() && !jwtService.isTokenExpired(accessToken)) {
            user = jwtService.extractUsername(accessToken);
        }

        //Token exists but has not been authenticated => We want to authenticate
        if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            //Get user details from database
            UserDetails userDetails = context.getBean(MerchantService.class).loadUserByUsername(user);
        /*
            Since we are injecting this filter before a UserPasswordAuthenticationFilter
            We are essentially creating a new UserPasswordAuthenticationToken everytime for validation from the
            UserPasswordAuthenticationFilter
        */
            if (jwtService.validateToken(accessToken, userDetails)) {
                //Create new auth token
                System.out.println("Jwt Filter: Success");
                setCredentials(request, userDetails);
                filterChain.doFilter(request, response);
                return;
            }
        }

        //Check for refresh token
        user = null; //reset user, refresh path should only check on refresh token
        String refreshToken = CookieUtils.getCookieValue(request, "refresh_token");
        if (!refreshToken.isEmpty() && !jwtService.isTokenExpired(refreshToken)) {
            user = jwtService.extractUsername(refreshToken);
        }

        if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = context.getBean(MerchantService.class).loadUserByUsername(user);

            //Gen new access token => this will be used the next call
            String newAccessToken = jwtService.createToken(userDetails.getUsername(), JwtService.ACCESS_TOKEN_TTL);
            Cookie accessTokenCookie = new Cookie("access_token", newAccessToken);
            accessTokenCookie.setMaxAge((int) (JwtService.ACCESS_TOKEN_TTL / 1000));
            accessTokenCookie.setSecure(false); // use HTTPS in production!
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setPath("/");
            response.addCookie(accessTokenCookie);

            //Create new auth token (The next time this filter is called, it will use the new access token)
            System.out.println("Jwt Filter: Success");
            setCredentials(request, userDetails);

        }
        //continue to next filter
        filterChain.doFilter(request, response);
    }

    private void setCredentials(HttpServletRequest request, UserDetails userDetails) {

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                List.of(GRANTED_ROLE)
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

    }
}