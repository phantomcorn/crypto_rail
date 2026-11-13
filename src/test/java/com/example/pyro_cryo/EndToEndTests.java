package com.example.pyro_cryo;

import com.example.pyro_cryo.dto.request.MerchantCreateRequest;
import com.example.pyro_cryo.dto.request.PayRequest;
import com.example.pyro_cryo.repository.MerchantRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class EndToEndTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper json;

    @Autowired
    MerchantRepository merchantRepository;

    @Test
    public void generateInvoiceReturnsCheckoutLink() throws Exception {
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
                    status().isOk()
                );
    }

    @Test
    public void createMerchantReturnsSuccess() throws Exception {

        MerchantCreateRequest request = new MerchantCreateRequest(
                "user1",
                "11111",
                "Google",
                "0x207F6986e453D5AEcaDa283E252fb78946E38A37",
                "www.google.com"
        );
        System.out.println(json.writeValueAsString(request));
        mockMvc.perform(
                    post("/api/merchant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(request))
                ).andExpect(
                        status().isOk()
                );
    }

    @Test
    public void duplicateMerchantReturns200() throws Exception {

        String user = "user1";

        Assertions.assertEquals(1, merchantRepository.findAll().size());
        Assertions.assertTrue(merchantRepository.existsById(user));

        MerchantCreateRequest request = new MerchantCreateRequest(
                "user1",
                "11111",
                "Google",
                "0x1111111111111111111111111111111111111111",
                "www.google.com"
        );

        mockMvc.perform(
                post("/api/merchant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request))
        ).andExpect(
                status().isOk()
        );

        Assertions.assertEquals(1, merchantRepository.findAll().size());

    }

}
