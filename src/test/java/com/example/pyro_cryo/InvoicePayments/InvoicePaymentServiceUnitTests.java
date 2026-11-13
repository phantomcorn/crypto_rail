package com.example.pyro_cryo.InvoicePayments;

import com.example.pyro_cryo.repository.InvoicePaymentRepository;
import com.example.pyro_cryo.service.InvoicePaymentService;
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
