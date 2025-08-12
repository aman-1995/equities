package com.equities.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = true)
    private Long transactionId;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "security_code", nullable = false)
    private String securityCode;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionSide side;

    public enum TransactionAction {
        INSERT, UPDATE, CANCEL
    }

    public enum TransactionSide {
        BUY, SELL
    }
} 