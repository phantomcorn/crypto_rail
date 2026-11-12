package com.example.crypto_rail.config;

import com.example.crypto_rail.config.filter.ApiKeyFilter;
import com.example.crypto_rail.config.filter.JwtFilter;
import com.example.crypto_rail.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Profile({"prod", "local"})
public class SecurityConfig {

    @Autowired
    MerchantService merchantService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtFilter jwtFilter;

    @Autowired
    ApiKeyFilter apiKeyFilter;

    @Bean
    SecurityFilterChain newFilterChain(HttpSecurity http) {
        return http
                .csrf((csrf) -> csrf.disable())
                .authorizeHttpRequests((auth) ->
                        auth
                                .requestMatchers("/api/key/**").hasAuthority(JwtFilter.GRANTED_ROLE.getAuthority())
                                .requestMatchers("/api/checkout/**").hasAnyAuthority(JwtFilter.GRANTED_ROLE.getAuthority(), ApiKeyFilter.GRANTED_ROLE.getAuthority())
                                .requestMatchers("/api/merchant/**").permitAll()
                ).sessionManagement(
                        session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> {
                    exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
                })
                .authenticationProvider(daoAuthProvider(encoder))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyFilter, JwtFilter.class)
                .build();
    }

    @Bean
    AuthenticationProvider daoAuthProvider(PasswordEncoder encoder) { //Tells spring we authenticate using our database
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(merchantService);
        provider.setPasswordEncoder(encoder);

        return provider;
    }

}
