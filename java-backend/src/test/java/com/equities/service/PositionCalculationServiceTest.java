package com.equities.service;

import com.equities.model.Position;
import com.equities.model.Transaction;
import com.equities.repository.PositionRepository;
import com.equities.repository.TransactionRepository;
import com.equities.service.TransactionEditException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

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

    @Test
    void testEditLatestTransactionShouldSucceed() {
        // Create initial transaction
        Transaction transaction1 = Transaction.builder()
                .tradeId(1L)
                .version(1)
                .securityCode("REL")
                .quantity(50)
                .action(Transaction.TransactionAction.INSERT)
                .side(Transaction.TransactionSide.BUY)
                .build();

        List<Position> result1 = positionCalculationService.processTransaction(transaction1);
        assertEquals(1, result1.size());
        assertEquals(50, result1.get(0).getQuantity());

        // Get the saved transaction and edit it
        List<Transaction> allTransactions = positionCalculationService.getAllTransactions();
        Transaction savedTransaction = allTransactions.get(0);
        
        // Edit the latest transaction
        savedTransaction.setQuantity(75);
        List<Position> result2 = positionCalculationService.processTransaction(savedTransaction);
        
        assertEquals(1, result2.size());
        assertEquals(75, result2.get(0).getQuantity());
    }

    @Test
    void testEditNonLatestTransactionShouldFail() {
        // Create initial transaction
        Transaction transaction1 = Transaction.builder()
                .tradeId(1L)
                .version(1)
                .securityCode("REL")
                .quantity(50)
                .action(Transaction.TransactionAction.INSERT)
                .side(Transaction.TransactionSide.BUY)
                .build();

        positionCalculationService.processTransaction(transaction1);

        // Create a new version of the same trade
        Transaction transaction2 = Transaction.builder()
                .tradeId(1L)
                .version(2)
                .securityCode("REL")
                .quantity(60)
                .action(Transaction.TransactionAction.UPDATE)
                .side(Transaction.TransactionSide.BUY)
                .build();

        positionCalculationService.processTransaction(transaction2);

        // Try to edit the first transaction (non-latest)
        List<Transaction> allTransactions = positionCalculationService.getAllTransactions();
        Transaction firstTransaction = allTransactions.stream()
                .filter(t -> t.getVersion() == 1)
                .findFirst()
                .orElseThrow();

        firstTransaction.setQuantity(100);

        // Should throw exception
        TransactionEditException exception = assertThrows(TransactionEditException.class, () -> {
            positionCalculationService.processTransaction(firstTransaction);
        });

        assertTrue(exception.getMessage().contains("Cannot edit transaction"));
        assertTrue(exception.getMessage().contains("Only the latest transaction version"));
    }

    @Test
    void testEditLatestTransactionWithMultipleTrades() {
        // Create transactions for different trades
        Transaction transaction1 = Transaction.builder()
                .tradeId(1L)
                .version(1)
                .securityCode("REL")
                .quantity(50)
                .action(Transaction.TransactionAction.INSERT)
                .side(Transaction.TransactionSide.BUY)
                .build();

        Transaction transaction2 = Transaction.builder()
                .tradeId(2L)
                .version(1)
                .securityCode("ITC")
                .quantity(40)
                .action(Transaction.TransactionAction.INSERT)
                .side(Transaction.TransactionSide.SELL)
                .build();

        positionCalculationService.processTransaction(transaction1);
        positionCalculationService.processTransaction(transaction2);

        // Create new version for trade 1
        Transaction transaction3 = Transaction.builder()
                .tradeId(1L)
                .version(2)
                .securityCode("REL")
                .quantity(60)
                .action(Transaction.TransactionAction.UPDATE)
                .side(Transaction.TransactionSide.BUY)
                .build();

        positionCalculationService.processTransaction(transaction3);

        // Try to edit the latest transaction of trade 1 (should succeed)
        List<Transaction> allTransactions = positionCalculationService.getAllTransactions();
        Transaction latestTrade1Transaction = allTransactions.stream()
                .filter(t -> t.getTradeId() == 1L && t.getVersion() == 2)
                .findFirst()
                .orElseThrow();

        latestTrade1Transaction.setQuantity(80);
        List<Position> result = positionCalculationService.processTransaction(latestTrade1Transaction);
        
        // Should succeed and update position
        Position relPosition = result.stream()
                .filter(p -> "REL".equals(p.getSecurityCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(80, relPosition.getQuantity());
    }

    @Test
    void testEditNonLatestTransactionWithMultipleTrades() {
        // Create transactions for different trades
        Transaction transaction1 = Transaction.builder()
                .tradeId(1L)
                .version(1)
                .securityCode("REL")
                .quantity(50)
                .action(Transaction.TransactionAction.INSERT)
                .side(Transaction.TransactionSide.BUY)
                .build();

        Transaction transaction2 = Transaction.builder()
                .tradeId(2L)
                .version(1)
                .securityCode("ITC")
                .quantity(40)
                .action(Transaction.TransactionAction.INSERT)
                .side(Transaction.TransactionSide.SELL)
                .build();

        positionCalculationService.processTransaction(transaction1);
        positionCalculationService.processTransaction(transaction2);

        // Create new version for trade 1
        Transaction transaction3 = Transaction.builder()
                .tradeId(1L)
                .version(2)
                .securityCode("REL")
                .quantity(60)
                .action(Transaction.TransactionAction.UPDATE)
                .side(Transaction.TransactionSide.BUY)
                .build();

        positionCalculationService.processTransaction(transaction3);

        // Try to edit the non-latest transaction of trade 1 (should fail)
        List<Transaction> allTransactions = positionCalculationService.getAllTransactions();
        Transaction nonLatestTrade1Transaction = allTransactions.stream()
                .filter(t -> t.getTradeId() == 1L && t.getVersion() == 1)
                .findFirst()
                .orElseThrow();

        nonLatestTrade1Transaction.setQuantity(100);

        // Should throw exception
        TransactionEditException exception = assertThrows(TransactionEditException.class, () -> {
            positionCalculationService.processTransaction(nonLatestTrade1Transaction);
        });

        assertTrue(exception.getMessage().contains("Cannot edit transaction"));
        assertTrue(exception.getMessage().contains("Only the latest transaction version"));
    }

    @Test
    void testGetAllTransactionsWithLatestVersionFlags() {
        // Create transactions for different trades
        Transaction transaction1 = Transaction.builder()
                .tradeId(1L)
                .version(1)
                .securityCode("REL")
                .quantity(50)
                .action(Transaction.TransactionAction.INSERT)
                .side(Transaction.TransactionSide.BUY)
                .build();

        Transaction transaction2 = Transaction.builder()
                .tradeId(1L)
                .version(2)
                .securityCode("REL")
                .quantity(60)
                .action(Transaction.TransactionAction.UPDATE)
                .side(Transaction.TransactionSide.BUY)
                .build();

        Transaction transaction3 = Transaction.builder()
                .tradeId(2L)
                .version(1)
                .securityCode("ITC")
                .quantity(40)
                .action(Transaction.TransactionAction.INSERT)
                .side(Transaction.TransactionSide.SELL)
                .build();

        positionCalculationService.processTransaction(transaction1);
        positionCalculationService.processTransaction(transaction2);
        positionCalculationService.processTransaction(transaction3);

        List<Transaction> allTransactions = positionCalculationService.getAllTransactions();

        // Find transactions by their characteristics
        Transaction relV1 = allTransactions.stream()
                .filter(t -> t.getTradeId() == 1L && t.getVersion() == 1)
                .findFirst()
                .orElseThrow();

        Transaction relV2 = allTransactions.stream()
                .filter(t -> t.getTradeId() == 1L && t.getVersion() == 2)
                .findFirst()
                .orElseThrow();

        Transaction itcV1 = allTransactions.stream()
                .filter(t -> t.getTradeId() == 2L && t.getVersion() == 1)
                .findFirst()
                .orElseThrow();

        // Verify latest version flags
        assertFalse(relV1.getIsLatestVersion()); // Version 1 is not latest for trade 1
        assertTrue(relV2.getIsLatestVersion());  // Version 2 is latest for trade 1
        assertTrue(itcV1.getIsLatestVersion());  // Version 1 is latest for trade 2 (only transaction)
    }
} 