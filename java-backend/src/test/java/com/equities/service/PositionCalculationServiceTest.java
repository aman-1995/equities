package com.equities.service;

import com.equities.model.Position;
import com.equities.model.Transaction;
import com.equities.repository.PositionRepository;
import com.equities.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PositionCalculationServiceTest {

    @Autowired
    private PositionCalculationService positionCalculationService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PositionRepository positionRepository;

    @BeforeEach
    void setUp() {
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    void testProcessSingleInsertTransaction() {
        Transaction transaction = Transaction.builder()
                .transactionId(1L)
                .tradeId(1L)
                .version(1)
                .securityCode("REL")
                .quantity(50)
                .action(Transaction.TransactionAction.INSERT)
                .side(Transaction.TransactionSide.BUY)
                .build();

        List<Position> result = positionCalculationService.processTransaction(transaction);

        assertEquals(1, result.size());
        Position position = result.get(0);
        assertEquals("REL", position.getSecurityCode());
        assertEquals(50, position.getQuantity());
    }

    @Test
    void testProcessBulkTransactionsAsync() {
        List<Transaction> transactions = List.of(
                Transaction.builder().transactionId(1L).tradeId(1L).version(1).securityCode("REL").quantity(50).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().transactionId(2L).tradeId(2L).version(1).securityCode("ITC").quantity(40).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.SELL).build()
        );

        CompletableFuture<List<Position>> future = positionCalculationService.processBulkTransactionsAsync(transactions);

        try {
            List<Position> result = future.get();
            assertEquals(2, result.size());
            assertEquals("ITC", result.get(0).getSecurityCode());
            assertEquals("REL", result.get(1).getSecurityCode());
        } catch (Exception e) {
            fail("Async operation failed: " + e.getMessage());
        }
    }

    @Test
    void testLoadSampleData() {
        List<Position> result = positionCalculationService.loadSampleData();

        assertEquals(3, result.size());
        
        Position relPosition = result.stream()
                .filter(p -> "REL".equals(p.getSecurityCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(60, relPosition.getQuantity());
        
        Position itcPosition = result.stream()
                .filter(p -> "ITC".equals(p.getSecurityCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, itcPosition.getQuantity());
        
        Position infPosition = result.stream()
                .filter(p -> "INF".equals(p.getSecurityCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(50, infPosition.getQuantity());
        
        assertEquals("INF", result.get(0).getSecurityCode());
        assertEquals("ITC", result.get(1).getSecurityCode());
        assertEquals("REL", result.get(2).getSecurityCode());
    }
} 