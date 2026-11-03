package com.example.crypto_rail.invoice.integration;

import com.example.crypto_rail.dto.InvoicePaymentDto;
import com.example.crypto_rail.invoice.unit.NotificationWithLogs;
import com.example.crypto_rail.service.InvoicePaymentService;
import com.example.crypto_rail.service.InvoiceService;
import com.example.crypto_rail.utils.Web3jClient;
import com.example.crypto_rail.utils.Web3jClientAdapter;
import io.reactivex.processors.PublishProcessor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.websocket.events.NewHead;
import org.web3j.protocol.websocket.events.NewHeadsNotification;
import org.web3j.protocol.websocket.events.NotificationParams;
import org.web3j.utils.Bloom;
import org.web3j.utils.Numeric;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Transactional
@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SpringBootTest @ActiveProfiles("test")
public class Web3jClientIntegrationTests {

    @Mock
    Web3jClientAdapter web3jClientAdapter;

    @Mock
    Web3j web3j;

    Web3jClient web3jClient;
    PublishProcessor<NewHeadsNotification> newHeadPublisher;
    List<NewHeadsNotification> normalChain;
    List<NewHeadsNotification> reorgChain;

    @MockitoSpyBean //MockitoSpyBean wraps a bean so that you can treat them as mock
    InvoicePaymentService invoicePaymentService;
    @MockitoSpyBean
    InvoiceService invoiceService;

    public void mockEthGetLogs(NewHead block, List<EthLog.LogResult> logs) throws IOException {
        Request<?, EthLog> mockReq = mock(Request.class);
        mockReq.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        EthLog mockEthLog = mock(EthLog.class);

        doReturn(mockReq).when(web3j).ethGetLogs(
                argThat(arg -> {
                    boolean equalHash = arg.getBlockHash() != null && arg.getBlockHash().equals(block.getHash());
                    boolean notNull = arg.getFromBlock() != null && arg.getToBlock() != null;
                    boolean isFromName = arg.getFromBlock() instanceof DefaultBlockParameterName;
                    boolean isToName = arg.getToBlock() instanceof DefaultBlockParameterName;
                    boolean inBetween = notNull &&
                            (
                                    (!isFromName && Numeric.toBigInt(block.getNumber()).compareTo(Numeric.toBigInt(arg.getFromBlock().getValue())) >= 0) ||
                                            (!isToName && Numeric.toBigInt(block.getNumber()).compareTo(Numeric.toBigInt(arg.getToBlock().getValue())) <= 0)
                            );

                    return equalHash || inBetween;
                })
        ); //using the when convention causes java to complain using wildcards
        when(mockReq.send()).thenReturn(mockEthLog);
        when(mockEthLog.getLogs()).thenReturn(logs);
    }

    public EthLog.LogObject newLogObj(boolean withTopic, String invoiceId) {
        EthLog.LogObject logResult = new EthLog.LogObject();
        if (withTopic) {
            logResult.setTopics(List.of(Web3jClient.topic0, invoiceId));
        } else {
            logResult.setTopics(Collections.emptyList());
        }
        return logResult;
    }

    public EthLog.LogObject newLogObj(boolean withTopic) {
        EthLog.LogObject logResult = new EthLog.LogObject();
        if (withTopic) {
            logResult.setTopics(List.of(Web3jClient.topic0, Hash.sha3String(UUID.randomUUID().toString())));
        } else {
            logResult.setTopics(Collections.emptyList());
        }
        return logResult;
    }

    public EthBlock newBlock(String number, String hash, String parentHash) {
        EthBlock.Block block = new EthBlock.Block();
        block.setNumber(number);
        block.setHash(hash);
        block.setParentHash(parentHash);
        EthBlock ethBlock = new EthBlock();
        ethBlock.setResult(block);
        ethBlock.setId(
                UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE
        );
        return ethBlock;
    }

