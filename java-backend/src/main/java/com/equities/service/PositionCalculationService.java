package com.equities.service;

import com.equities.model.Position;
import com.equities.model.ProcessingState;
import com.equities.model.Transaction;
import com.equities.repository.PositionRepository;
import com.equities.repository.ProcessingStateRepository;
import com.equities.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionCalculationService {

    private final TransactionRepository transactionRepository;
    private final PositionRepository positionRepository;
    private final ProcessingStateRepository processingStateRepository;

    @Transactional
    public List<Position> processTransaction(Transaction transaction) {
        log.info("Processing transaction: Trade={}, Security={}, Qty={}", 
                transaction.getTradeId(), transaction.getSecurityCode(), transaction.getQuantity());

        boolean isEdit = false;
        String affectedSecurity = null;

        // For new transactions, transactionId will be null and will be auto-generated
        if (transaction.getTransactionId() != null && transactionRepository.existsByTransactionId(transaction.getTransactionId())) {
            // Update existing transaction - validate it's the latest version
            Transaction existing = transactionRepository.findByTransactionId(transaction.getTransactionId()).orElseThrow();
            validateTransactionEdit(existing);
            
            // Store the affected security for recalculation
            affectedSecurity = existing.getSecurityCode();
            isEdit = true;
            
            existing.setTradeId(transaction.getTradeId());
            existing.setVersion(transaction.getVersion());
            existing.setSecurityCode(transaction.getSecurityCode());
            existing.setQuantity(transaction.getQuantity());
            existing.setAction(transaction.getAction());
            existing.setSide(transaction.getSide());
            transactionRepository.save(existing);
        } else {
            // Create new transaction (transactionId will be auto-generated)
            if (transaction.getTransactionId() == null) {
                transaction.setTransactionId(generateNextTransactionId());
            }
            transactionRepository.save(transaction);
        }

        // For edits, we need to recalculate positions for the affected security
        if (isEdit) {
            return recalculatePositionsForSecurity(affectedSecurity);
        } else {
            return recalculatePositionsDelta();
        }
    }

    @Transactional
    public List<Position> processBulkTransactions(List<Transaction> transactions) {
        log.info("Processing {} transactions in bulk", transactions.size());
        
        // Generate transaction IDs for new transactions
        Long currentMaxId = transactionRepository.findMaxTransactionId().orElse(0L);
        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);
            if (transaction.getTransactionId() == null) {
                transaction.setTransactionId(currentMaxId + i + 1);
            }
        }
        
        transactionRepository.saveAll(transactions);
        return recalculatePositionsDelta();
    }

    public List<Position> getAllPositions() {
        return positionRepository.findAllByOrderBySecurityCodeAsc();
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = transactionRepository.findAll();
        return populateLatestVersionFlags(transactions);
    }

    @Transactional
    public void clearAllData() {
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
        processingStateRepository.deleteAll();
    }

    @Transactional
    public List<Position> forceFullRecalculation() {
        return recalculateAllPositions();
    }

    public ProcessingState getProcessingState() {
        return processingStateRepository.findByStateKey("POSITION_CALCULATION")
                .orElse(ProcessingState.builder()
                        .stateKey("POSITION_CALCULATION")
                        .lastProcessedTransactionId(0L)
                        .lastProcessedTimestamp(LocalDateTime.now())
                        .build());
    }

    @Transactional
    public List<Position> loadSampleData() {
        clearAllData();
        
        List<Transaction> sampleTransactions = List.of(
                Transaction.builder().tradeId(1L).version(1).securityCode("REL").quantity(50).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().tradeId(2L).version(1).securityCode("ITC").quantity(40).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.SELL).build(),
                Transaction.builder().tradeId(3L).version(1).securityCode("INF").quantity(70).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().tradeId(1L).version(2).securityCode("REL").quantity(60).action(Transaction.TransactionAction.UPDATE).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().tradeId(2L).version(2).securityCode("ITC").quantity(30).action(Transaction.TransactionAction.CANCEL).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().tradeId(4L).version(1).securityCode("INF").quantity(20).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.SELL).build()
        );

        return processBulkTransactions(sampleTransactions);
    }

    private List<Position> recalculatePositionsDelta() {
        Long lastProcessedId = getLastProcessedTransactionId();
        List<Transaction> newTransactions = transactionRepository.findTransactionsAfterId(lastProcessedId);
        
        if (newTransactions.isEmpty()) {
            return getAllPositions();
        }
        
        // Get affected securities from new transactions
        Set<String> affectedSecurities = new HashSet<>();
        for (Transaction tx : newTransactions) {
            affectedSecurities.add(tx.getSecurityCode());
        }
        
        // Get current positions and reset affected securities
        Map<String, Integer> currentPositions = getAllPositions().stream()
                .collect(Collectors.toMap(Position::getSecurityCode, Position::getQuantity));
        
        for (String securityCode : affectedSecurities) {
            currentPositions.put(securityCode, 0);
        }
        
        // Get only trades that affect the affected securities
        List<Long> relevantTradeIds = transactionRepository.findTradeIdsBySecurityCodes(new ArrayList<>(affectedSecurities));
        
        // Get all transactions for relevant trades in one query
        List<Transaction> relevantTransactions = transactionRepository.findTransactionsByTradeIds(relevantTradeIds);
        
        // Group transactions by trade ID and process each trade
        Map<Long, List<Transaction>> transactionsByTrade = relevantTransactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTradeId));
        
        for (List<Transaction> tradeTransactions : transactionsByTrade.values()) {
            processTradeForPositions(tradeTransactions, currentPositions);
        }
        
        updatePositionsInDatabase(currentPositions);
        updateLastProcessedTransactionId(newTransactions);
        
        return getAllPositions();
    }

    private Long getLastProcessedTransactionId() {
        return processingStateRepository.findByStateKey("POSITION_CALCULATION")
                .map(ProcessingState::getLastProcessedTransactionId)
                .orElse(0L);
    }

    private void updateLastProcessedTransactionId(List<Transaction> newTransactions) {
        Long maxNewTransactionId = newTransactions.stream()
                .mapToLong(Transaction::getTransactionId)
                .max()
                .orElse(0L);
        
        Long currentLastProcessedId = getLastProcessedTransactionId();
        Long newLastProcessedId = Math.max(currentLastProcessedId, maxNewTransactionId);
        
        ProcessingState state = processingStateRepository.findByStateKey("POSITION_CALCULATION")
                .orElse(ProcessingState.builder()
                        .stateKey("POSITION_CALCULATION")
                        .lastProcessedTransactionId(0L)
                        .lastProcessedTimestamp(LocalDateTime.now())
                        .build());
        
        state.setLastProcessedTransactionId(newLastProcessedId);
        state.setLastProcessedTimestamp(LocalDateTime.now());
        processingStateRepository.save(state);
    }

    private Long generateNextTransactionId() {
        Long maxTransactionId = transactionRepository.findMaxTransactionId().orElse(0L);
        return maxTransactionId + 1;
    }

    private void validateTransactionEdit(Transaction transaction) {
        Long tradeId = transaction.getTradeId();
        Optional<Transaction> latestTransaction = transactionRepository.findLatestTransactionByTradeId(tradeId);
        
        if (latestTransaction.isPresent()) {
            Transaction latest = latestTransaction.get();
            if (!latest.getTransactionId().equals(transaction.getTransactionId())) {
                throw new TransactionEditException(
                    String.format("Cannot edit transaction %d (version %d) for trade %d. " +
                                "Only the latest transaction version %d can be edited.",
                                transaction.getTransactionId(), transaction.getVersion(), 
                                tradeId, latest.getVersion())
                );
            }
        }
    }

    private List<Transaction> populateLatestVersionFlags(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return transactions;
        }

        // Group transactions by trade ID
        Map<Long, List<Transaction>> transactionsByTrade = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTradeId));

        // For each trade, find the latest version and mark it
        for (List<Transaction> tradeTransactions : transactionsByTrade.values()) {
            if (tradeTransactions.isEmpty()) {
                continue;
            }

            // Find the transaction with the highest version for this trade
            Transaction latestTransaction = tradeTransactions.stream()
                    .max(Comparator.comparingInt(Transaction::getVersion))
                    .orElse(null);

            if (latestTransaction != null) {
                // Mark all transactions in this trade with their latest version status
                for (Transaction transaction : tradeTransactions) {
                    transaction.setIsLatestVersion(
                        transaction.getTransactionId().equals(latestTransaction.getTransactionId())
                    );
                }
            }
        }

        return transactions;
    }

    private List<Position> recalculatePositionsForSecurity(String securityCode) {
        // Get all trades that affect this security
        List<Long> relevantTradeIds = transactionRepository.findTradeIdsBySecurityCodes(List.of(securityCode));
        
        // Get all transactions for relevant trades
        List<Transaction> relevantTransactions = transactionRepository.findTransactionsByTradeIds(relevantTradeIds);
        
        // Get current positions
        Map<String, Integer> currentPositions = getAllPositions().stream()
                .collect(Collectors.toMap(Position::getSecurityCode, Position::getQuantity));
        
        // Reset the affected security
        currentPositions.put(securityCode, 0);
        
        // Group transactions by trade ID and process each trade
        Map<Long, List<Transaction>> transactionsByTrade = relevantTransactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTradeId));
        
        for (List<Transaction> tradeTransactions : transactionsByTrade.values()) {
            processTradeForPositions(tradeTransactions, currentPositions);
        }
        
        updatePositionsInDatabase(currentPositions);
        
        return getAllPositions();
    }

    private void updatePositionsInDatabase(Map<String, Integer> positionMap) {
        List<Position> existingPositions = positionRepository.findAll();
        Map<String, Position> existingPositionMap = existingPositions.stream()
                .collect(Collectors.toMap(Position::getSecurityCode, p -> p));
        
        // Update or create positions
        for (Map.Entry<String, Integer> entry : positionMap.entrySet()) {
            String securityCode = entry.getKey();
            Integer quantity = entry.getValue();
            
            Position existingPosition = existingPositionMap.get(securityCode);
            
            if (existingPosition != null) {
                existingPosition.setQuantity(quantity);
                positionRepository.save(existingPosition);
            } else {
                Position newPosition = Position.builder()
                        .securityCode(securityCode)
                        .quantity(quantity)
                        .build();
                positionRepository.save(newPosition);
            }
        }
        
        // Remove positions with zero quantity
        for (Position existingPosition : existingPositions) {
            if (!positionMap.containsKey(existingPosition.getSecurityCode()) || 
                positionMap.get(existingPosition.getSecurityCode()) == 0) {
                positionRepository.delete(existingPosition);
            }
        }
    }

    private List<Position> recalculateAllPositions() {
        // Get all transactions in one query, ordered by trade ID and version
        List<Transaction> allTransactions = transactionRepository.findAllByOrderByTradeIdAscVersionAsc();
        Map<String, Integer> positionMap = new ConcurrentHashMap<>();
        
        // Group transactions by trade ID and process each trade
        Map<Long, List<Transaction>> transactionsByTrade = allTransactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTradeId));
        
        for (List<Transaction> tradeTransactions : transactionsByTrade.values()) {
            processTradeForPositions(tradeTransactions, positionMap);
        }
        
        List<Position> positions = positionMap.entrySet().stream()
                .map(entry -> Position.builder()
                        .securityCode(entry.getKey())
                        .quantity(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(Position::getSecurityCode))
                .toList();
        
        try {
            List<Position> existingPositions = positionRepository.findAll();
            Map<String, Position> existingPositionMap = existingPositions.stream()
                    .collect(Collectors.toMap(Position::getSecurityCode, p -> p));
            
            List<Position> savedPositions = new ArrayList<>();
            
            for (Position position : positions) {
                Position existingPosition = existingPositionMap.get(position.getSecurityCode());
                Position savedPosition;
                
                if (existingPosition != null) {
                    existingPosition.setQuantity(position.getQuantity());
                    savedPosition = positionRepository.save(existingPosition);
                } else {
                    savedPosition = positionRepository.save(position);
                }
                savedPositions.add(savedPosition);
            }
            
            for (Position existingPosition : existingPositions) {
                if (!positionMap.containsKey(existingPosition.getSecurityCode()) || 
                    positionMap.get(existingPosition.getSecurityCode()) == 0) {
                    positionRepository.delete(existingPosition);
                }
            }
            
            // Update processing state for full recalculation
            List<Transaction> allTransactionsForState = transactionRepository.findAll();
            updateLastProcessedTransactionId(allTransactionsForState);
            
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
        
        boolean isCancelled = transactions.stream()
                .anyMatch(tx -> tx.getAction() == Transaction.TransactionAction.CANCEL);
        
        if (isCancelled) {
            Transaction latestTransaction = transactions.stream()
                    .max(Comparator.comparingInt(Transaction::getVersion))
                    .orElse(null);

            String securityCode = latestTransaction.getSecurityCode();
            positionMap.put(securityCode, 0);
            return;
        }
        
        Transaction latestTransaction = transactions.stream()
                .max(Comparator.comparingInt(Transaction::getVersion))
                .orElse(null);
        
        if (latestTransaction == null) {
            return;
        }
        
        String securityCode = latestTransaction.getSecurityCode();
        int quantity = latestTransaction.getQuantity();
        int impact = latestTransaction.getSide() == Transaction.TransactionSide.BUY ? quantity : -quantity;
        
        positionMap.merge(securityCode, impact, Integer::sum);
    }
} 