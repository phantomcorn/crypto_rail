package com.example.crypto_rail.InvoicePayments;

import com.example.crypto_rail.repository.InvoicePaymentRepository;
import com.example.crypto_rail.service.InvoicePaymentService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ExtendWith(MockitoExtension.class)
public class InvoicePaymentServiceUnitTests {

    @InjectMocks
    InvoicePaymentService invoicePaymentService;

    @Mock
    InvoicePaymentRepository invoicePaymentRepository;

}
