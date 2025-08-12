package com.equities.repository;

import com.equities.model.ProcessingState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessingStateRepository extends JpaRepository<ProcessingState, Long> {

    Optional<ProcessingState> findByStateKey(String stateKey);
}
