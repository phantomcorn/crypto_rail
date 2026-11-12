package com.example.crypto_rail;

import com.example.crypto_rail.dto.request.ApiKeyCreateRequest;
import com.example.crypto_rail.dto.request.MerchantCreateRequest;
import com.example.crypto_rail.dto.request.MerchantLoginRequest;
import com.example.crypto_rail.dto.request.PayRequest;
import com.example.crypto_rail.entity.Merchant;
import com.example.crypto_rail.repository.MerchantRepository;
import com.example.crypto_rail.service.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.web3j.utils.Strings.repeat;

@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthIntegrationTests {

    String username = "user1";
    String password = "password";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper json;

    @Autowired
    MerchantRepository merchantRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtService jwtService;

    @Autowired
    Clock clock;

    @TestConfiguration
    static class AuthConfig { //override clock

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"), ZoneId.systemDefault());
        }
    }

    public String login() throws Exception {
        MerchantLoginRequest request = new MerchantLoginRequest(
                username,
                password
        );

        MvcResult response = mockMvc.perform(
                post("/api/merchant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isOk()
        ).andReturn();



        Cookie[] cookies = response.getResponse().getCookies();
        Assertions.assertEquals("access_token", cookies[0].getName());
        return cookies[0].getValue();
    }

    @BeforeEach
    public void register() throws Exception {

        MerchantCreateRequest request = new MerchantCreateRequest(
                username,
                password,
                "Merchant A",
                "0x" + repeat('2', 40),
                "https://www.google.com"
        );

        mockMvc.perform(
                post("/api/merchant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isOk()
        );

    }

    @AfterEach
    public void resetTime() {
        ReflectionTestUtils.setField(jwtService, "clock", clock);
    }

    @Test
    public void registerStoreHashedPasswordNotRawPassword() {
        Assertions.assertTrue(merchantRepository.existsById(username));
        Merchant found = merchantRepository.findById(username).get();

        Assertions.assertTrue(encoder.matches(password, found.getPassword()));
        Assertions.assertNotEquals(found.getPassword(), password);

    }

    @Test
    public void loginUsingValidCredentialsReturnsSuccessAndAccessTokenAsCookie() throws Exception {
        MerchantLoginRequest request = new MerchantLoginRequest(
                username,
                password
        );

        MvcResult response = mockMvc.perform(
                post("/api/merchant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isOk()
        ).andReturn();

        String result = response.getResponse().getContentAsString();
        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(2,response.getResponse().getCookies().length);
    }

    @Test
    public void invalidLoginReturnsNoCookie() throws Exception {
        MerchantLoginRequest request = new MerchantLoginRequest(
                username,
                "WRONG_PASSWORD"
        );

        MvcResult response = mockMvc.perform(
                post("/api/merchant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isUnauthorized()
        ).andReturn();

        String result = response.getResponse().getContentAsString();
        Assertions.assertEquals("Incorrect credentials", result);
        Assertions.assertEquals(0, response.getResponse().getCookies().length);
    }

    @Test
    public void invoiceEndpointInvalidWithoutAuth() throws Exception {

        PayRequest request = new PayRequest(
                BigInteger.valueOf(1000000),
                "0x207F6986e453D5AEcaDa283E252fb78946E38A37",
                "0x1c7d4b196cb0c7b01d743fbc6116a902379c7238"
        );

        mockMvc.perform(
                post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isUnauthorized()
        );
    }


    @Test
    public void apiKeyEndpointInvalidWithoutAuth() throws Exception {

        ApiKeyCreateRequest request = new ApiKeyCreateRequest("key1");

        mockMvc.perform(
                post("/api/key/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isUnauthorized()
        );
    }


    @Test
    public void canAuthenticateUsingToken() throws Exception {
        String accessToken = login();

        Cookie cookie = new Cookie("access_token", accessToken);
        cookie.setHttpOnly(true);

        PayRequest request = new PayRequest(
                BigInteger.valueOf(1000000),
                "0x207F6986e453D5AEcaDa283E252fb78946E38A37",
                "0x1c7d4b196cb0c7b01d743fbc6116a902379c7238"
        );

        mockMvc.perform(
                post("/api/checkout")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isOk()
        );
    }

    @Test
    public void accessTokenExpiresAfter2Minutes() throws Exception {
        String accessToken = login();
        Assertions.assertFalse(jwtService.isTokenExpired(accessToken));

        //Does not expire at exactly 2 minutes
        ReflectionTestUtils.setField(jwtService, "clock", Clock.offset(clock, Duration.ofMillis(JwtService.ACCESS_TOKEN_TTL)));
        Assertions.assertFalse(jwtService.isTokenExpired(accessToken));

        //Expires right after 2 minutes
        ReflectionTestUtils.setField(jwtService, "clock", Clock.offset(clock, Duration.ofMillis(JwtService.ACCESS_TOKEN_TTL + 1)));
        Assertions.assertTrue(jwtService.isTokenExpired(accessToken));
    }

    @Test
    public void refreshTokenExpiresAfter6Hours() throws Exception {
        String refreshToken = jwtService.createToken(username, JwtService.REFRESH_TOKEN_TTL);
        Assertions.assertFalse(jwtService.isTokenExpired(refreshToken));
        //Does not expire at exactly 6 hours
        ReflectionTestUtils.setField(jwtService, "clock", Clock.offset(clock, Duration.ofMillis(JwtService.REFRESH_TOKEN_TTL)));
        Assertions.assertFalse(jwtService.isTokenExpired(refreshToken));

        //Expires right after 6 hours
        ReflectionTestUtils.setField(jwtService, "clock", Clock.offset(clock, Duration.ofMillis(JwtService.REFRESH_TOKEN_TTL + 1)));
        Assertions.assertTrue(jwtService.isTokenExpired(refreshToken));
    }

    @Test
    public void expiredAccessTokenWillInvalidateAuth() throws Exception {
        String accessToken = login();
        Cookie cookie = new Cookie("access_token", accessToken);
        cookie.setHttpOnly(true);

        ReflectionTestUtils.setField(jwtService, "clock", Clock.offset(clock, Duration.ofSeconds(121)));
        PayRequest request = new PayRequest(
            BigInteger.valueOf(1000000),
            "0x207F6986e453D5AEcaDa283E252fb78946E38A37",
            "0x1c7d4b196cb0c7b01d743fbc6116a902379c7238"
        );

        mockMvc.perform(
            post("/api/checkout")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))
        ).andExpect(
            status().isUnauthorized()
        );
    }

    @Test
    public void validRefreshTokenWillRegenerateAccessTokenAndAllowAccess() throws Exception {
        String accessToken = login();
        Cookie cookie = new Cookie("access_token", accessToken);
        cookie.setHttpOnly(true);
        ReflectionTestUtils.setField(jwtService, "clock", Clock.offset(clock, Duration.ofMillis(JwtService.ACCESS_TOKEN_TTL + 1)));
        Assertions.assertTrue(jwtService.isTokenExpired(accessToken));

        String refreshToken = jwtService.createToken(username, JwtService.REFRESH_TOKEN_TTL);
        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);

        PayRequest request = new PayRequest(
                BigInteger.valueOf(1000000),
                "0x207F6986e453D5AEcaDa283E252fb78946E38A37",
                "0x1c7d4b196cb0c7b01d743fbc6116a902379c7238"
        );

        Cookie[] cookiesResponseWithoutRefreshToken = mockMvc.perform(
                post("/api/checkout")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isUnauthorized()
        ).andReturn().getResponse().getCookies();

        Cookie[] cookiesResponseWithRefreshToken = mockMvc.perform(
                post("/api/checkout")
                        .cookie(cookie, refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isOk()
        ).andReturn().getResponse().getCookies();

        Assertions.assertEquals(0, cookiesResponseWithoutRefreshToken.length);
        Assertions.assertEquals(1, cookiesResponseWithRefreshToken.length);

        String key = cookiesResponseWithRefreshToken[0].getName();
        String newAccessToken = cookiesResponseWithRefreshToken[0].getValue();
        Assertions.assertEquals("access_token", key);
        Assertions.assertNotEquals(newAccessToken, accessToken);
    }

    @Test
    public void expiredRefreshTokenAndAccessTokenReturnsInvalidAuth() throws Exception {
        String accessToken = login();
        Cookie cookie = new Cookie("access_token", accessToken);
        cookie.setHttpOnly(true);
        ReflectionTestUtils.setField(jwtService, "clock", Clock.offset(clock, Duration.ofMillis(JwtService.ACCESS_TOKEN_TTL + 1)));
        Assertions.assertTrue(jwtService.isTokenExpired(accessToken));

        //reset clock
        ReflectionTestUtils.setField(jwtService, "clock", clock);

        String refreshToken = jwtService.createToken(username, JwtService.REFRESH_TOKEN_TTL);
        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        ReflectionTestUtils.setField(jwtService, "clock", Clock.offset(clock, Duration.ofMillis(JwtService.REFRESH_TOKEN_TTL + 1)));
        Assertions.assertTrue(jwtService.isTokenExpired(refreshToken));

        PayRequest request = new PayRequest(
                BigInteger.valueOf(1000000),
                "0x207F6986e453D5AEcaDa283E252fb78946E38A37",
                "0x1c7d4b196cb0c7b01d743fbc6116a902379c7238"
        );

        mockMvc.perform(
                post("/api/checkout")
                        .cookie(cookie, refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isUnauthorized()
        );

    }

}
