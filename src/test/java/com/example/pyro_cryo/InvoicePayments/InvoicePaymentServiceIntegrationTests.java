package com.example.pyro_cryo.InvoicePayments;

import com.example.pyro_cryo.dto.InvoicePaymentDto;
import com.example.pyro_cryo.entity.InvoicePayment;
import com.example.pyro_cryo.repository.InvoicePaymentRepository;
import com.example.pyro_cryo.service.InvoicePaymentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InvoicePaymentServiceIntegrationTests {

    @Autowired
    InvoicePaymentService invoicePaymentService;

    @Autowired
    InvoicePaymentRepository invoicePaymentRepository;

    public String random(Integer length) {
        byte[] array = new byte[length]; // length is bounded by 7
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }

    @Test
    public void addAddsNewInvoicePaymentToDb() {

        Assertions.assertEquals(0, invoicePaymentRepository.findAll().size());

        invoicePaymentService.add(new InvoicePaymentDto(random(66), BigInteger.ONE, random(66)));

        Assertions.assertEquals(1, invoicePaymentRepository.findAll().size());
    }

    @Test
    public void addDoesNotAddEntryWithDuplicateHashId() {
        String id = random(66);

        invoicePaymentService.add(new InvoicePaymentDto(id, BigInteger.ONE, random(66)));

        Assertions.assertEquals(1, invoicePaymentRepository.findAll().size());

        invoicePaymentService.add(new InvoicePaymentDto(id, BigInteger.TWO, random(66)));

        Assertions.assertEquals(1, invoicePaymentRepository.findAll().size());
    }

    @Test
    public void canFinalizeRetrievesAllBlockWithAtLeastTwelveBlockAheadOfIt() {


        int[] BLOCK_NUM = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18};
        for (int blockNum : BLOCK_NUM) {
            invoicePaymentService.add(new InvoicePaymentDto(random(66), BigInteger.valueOf(blockNum), random(66)));
        }

        int currentBlock = BLOCK_NUM[BLOCK_NUM.length - 1]; //18
        int[] blockWithAtLeastTwelveAhead = Arrays.stream(BLOCK_NUM).filter(i -> currentBlock - i >= 12).toArray();

        List<InvoicePaymentDto>  canFinalizePayment = invoicePaymentService.canFinalize(BigInteger.valueOf(currentBlock));

        Assertions.assertEquals(blockWithAtLeastTwelveAhead.length, canFinalizePayment.size());

        for (int i = 0; i < canFinalizePayment.size(); i++) {
            Assertions.assertEquals(BigInteger.valueOf(blockWithAtLeastTwelveAhead[i]), canFinalizePayment.get(i).blockNumber());
        }
    }

    @Test
    public void getPaymentBetweenRetrievesDataBetweenRange() {
        int[] ALL_BLOCK_NUM = {1,2,3,4,5,6,7,8,9,10};
        for (int blockNum : ALL_BLOCK_NUM) {
            invoicePaymentService.add(new InvoicePaymentDto(random(66), BigInteger.valueOf(blockNum), random(66)));
        }

        BigInteger from = BigInteger.valueOf(5);
        BigInteger to = BigInteger.valueOf(10);
        int size = to.subtract(from).intValue() + 1;
        List<InvoicePaymentDto>  payments = invoicePaymentService.getPaymentBetween(from, to);

        List<Integer> FILTERED_BLOCK_NUM = Arrays.stream(new int[] {5,6,7,8,9,10}).boxed().toList();
        Assertions.assertEquals(size, payments.size());
        List<Integer> paymentBlockNumbers = payments.stream().map(payment -> payment.blockNumber().intValue()).toList();
        Assertions.assertTrue(paymentBlockNumbers.containsAll(FILTERED_BLOCK_NUM));
    }

    @Test
    public void getPaymentBetweenRangeIsInclusive() {

        List<InvoicePaymentDto>  payments = invoicePaymentService.getPaymentBetween(BigInteger.ONE,BigInteger.ONE);
        Assertions.assertEquals(0, payments.size());

        int repeat = 5;
        for (int i = 0; i < repeat; i++) {
            invoicePaymentService.add(new InvoicePaymentDto(random(66), BigInteger.valueOf(1), random(66)));
        }

        payments = invoicePaymentService.getPaymentBetween(BigInteger.ONE,BigInteger.ONE);
        Assertions.assertEquals(repeat, payments.size());
        payments.forEach(payment -> {
            Assertions.assertEquals(1, payment.blockNumber().intValue());
        });
    }

    @Test
    public void updateUpdatesAllInvoicePayment() {

        List<String> ids = List.of(random(66), random(66), random(66));

        List<InvoicePayment> payments = List.of(
                new InvoicePayment(ids.get(0), BigInteger.valueOf(1), random(66)),
                new InvoicePayment(ids.get(1), BigInteger.valueOf(2), random(66)),
                new InvoicePayment(ids.get(2), BigInteger.valueOf(3), random(66))
        );
        invoicePaymentRepository.saveAll(payments);
        Assertions.assertEquals(payments.size(), invoicePaymentRepository.findAll().size());

        List<InvoicePaymentDto> paymentDtos = List.of(
                new InvoicePaymentDto(ids.get(0), BigInteger.valueOf(3), random(66)),
                new InvoicePaymentDto(ids.get(1), BigInteger.valueOf(4), random(66)),
                new InvoicePaymentDto(ids.get(2), BigInteger.valueOf(5), random(66))
        );

        invoicePaymentService.update(paymentDtos);
        Assertions.assertEquals(paymentDtos.size(), invoicePaymentRepository.findAll().size());
        Assertions.assertTrue(invoicePaymentRepository.findAll().stream().map(InvoicePayment::getInvoiceHashId).toList().containsAll(ids));
    }

    @Test
    public void insertCreatesNewInvoicePayment() {

        List<String> ids = List.of(random(66), random(66), random(66));

        List<InvoicePaymentDto> paymentDtos = List.of(
                new InvoicePaymentDto(ids.get(0), BigInteger.valueOf(1), random(66)),
                new InvoicePaymentDto(ids.get(1), BigInteger.valueOf(2), random(66)),
                new InvoicePaymentDto(ids.get(2), BigInteger.valueOf(3), random(66))
        );

        Assertions.assertEquals(0, invoicePaymentRepository.findAll().size());
        invoicePaymentService.insert(paymentDtos);
        Assertions.assertEquals(paymentDtos.size(), invoicePaymentRepository.findAll().size());
        Assertions.assertTrue(invoicePaymentRepository.findAll().stream().map(InvoicePayment::getInvoiceHashId).toList().containsAll(ids));
    }

    @Test
    public void insertDuplicateKeyDoesNotCreateDuplicateEntry() {
        String id = random(66);

        List<InvoicePaymentDto> paymentDtos = List.of(
                new InvoicePaymentDto(id, BigInteger.valueOf(1), random(66)),
                new InvoicePaymentDto(id, BigInteger.valueOf(2), random(66))
        );

        invoicePaymentService.insert(paymentDtos);
        Assertions.assertEquals(1, invoicePaymentRepository.findAll().size());
        Assertions.assertEquals(id, invoicePaymentRepository.findAll().getFirst().getInvoiceHashId());
    }

    @Test
    public void insertShouldNotUpdateEntryWithExistingKey() {
        String id = random(66);

        List<InvoicePaymentDto> paymentDtos = List.of(
                new InvoicePaymentDto(id, BigInteger.valueOf(1), random(66))
        );

        invoicePaymentService.insert(paymentDtos);
        Assertions.assertEquals(1, invoicePaymentRepository.findAll().size());
        Assertions.assertEquals(id, invoicePaymentRepository.findAll().getFirst().getInvoiceHashId());
        Assertions.assertEquals(paymentDtos.getFirst().blockHash(), invoicePaymentRepository.findAll().getFirst().getBlockHash());

        List<InvoicePaymentDto> updatedPaymentDtos = List.of(
                new InvoicePaymentDto(id, BigInteger.valueOf(2), random(66))
        );
        invoicePaymentService.insert(updatedPaymentDtos);

        Assertions.assertEquals(1, invoicePaymentRepository.findAll().size());
        Assertions.assertEquals(id, invoicePaymentRepository.findAll().getFirst().getInvoiceHashId());
        Assertions.assertEquals(paymentDtos.getFirst().blockHash(), invoicePaymentRepository.findAll().getFirst().getBlockHash());
        Assertions.assertEquals(paymentDtos.getFirst().blockNumber(), invoicePaymentRepository.findAll().getFirst().getBlockNumber());
    }

    @Test
    public void updateShouldNotCreateNewEntry() {
        String id = random(66);

        Assertions.assertEquals(0, invoicePaymentRepository.findAll().size());

        List<InvoicePaymentDto> paymentDtos = List.of(
                new InvoicePaymentDto(id, BigInteger.valueOf(1), random(66))
        );

        invoicePaymentService.update(paymentDtos);
        Assertions.assertEquals(0, invoicePaymentRepository.findAll().size());
    }

    @Test
    public void removeAllByIdRemovesEntryFromDb() {
        List<String> ids = List.of(random(66), random(66), random(66));

        List<InvoicePayment> payments = List.of(
                new InvoicePayment(ids.get(0), BigInteger.valueOf(1), random(66)),
                new InvoicePayment(ids.get(1), BigInteger.valueOf(2), random(66)),
                new InvoicePayment(ids.get(2), BigInteger.valueOf(3), random(66))
        );
        invoicePaymentRepository.saveAll(payments);

        Assertions.assertEquals(3, invoicePaymentRepository.findAll().size());
        invoicePaymentService.removeAllById(List.of(ids.getFirst()));
        Assertions.assertEquals(2, invoicePaymentRepository.findAll().size());
    }

    @Test
    public void removeAllByIdDoesNothingOnNonExistingKey() {
        List<String> ids = List.of(random(66), random(66), random(66));

        List<InvoicePayment> payments = List.of(
                new InvoicePayment(ids.get(0), BigInteger.valueOf(1), random(66)),
                new InvoicePayment(ids.get(1), BigInteger.valueOf(2), random(66)),
                new InvoicePayment(ids.get(2), BigInteger.valueOf(3), random(66))
        );

        invoicePaymentRepository.saveAll(payments);

        Assertions.assertEquals(3, invoicePaymentRepository.findAll().size());
        invoicePaymentService.removeAllById(List.of(random(66)));
        Assertions.assertEquals(3, invoicePaymentRepository.findAll().size());
    }
}
