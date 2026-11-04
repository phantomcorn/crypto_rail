package com.example.crypto_rail.utils;

import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.service.InvoiceService;
import com.example.crypto_rail.service.InvoicePaymentService;
import com.example.crypto_rail.dto.InvoicePaymentDto;
import com.example.crypto_rail.utils.dto.NewSignatureDto;
import com.example.crypto_rail.utils.dto.BlockDto;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.websocket.events.NewHead;
import org.web3j.protocol.websocket.events.NewHeadsNotification;
import org.web3j.utils.Bloom;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Profile("!test")
public class Web3jClient {

    private static final BigInteger FIVE_MINUTES = BigInteger.valueOf(500);
    public static final Long CHAIN_ID = 11155111L;
    public static final String APP_NAME = "CryptoRail";
    public static final String CRYPTO_RAIL_ADDRESS = "0xd0F29Ad5DC15a44df7461D16C125cfd809150F19";
    public static final String APP_VERSION = "0.0.1";
    public static final String topic0 = Hash.sha3String("PaymentReceived(bytes32)");

    private final Deque<BlockDto> processedBlock = new ArrayDeque<>();

    @Getter
    Disposable disposable;

    @Autowired
    Web3j web3j;

    @Autowired
    InvoiceService invoiceService;

    @Autowired
    InvoicePaymentService invoicePaymentService;

    public Web3jClient(Web3jAdapter adapter) {
        //            log.error("newHeads subscriber failed", err);
        disposable = adapter.getPublisher().subscribe(this::handler, adapter::onError);
    }

    private void handler(NewHeadsNotification notification) throws IOException {
        NewHead newBlock = notification.getParams().getResult();
        BigInteger newBlockNumber = Numeric.toBigInt(newBlock.getNumber());
        String msg = "";
        if (isReorg(newBlock)) {
            msg += "Reorg: True";
            handleReorg(newBlock);
        } else {
            msg += "Reorg: False";
        }


        boolean exists = Bloom.test(newBlock.getLogsBloom(), CRYPTO_RAIL_ADDRESS, topic0);
        if (exists) {

            EthLog ethLog = web3j.ethGetLogs(
                    new EthFilter(newBlock.getHash(), CRYPTO_RAIL_ADDRESS).addSingleTopic(topic0)
            ).send();

            //Find all PaymentReceived(hash_id) emitted in this new block
            for (EthLog.LogResult<?> result : ethLog.getLogs()) {
                if (result instanceof Log) {
                    Log log = (Log) result.get();
                    List<String> topics = log.getTopics();
                    assert topics.size() == 2; //topic0 will always be keccack of function signature, topic1 always be first indexed argument see: https://www.alchemy.com/docs/deep-dive-into-eth_getlogs#deciphering-the-response
                    String invoiceHashId = topics.get(1);
                    if (!invoiceService.detectPayment(invoiceHashId)) {
                        //log not found invoice id
                    }
                    //log update status to DETECTED
                    invoicePaymentService.add(new InvoicePaymentDto(invoiceHashId, newBlockNumber, newBlock.getHash()));
                    System.out.println("Invoice " + invoiceHashId + " -> DETECTED");
                }
            }
        }

        System.out.println("Skipping " + newBlockNumber + " " + msg);

        //Find every invoice that can be marked finalized
        List<InvoicePaymentDto> payments = invoicePaymentService.canFinalize(newBlockNumber);
        invoiceService.toFinality(payments.stream().map(InvoicePaymentDto::invoiceHashId).toList());

        //Mark this block as processed
        processedBlock.addLast(new BlockDto(newBlock.getNumber(), newBlock.getHash()));
    }

    public boolean isReorg(NewHead newHead) {
        // [B] <- [A]
        // Make sure newHead's parent hash points to block B (i.e last processed block)
        if (processedBlock.isEmpty()) {
            return false;
        }
        BlockDto prevBlock = processedBlock.getLast();
        return !newHead.getParentHash().equals(prevBlock.blockHash());
    }

