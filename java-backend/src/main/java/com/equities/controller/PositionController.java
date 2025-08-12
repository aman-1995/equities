package com.equities.controller;

import com.equities.model.Position;
import com.equities.model.ProcessingState;
import com.equities.model.Transaction;
import com.equities.service.PositionCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class PositionController {

    private final PositionCalculationService positionCalculationService;

    @GetMapping("/positions")
    public ResponseEntity<List<Position>> getAllPositions() {
        List<Position> positions = positionCalculationService.getAllPositions();
        return ResponseEntity.ok(positions);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = positionCalculationService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/transaction")
    public ResponseEntity<List<Position>> processTransaction(@RequestBody Transaction transaction) {
        List<Position> positions = positionCalculationService.processTransaction(transaction);
        return ResponseEntity.ok(positions);
    }

    @PostMapping("/transactions/bulk")
    public ResponseEntity<List<Position>> processBulkTransactions(@RequestBody List<Transaction> transactions) {
        List<Position> positions = positionCalculationService.processBulkTransactions(transactions);
        return ResponseEntity.ok(positions);
    }



    @PostMapping("/load-sample-data")
    public ResponseEntity<List<Position>> loadSampleData() {
        log.info("Loading sample data via REST API");
        List<Position> positions = positionCalculationService.loadSampleData();
        return ResponseEntity.ok(positions);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAllData() {
        log.info("Clearing all data via REST API");
        positionCalculationService.clearAllData();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/processing-state")
    public ResponseEntity<ProcessingState> getProcessingState() {
        ProcessingState state = positionCalculationService.getProcessingState();
        return ResponseEntity.ok(state);
    }

    @PostMapping("/force-recalculation")
    public ResponseEntity<List<Position>> forceFullRecalculation() {
        log.info("Forcing full recalculation via REST API");
        List<Position> positions = positionCalculationService.forceFullRecalculation();
        return ResponseEntity.ok(positions);
    }
} 