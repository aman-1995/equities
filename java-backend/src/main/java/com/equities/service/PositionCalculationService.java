package com.equities.service;

import com.equities.model.Position;
import com.equities.model.Transaction;
import com.equities.repository.PositionRepository;
import com.equities.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionCalculationService {

    private final TransactionRepository transactionRepository;
    private final PositionRepository positionRepository;

    @Transactional
    public List<Position> processTransaction(Transaction transaction) {
        log.info("Processing transaction: ID={}, Trade={}, Version={}, Security={}, Qty={}, Action={}, Side={}", 
                transaction.getTransactionId(), 
                transaction.getTradeId(), 
                transaction.getVersion(), 
                transaction.getSecurityCode(), 
                transaction.getQuantity(), 
                transaction.getAction(), 
                transaction.getSide());

        if (transactionRepository.existsByTransactionId(transaction.getTransactionId())) {
            Transaction existing = transactionRepository.findByTransactionId(transaction.getTransactionId()).orElseThrow();
            existing.setTradeId(transaction.getTradeId());
            existing.setVersion(transaction.getVersion());
            existing.setSecurityCode(transaction.getSecurityCode());
            existing.setQuantity(transaction.getQuantity());
            existing.setAction(transaction.getAction());
            existing.setSide(transaction.getSide());
            transactionRepository.save(existing);
        } else {
            transactionRepository.save(transaction);
        }

        return recalculateAllPositions();
    }

    @Transactional
    @Async
    public CompletableFuture<List<Position>> processBulkTransactionsAsync(List<Transaction> transactions) {
        log.info("Processing {} transactions asynchronously", transactions.size());
        
        List<Position> result = processBulkTransactions(transactions);
        return CompletableFuture.completedFuture(result);
    }

    @Transactional
    public List<Position> processBulkTransactions(List<Transaction> transactions) {
        log.info("Processing {} transactions in bulk", transactions.size());
        
        for (Transaction transaction : transactions) {
            log.debug("Processing transaction: ID={}, Trade={}, Version={}, Security={}, Qty={}, Action={}, Side={}", 
                    transaction.getTransactionId(), 
                    transaction.getTradeId(), 
                    transaction.getVersion(), 
                    transaction.getSecurityCode(), 
                    transaction.getQuantity(), 
                    transaction.getAction(), 
                    transaction.getSide());
            
            transactionRepository.save(transaction);
            log.debug("Saved transaction ID {}", transaction.getTransactionId());
        }
        
        log.info("All transactions saved, recalculating positions...");
        return recalculateAllPositions();
    }

    public List<Position> getAllPositions() {
        return positionRepository.findAllByOrderBySecurityCodeAsc();
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Transactional
    public void clearAllData() {
        log.info("Clearing all data");
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Transactional
    public List<Position> loadSampleData() {
        log.info("Loading sample data...");
        
        log.info("Clearing all existing data...");
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
        log.info("All existing data cleared");
        
        List<Transaction> sampleTransactions = List.of(
                Transaction.builder().transactionId(1L).tradeId(1L).version(1).securityCode("REL").quantity(50).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().transactionId(2L).tradeId(2L).version(1).securityCode("ITC").quantity(40).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.SELL).build(),
                Transaction.builder().transactionId(3L).tradeId(3L).version(1).securityCode("INF").quantity(70).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().transactionId(4L).tradeId(1L).version(2).securityCode("REL").quantity(60).action(Transaction.TransactionAction.UPDATE).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().transactionId(5L).tradeId(2L).version(2).securityCode("ITC").quantity(30).action(Transaction.TransactionAction.CANCEL).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().transactionId(6L).tradeId(4L).version(1).securityCode("INF").quantity(20).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.SELL).build()
        );

        log.info("Processing {} sample transactions", sampleTransactions.size());
        List<Position> result = processBulkTransactions(sampleTransactions);
        log.info("Sample data loaded, resulting in {} positions", result.size());
        return result;
    }

    private List<Position> recalculateAllPositions() {
        log.info("Recalculating all positions");
        
        List<Long> tradeIds = transactionRepository.findAllTradeIds();
        log.info("Found {} trade IDs: {}", tradeIds.size(), tradeIds);
        
        Map<String, Integer> positionMap = new ConcurrentHashMap<>();
        
        for (Long tradeId : tradeIds) {
            List<Transaction> tradeTransactions = transactionRepository.findByTradeIdOrderByVersionAsc(tradeId);
            log.debug("Trade {} has {} transactions", tradeId, tradeTransactions.size());
            processTradeForPositions(tradeTransactions, positionMap);
        }
        
        log.info("Final position map: {}", positionMap);
        
        List<Position> positions = positionMap.entrySet().stream()
                .map(entry -> Position.builder()
                        .securityCode(entry.getKey())
                        .quantity(entry.getValue())
                        .build())
                .sorted((p1, p2) -> p1.getSecurityCode().compareTo(p2.getSecurityCode()))
                .collect(Collectors.toList());
        
        try {
            positionRepository.deleteAll();
            
            List<Position> savedPositions = new ArrayList<>();
            for (Position position : positions) {
                Position savedPosition = positionRepository.save(position);
                savedPositions.add(savedPosition);
                log.debug("Saved position: {} = {}", position.getSecurityCode(), position.getQuantity());
            }
            
            log.info("Recalculated {} positions: {}", savedPositions.size(), 
                    savedPositions.stream()
                            .map(p -> p.getSecurityCode() + ":" + p.getQuantity())
                            .collect(Collectors.joining(", ")));
            return savedPositions;
            
        } catch (Exception e) {
            log.error("Error in position recalculation: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void processTradeForPositions(List<Transaction> transactions, Map<String, Integer> positionMap) {
        if (transactions.isEmpty()) {
            return;
        }
        
        Long tradeId = transactions.get(0).getTradeId();
        log.debug("Processing trade {} with {} transactions", tradeId, transactions.size());
        
        boolean isCancelled = transactions.stream()
                .anyMatch(tx -> tx.getAction() == Transaction.TransactionAction.CANCEL);
        
        if (isCancelled) {
            Transaction latestTransaction = transactions.stream()
                    .max((t1, t2) -> Integer.compare(t1.getVersion(), t2.getVersion()))
                    .orElse(null);
            
            if (latestTransaction != null) {
                String securityCode = latestTransaction.getSecurityCode();
                positionMap.put(securityCode, 0);
                log.debug("Trade {} is cancelled, setting {} position to 0", tradeId, securityCode);
            } else {
                log.warn("No latest transaction found for cancelled trade {}", tradeId);
            }
            return;
        }
        
        Transaction latestTransaction = transactions.stream()
                .max((t1, t2) -> Integer.compare(t1.getVersion(), t2.getVersion()))
                .orElse(null);
        
        if (latestTransaction == null) {
            log.warn("No latest transaction found for trade {}", tradeId);
            return;
        }
        
        String securityCode = latestTransaction.getSecurityCode();
        int quantity = latestTransaction.getQuantity();
        int impact = latestTransaction.getSide() == Transaction.TransactionSide.BUY ? quantity : -quantity;
        
        int previousValue = positionMap.getOrDefault(securityCode, 0);
        positionMap.merge(securityCode, impact, Integer::sum);
        int newValue = positionMap.get(securityCode);
        
        log.debug("Trade {}: {} {} {} = impact {} ({} -> {})", 
                tradeId, 
                securityCode, 
                latestTransaction.getSide(), 
                quantity, 
                impact,
                previousValue,
                newValue);
    }
} 