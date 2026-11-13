package com.example.pyro_cryo.controller;

import com.example.pyro_cryo.dto.InvoiceDto;
import com.example.pyro_cryo.dto.PayRequestDto;
import com.example.pyro_cryo.dto.request.PayRequest;
import com.example.pyro_cryo.dto.response.PayResponse;
import com.example.pyro_cryo.service.InvoiceService;
import com.example.pyro_cryo.utils.Web3jClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.Optional;

@Controller
public class InvoiceController {

    public record MessageView(String invoiceId, String hashId, String tokenAddress, String amount, String merchantAddress){};
    public record DomainView(String name, String version, String chainId, String verifyingContract){};

    @Autowired
    InvoiceService invoiceService;

    @PostMapping("/api/checkout")
    public ResponseEntity<PayResponse> pay(@RequestBody PayRequest request) throws IOException {

        PayRequestDto dto = PayRequestDto.builder()
                .amount(request.getAmount())
                .merchantAddress(request.getMerchantAddress())
                .tokenAddress(request.getTokenAddress())
                .build();


        PayResponse resp = invoiceService.createInvoice(dto);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/checkout/{invoiceId}")
    public String renderCheckoutPage(Model model, @PathVariable String invoiceId) {

        Optional<InvoiceDto> invoiceObj = invoiceService.getInvoiceDetails(invoiceId);
        if (invoiceObj.isPresent()) {
            InvoiceDto invoiceDto = invoiceObj.get();

            model.addAttribute("domain", new DomainView(
                    invoiceDto.name(),
                    invoiceDto.version(),
                    invoiceDto.chainId().toString(),
                    Web3jClient.CONTRACT_ADDRESS
            ));

            model.addAttribute("message", new MessageView(
                    invoiceDto.id(),
                    invoiceDto.hashId(),
                    invoiceDto.tokenAddress(),
                    invoiceDto.amount().toString(),
                    invoiceDto.merchantAddress()
            ));

            model.addAttribute("voucher", invoiceDto.signature());
            model.addAttribute("deadline", invoiceDto.deadline());
        }

        return "index.html"; //src/main/resources/templates/index.html (Thymeleaf default path)
    }
}
