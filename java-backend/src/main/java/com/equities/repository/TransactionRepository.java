package com.equities.repository;

import com.equities.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTradeIdOrderByVersionAsc(Long tradeId);

    Optional<Transaction> findByTransactionId(Long transactionId);

    Optional<Transaction> findByTradeIdAndVersion(Long tradeId, Integer version);

    boolean existsByTransactionId(Long transactionId);

    void deleteByTradeId(Long tradeId);

    @Query("SELECT DISTINCT t.tradeId FROM Transaction t ORDER BY t.tradeId")
    List<Long> findAllTradeIds();

    @Query("SELECT t FROM Transaction t WHERE t.version = (SELECT MAX(t2.version) FROM Transaction t2 WHERE t2.tradeId = t.tradeId)")
    List<Transaction> findLatestTransactionsForAllTrades();

    @Query("SELECT t FROM Transaction t WHERE t.id > :lastProcessedId ORDER BY t.id")
    List<Transaction> findTransactionsAfterId(@Param("lastProcessedId") Long lastProcessedId);

    @Query("SELECT MAX(t.id) FROM Transaction t")
    Optional<Long> findMaxTransactionId();

    @Query("SELECT DISTINCT t.tradeId FROM Transaction t WHERE t.securityCode IN :securityCodes ORDER BY t.tradeId")
    List<Long> findTradeIdsBySecurityCodes(@Param("securityCodes") List<String> securityCodes);

    @Query("SELECT t FROM Transaction t WHERE t.tradeId IN :tradeIds ORDER BY t.tradeId, t.version")
    List<Transaction> findTransactionsByTradeIds(@Param("tradeIds") List<Long> tradeIds);

    @Query("SELECT t FROM Transaction t ORDER BY t.tradeId, t.version")
    List<Transaction> findAllByOrderByTradeIdAscVersionAsc();
} 