    public void mockForkChain() throws IOException {
        for (NewHeadsNotification newHeadsNotification : reorgChain) {
            NewHead head = newHeadsNotification.getParams().getResult();
            EthBlock block = newBlock(head.getNumber(), head.getHash(), head.getParentHash());
            if (Bloom.test(head.getLogsBloom(), Web3jClient.CRYPTO_RAIL_ADDRESS, Web3jClient.topic0)) {
                mockEthGetLogs(head, List.of(newLogObj(true)));
            } else {
                mockEthGetLogs(head, List.of(newLogObj(false)));
            }
            mockBlockByNumber(block);
            mockBlockByHash(block);
        }
    }

    public void mockNormalChain() throws IOException {
        for (NewHeadsNotification newHeadsNotification : normalChain) {
            NewHead head = newHeadsNotification.getParams().getResult();
            EthBlock block = newBlock(head.getNumber(), head.getHash(), head.getParentHash());
            if (Bloom.test(head.getLogsBloom(), Web3jClient.CRYPTO_RAIL_ADDRESS, Web3jClient.topic0)) {
                mockEthGetLogs(head, List.of(newLogObj(true)));
            } else {
                mockEthGetLogs(head, List.of(newLogObj(false)));
            }
            mockBlockByNumber(block);
            mockBlockByHash(block);
        }
    }

    public void mockChain(List<NotificationWithLogs> notificationWithLogs) throws IOException {
        for (NotificationWithLogs notificationWithLog : notificationWithLogs) {
            List<String> invoiceIds = notificationWithLog.invoiceIds();
            NewHeadsNotification newHeadsNotification = notificationWithLog.notification();
            NewHead head = newHeadsNotification.getParams().getResult();
            EthBlock block = newBlock(head.getNumber(), head.getHash(), head.getParentHash());
            assert(Bloom.test(head.getLogsBloom(), Web3jClient.CRYPTO_RAIL_ADDRESS, Web3jClient.topic0) == !invoiceIds.isEmpty());
            if (Bloom.test(head.getLogsBloom(), Web3jClient.CRYPTO_RAIL_ADDRESS, Web3jClient.topic0)) {
                List<EthLog.LogResult> results = invoiceIds.stream().map(id -> newLogObj(true, id)).collect(Collectors.toList());
                mockEthGetLogs(head, results);
            } else {
                mockEthGetLogs(head, List.of(newLogObj(false)));
            }
            mockBlockByNumber(block);
            mockBlockByHash(block);
        }

    }

    public void mockBlockByNumber(EthBlock result) throws IOException {
        Request<?, EthBlock> request = mock(Request.class);
        request.setId(result.getId());
        DefaultBlockParameter exact = DefaultBlockParameter.valueOf(result.getBlock().getNumber());
        doReturn(request).when(web3j).ethGetBlockByNumber(argThat(val -> val.getValue().equals(exact.getValue())), eq(false));
        when(request.send()).thenReturn(result);
    }

    public void mockBlockByHash(EthBlock result) throws IOException {
        Request<?, EthBlock> request = mock(Request.class);
        doReturn(request).when(web3j).ethGetBlockByHash(eq(result.getBlock().getHash()), eq(false));
        when(request.send()).thenReturn(result);
    }

    public boolean containsPaymentEvent(NewHead block) {
        return Bloom.test(block.getLogsBloom(), Web3jClient.CRYPTO_RAIL_ADDRESS, Web3jClient.topic0);
    }

    public boolean isBeforeCommonAncestor(NewHead block) {
        NewHead commonAncestor = normalChain.get(5).getParams().getResult();
        return Numeric.toBigInt(block.getNumber()).compareTo(Numeric.toBigInt(commonAncestor.getNumber())) < 0;
    }

    public boolean isAfterOrOnCommonAncestor(NewHead block) {
        NewHead commonAncestor = normalChain.get(5).getParams().getResult();
        return Numeric.toBigInt(block.getNumber()).compareTo(Numeric.toBigInt(commonAncestor.getNumber())) >= 0;
    }


