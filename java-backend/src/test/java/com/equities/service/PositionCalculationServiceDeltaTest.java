package com.equities.service;

import com.equities.model.Position;
import com.equities.model.ProcessingState;
import com.equities.model.Transaction;
import com.equities.repository.PositionRepository;
import com.equities.repository.ProcessingStateRepository;
import com.equities.repository.TransactionRepository;
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
class PositionCalculationServiceDeltaTest {

    @Autowired
    private PositionCalculationService positionCalculationService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private ProcessingStateRepository processingStateRepository;

    @BeforeEach
    void setUp() {
        positionCalculationService.clearAllData();
    }

    @Test
    void testDeltaProcessing_InitialLoad() {
        // Initial transactions
        List<Transaction> initialTransactions = List.of(
                Transaction.builder().tradeId(1L).version(1).securityCode("REL").quantity(50).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().tradeId(2L).version(1).securityCode("ITC").quantity(40).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.SELL).build()
        );

        List<Position> positions = positionCalculationService.processBulkTransactions(initialTransactions);
        
        assertEquals(2, positions.size());
        assertTrue(positions.stream().anyMatch(p -> p.getSecurityCode().equals("REL") && p.getQuantity() == 50));
        assertTrue(positions.stream().anyMatch(p -> p.getSecurityCode().equals("ITC") && p.getQuantity() == -40));

        // Verify processing state was updated
        ProcessingState state = positionCalculationService.getProcessingState();
        assertNotNull(state);
        assertTrue(state.getLastProcessedTransactionId() > 0);
    }

    @Test
    void testDeltaProcessing_NewTransactions() {
        // Initial transactions
        List<Transaction> initialTransactions = List.of(
                Transaction.builder().tradeId(1L).version(1).securityCode("REL").quantity(50).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.BUY).build()
        );
        positionCalculationService.processBulkTransactions(initialTransactions);

        // New transactions (delta)
        List<Transaction> newTransactions = List.of(
                Transaction.builder().tradeId(2L).version(1).securityCode("ITC").quantity(30).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.BUY).build(),
                Transaction.builder().tradeId(1L).version(2).securityCode("REL").quantity(60).action(Transaction.TransactionAction.UPDATE).side(Transaction.TransactionSide.BUY).build()
        );

        List<Position> positions = positionCalculationService.processBulkTransactions(newTransactions);
        
        assertEquals(2, positions.size());
        assertTrue(positions.stream().anyMatch(p -> p.getSecurityCode().equals("REL") && p.getQuantity() == 60));
        assertTrue(positions.stream().anyMatch(p -> p.getSecurityCode().equals("ITC") && p.getQuantity() == 30));

        // Verify processing state was updated
        ProcessingState state = positionCalculationService.getProcessingState();
        assertTrue(state.getLastProcessedTransactionId() > 0);
    }

    @Test
    void testDeltaProcessing_NoNewTransactions() {
        // Initial transactions
        List<Transaction> initialTransactions = List.of(
                Transaction.builder().tradeId(1L).version(1).securityCode("REL").quantity(50).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.BUY).build()
        );
        List<Position> initialPositions = positionCalculationService.processBulkTransactions(initialTransactions);

        // Process again with no new transactions
        List<Position> positions = positionCalculationService.processBulkTransactions(List.of());
        
        assertEquals(initialPositions.size(), positions.size());
        assertEquals(initialPositions, positions);
    }

    @Test
    void testForceFullRecalculation() {
        // Initial transactions
        List<Transaction> initialTransactions = List.of(
                Transaction.builder().tradeId(1L).version(1).securityCode("REL").quantity(50).action(Transaction.TransactionAction.INSERT).side(Transaction.TransactionSide.BUY).build()
        );
        positionCalculationService.processBulkTransactions(initialTransactions);

        // Force full recalculation
        List<Position> positions = positionCalculationService.forceFullRecalculation();
        
        assertEquals(1, positions.size());
        assertTrue(positions.stream().anyMatch(p -> p.getSecurityCode().equals("REL") && p.getQuantity() == 50));
    }
}
