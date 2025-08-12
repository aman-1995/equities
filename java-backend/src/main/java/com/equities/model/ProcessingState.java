package com.equities.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processing_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_key", unique = true, nullable = false)
    private String stateKey;

    @Column(name = "last_processed_transaction_id")
    private Long lastProcessedTransactionId;

    @Column(name = "last_processed_timestamp")
    private java.time.LocalDateTime lastProcessedTimestamp;
}