    @BeforeEach
    public void loadBlockFromJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        normalChain = Arrays.stream(objectMapper.readValue(new File("src/test/resources/blocks.json"), NewHeadsNotification[].class)).toList();
        reorgChain = Arrays.stream(objectMapper.readValue(new File("src/test/resources/reorg-blocks.json"), NewHeadsNotification[].class)).toList();
    }

    @BeforeEach
    public void buildClient() {
        newHeadPublisher = PublishProcessor.create();
        when(web3jClientAdapter.getNotification()).thenReturn(newHeadPublisher);
        web3jClient = new Web3jClient(web3jClientAdapter);
        ReflectionTestUtils.setField(web3jClient, "web3j", web3j);
        ReflectionTestUtils.setField(web3jClient, "invoiceService", invoiceService);
        ReflectionTestUtils.setField(web3jClient, "invoicePaymentService", invoicePaymentService);
    }

    @Test
    public void canSeeNewBlock() {
        NewHeadsNotification mockNotification = Mockito.mock(NewHeadsNotification.class);
        NotificationParams<NewHead> mockParams = Mockito.mock(NotificationParams.class);
        NewHead mockNewHead = Mockito.mock(NewHead.class);
        Mockito.when(mockNotification.getParams()).thenReturn(mockParams);
        Mockito.when(mockParams.getResult()).thenReturn(mockNewHead);

        newHeadPublisher.onNext(mockNotification);

        Mockito.verify(mockNotification).getParams();
        Mockito.verify(mockNotification.getParams()).getResult();
    }

    @Test
    public void aNewBlockWithoutTopicDoesNotCallEthGetLogs() {
        newHeadPublisher.onNext(normalChain.getFirst());
        Mockito.verify(web3j, never()).ethGetLogs(any());
    }

    @Test
    public void aNewBlockWithTopicShouldCallEthGetLogs() {
        NewHeadsNotification blockNotificationWithTopic = normalChain.get(1);
        NewHead blockWithTopic = blockNotificationWithTopic.getParams().getResult();
        Assertions.assertEquals("0x00000000000400000000000000000000000000000008000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000", blockWithTopic.getLogsBloom());
        Assertions.assertTrue(containsPaymentEvent(blockWithTopic));
        newHeadPublisher.onNext(blockNotificationWithTopic);
        Mockito.verify(web3j).ethGetLogs(any());
    }

    @Test
    public void aNewBlockWithTopicCanFindMultipleTopicLogs() throws IOException {
        EthLog.LogObject logResult1 = newLogObj(true);
        EthLog.LogObject logResult2 = newLogObj(true);


        NewHeadsNotification notification = normalChain.get(1);
        NewHead block = notification.getParams().getResult();
        mockEthGetLogs(block, List.of(logResult1, logResult2));

        newHeadPublisher.onNext(notification);
        verify(invoicePaymentService).add(new InvoicePaymentDto(
                logResult1.getTopics().get(1),
                Numeric.toBigInt(block.getNumber()),
                block.getHash()
        ));
        verify(invoicePaymentService).add(new InvoicePaymentDto(
                logResult2.getTopics().get(1),
                Numeric.toBigInt(block.getNumber()),
                block.getHash()
        ));
    }

    @Test
    public void aNewBlockWithTopicWillAlwaysCheckForPaymentsToFinalize() throws IOException {
        EthLog.LogObject logResult = newLogObj(true);
        NewHeadsNotification notificationWithTopic = normalChain.get(1);
        NewHead blockWithTopic = notificationWithTopic.getParams().getResult();
        mockEthGetLogs(blockWithTopic, List.of(logResult));
        newHeadPublisher.onNext(notificationWithTopic);
        verify(invoicePaymentService).canFinalize(Numeric.toBigInt(blockWithTopic.getNumber()));
    }

    @Test
    public void aNewBlockWithoutTopicWillAlwaysCheckForPaymentsToFinalize() {
        NewHeadsNotification notificationWithoutTopic = normalChain.getFirst();
        NewHead blockWithoutTopic = notificationWithoutTopic.getParams().getResult();
        newHeadPublisher.onNext(notificationWithoutTopic);
        verify(invoicePaymentService).canFinalize(Numeric.toBigInt(blockWithoutTopic.getNumber()));
    }

    @Test
    public void testParsing() {
        Assertions.assertEquals(10, normalChain.size());
        Assertions.assertEquals(6, reorgChain.size());
    }

    @Test
    public void testChainHasACommonAncestor() {
        NewHead commonAncestor = normalChain.get(5).getParams().getResult();
        NewHead block106 = normalChain.get(6).getParams().getResult();
        NewHead fork106 = reorgChain.get(0).getParams().getResult();

        Assertions.assertEquals(commonAncestor.getHash(), block106.getParentHash());
        Assertions.assertEquals(block106.getParentHash(), fork106.getParentHash());
    }

    @Test
    public void canDetectReorg() throws IOException {
        newHeadPublisher.onNext(normalChain.get(0));
        mockEthGetLogs(normalChain.get(1).getParams().getResult(), List.of(newLogObj(true)));
        newHeadPublisher.onNext(normalChain.get(1));
        newHeadPublisher.onNext(normalChain.get(2));
        newHeadPublisher.onNext(normalChain.get(3));
        newHeadPublisher.onNext(normalChain.get(4));
        newHeadPublisher.onNext(normalChain.get(5));
        newHeadPublisher.onNext(normalChain.get(6));
        Assertions.assertTrue(web3jClient.isReorg(reorgChain.get(1).getParams().getResult()));
    }

    @Test
    public void aReorgWillCheckForPaymentToFinalize() throws IOException {
        NewHead commonAncestor = normalChain.get(5).getParams().getResult();

        mockNormalChain();
        mockForkChain();

        for (NewHeadsNotification notification : normalChain) {
            newHeadPublisher.onNext(notification);
        }

        newHeadPublisher.onNext(reorgChain.get(4)); //trigger reorg
        verify(invoicePaymentService).canFinalize(Numeric.toBigInt(reorgChain.get(4).getParams().getResult().getNumber()));
    }

    @Test
    public void aReorgSweepsFromCommonAncestorToLatest() throws IOException {
        NewHead commonAncestor = normalChain.get(5).getParams().getResult();

        mockNormalChain();
        mockForkChain();

        for (NewHeadsNotification notification : normalChain) {
            newHeadPublisher.onNext(notification);
        }

        newHeadPublisher.onNext(reorgChain.get(4)); //trigger reorg
        verify(web3j).ethGetLogs(argThat(filter -> {
                    if (filter.getFromBlock() == null && filter.getToBlock() == null) {
                        return false;
                    }

                    return filter.getFromBlock().getValue().equals(commonAncestor.getNumber()) &&
                            filter.getToBlock().getValue().equals(DefaultBlockParameterName.LATEST.getValue()) &&
                            filter.getAddress().get(0).equals(Web3jClient.CRYPTO_RAIL_ADDRESS);
                }
        ));
    }

    @Test
    public void initiallyThereIsNoReorg() {
        Assertions.assertFalse(web3jClient.isReorg(normalChain.get(0).getParams().getResult()));
    }

    @Test
    public void mockGetBlockByNumberGrabsTheCorrectBlock() throws IOException {
        mockNormalChain();

        for (NewHeadsNotification notification : normalChain) {
            NewHead expectedBlock = notification.getParams().getResult();
            EthBlock.Block resp = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(Numeric.toBigInt(expectedBlock.getNumber())), false).send().getBlock();
            Assertions.assertEquals(expectedBlock.getHash(), resp.getHash());
        }
    }

    @Test
    public void mockGetBlockByHashGrabsTheCorrectBlock() throws IOException {
        mockNormalChain();

        for (NewHeadsNotification notification : normalChain) {
            NewHead expectedBlock = notification.getParams().getResult();
            EthBlock.Block resp = web3j.ethGetBlockByHash(expectedBlock.getHash(), false).send().getBlock();
            Assertions.assertEquals(expectedBlock.getHash(), resp.getHash());
        }
    }

//    List<Throwable> errors = new ArrayList<>();
//
//    @BeforeEach
//    void captureRxErrors() { RxJavaPlugins.setErrorHandler(errors::add); }
//
//    @AfterEach
//    void resetRx() { RxJavaPlugins.reset(); Assertions.assertTrue(errors.isEmpty(), () -> "handler threw: " + errors); }
}
