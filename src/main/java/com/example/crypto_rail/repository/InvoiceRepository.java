package com.example.crypto_rail.repository;

import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.entity.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findByMerchantAddress(String merchantAddress);
    Optional<Invoice> findByHashId(String hashId);
    List<Invoice> findAllByHashIdIn(List<String> hashIds);



    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Invoice i
           SET i.status = :to
         WHERE i.hashId IN :hashIds
           AND i.status = :from
        """)
    int transition(List<String> hashIds, Status from, Status to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        SELECT i.hashId
        FROM Invoice i
        WHERE i.hashId IN :hashIds
        AND i.status = :from
        ORDER BY i.hashId
        """)
    List<String> lockByStatus(List<String> hashIds, Status from);
}