    public void handleReorg(NewHead head) throws IOException {

        BlockDto commonBlock = commonAncestor(head);


        //Sweep starting from p to latest block
        Request<?, EthLog> request = web3j.ethGetLogs(new EthFilter(
                DefaultBlockParameter.valueOf(Numeric.toBigInt(commonBlock.blockNumber())),
                DefaultBlockParameterName.LATEST,
                CRYPTO_RAIL_ADDRESS
        ).addSingleTopic(topic0));

        List<EthLog.LogResult> logResults = request.send().getLogs();
        List<EthLog.LogResult> filteredLogResults = logResults.stream().filter(
                result -> {
                    if (result instanceof Log) {
                        Log log = (Log) result.get();
                        List<String> topics = log.getTopics();
                        return topics.size() == 2; //topic0 will always be keccack of function signature, topic1 always be first indexed argument see: https://www.alchemy.com/docs/deep-dive-into-eth_getlogs#deciphering-the-response

                    }
                    return false;
                }
        ).toList();

        Set<InvoicePaymentDto> paymentInDb = new HashSet<>(invoicePaymentService.getPaymentFrom(Numeric.toBigInt(commonBlock.blockNumber())));

        if (filteredLogResults.stream().findAny().isEmpty()) {
            //No payment on chain, reorg entire payment in DB
            List<String> droppedHashIds = paymentInDb.stream().map(InvoicePaymentDto::invoiceHashId).toList();
            invoicePaymentService.removeAllById(droppedHashIds);
            invoiceService.revertAll(droppedHashIds);
            return;
        }

        Set<InvoicePaymentDto> paymentOnChain = filteredLogResults.stream().map(result -> {
            Log log = (Log) result;
            return new InvoicePaymentDto(log.getTopics().get(1), log.getBlockNumber(), log.getBlockHash());
        }).collect(Collectors.toSet());




        /*
        We have two list ids hashIdsOnChain and hashIdsInDb. We need to process them based on their intersection

        Predicate:
          - All CONFIRMED invoice should never get reorg
          - Any invoice already in hashIdsInDb are DETECTED/REORG/CONFIRMED
          - Any hashIdsOnReorgBlocks not in hashIdsInDb are new and should be considered a new payment -> new InvoicePayment
          - Any hashIdsInDb not in hashIdsOnReorgBlocks are reorged
          - In both -> REORG but still included therefore no change
         */

        Set<String> hashIdsOnChain = paymentOnChain.stream().map(InvoicePaymentDto::invoiceHashId).collect(Collectors.toSet());
        Set<String> hashIdsInDb = paymentInDb.stream().map(InvoicePaymentDto::invoiceHashId).collect(Collectors.toSet());

        List<InvoicePaymentDto> stillValidNeedUpdate = paymentOnChain.stream().filter(payment -> hashIdsInDb.contains(payment.invoiceHashId())).toList(); //update entry to new block number + hash
        List<InvoicePaymentDto> newPayments = paymentOnChain.stream().filter(payment -> !hashIdsInDb.contains(payment.invoiceHashId())).toList();// Add to invoice_payments table
        List<InvoicePaymentDto> droppedPayment = paymentInDb.stream().filter(payment -> !hashIdsOnChain.contains(payment.invoiceHashId())).toList();// Update invoice status to REORG

        //1. update valid payments but modified block number + hash
        invoicePaymentService.update(stillValidNeedUpdate);
        //2. add new payments to table and update status
        List<String> newHashIds = newPayments.stream().map(InvoicePaymentDto::invoiceHashId).toList();
        invoicePaymentService.insert(newPayments);
        invoiceService.detectPaymentAll(newHashIds);
        //3. drop payment (Delete payment entry and update status in invoices table)
        List<String> droppedHashIds = droppedPayment.stream().map(InvoicePaymentDto::invoiceHashId).toList();
        invoicePaymentService.removeAllById(droppedHashIds);
        invoiceService.revertAll(droppedHashIds);









    }

    private BlockDto commonAncestor(NewHead head) throws IOException {
        //To reach this point: must always be atleast 1 processed block
        //ASSUMPTION: NewHead always 1 block greater than last processedBlock number
        //We find the common block number between forked head and block p
        BlockDto p = processedBlock.removeLast();
        assert(Numeric.toBigInt(p.blockNumber()).add(BigInteger.ONE).equals(Numeric.toBigInt(head.getNumber())));
        EthBlock.Block q = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(Numeric.toBigInt(p.blockNumber())), false).send().getBlock();

        while(!p.blockHash().equals(q.getHash())) {
            //if processedBlock is empty => common ancestor becomes the fork common block (q)
            if (processedBlock.isEmpty()) {
                return new BlockDto(Numeric.toHexString(q.getNumber().toByteArray()), q.getHash());
            }
            p = processedBlock.removeLast();
            q = web3j.ethGetBlockByHash(q.getParentHash(),false).send().getBlock();
        }

        System.out.println(Numeric.toBigInt(p.blockNumber()) + "  " + p.blockHash());
        System.out.println(q.getNumber() + "  " + q.getHash());
        assert(Numeric.toBigInt(p.blockNumber()).equals(q.getNumber()) && p.blockHash().equals(q.getHash()));
        return p;
    }





    public static NewSignatureDto signTypedMesssge(Web3j web3j, Credentials credentials, PayRequestDto dto) throws IOException {
        String jsonMessageString = Files.readString(Paths.get("src/main/resources/static/typedData.json").toAbsolutePath());
        JsonObject jsonObject = new JsonObject(jsonMessageString);
        BigInteger deadline = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock().getTimestamp()
                .add(FIVE_MINUTES);
        jsonObject.getJsonObject("message").put("id", dto.getHashId());
        jsonObject.getJsonObject("message").put("tokenAddr", dto.getTokenAddress());
        jsonObject.getJsonObject("message").put("amount", dto.getAmount());
        jsonObject.getJsonObject("message").put("merchantAddr", dto.getMerchantAddress());
        jsonObject.getJsonObject("message").put("deadline", deadline);

        jsonObject.getJsonObject("domain").put("name", APP_NAME);
        jsonObject.getJsonObject("domain").put("version", APP_VERSION);
        jsonObject.getJsonObject("domain").put("chainId", CHAIN_ID);
        jsonObject.getJsonObject("domain").put("verifyingContract", CRYPTO_RAIL_ADDRESS);

        Sign.SignatureData signatureData = Sign.signTypedData(jsonObject.toString(), credentials.getEcKeyPair());
        byte[] r = signatureData.getR();
        byte[] s = signatureData.getS();
        byte[] v = signatureData.getV();

        // Standard 65-byte layout: 32 bytes R, 32 bytes S, 1 byte V
        byte[] signatureBytes = new byte[65];
        System.arraycopy(r, 0, signatureBytes, 0, 32);
        System.arraycopy(s, 0, signatureBytes, 32, 32);
        System.arraycopy(v, 0, signatureBytes, 64, 1);

        return new NewSignatureDto(Numeric.toHexString(signatureBytes), deadline);
    }

}
