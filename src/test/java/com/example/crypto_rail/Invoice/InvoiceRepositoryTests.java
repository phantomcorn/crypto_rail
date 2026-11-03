package com.example.crypto_rail.Invoice;

import com.example.crypto_rail.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
public class InvoiceRepositoryTests {

    @Autowired
    InvoiceRepository invoiceRepository;

}
