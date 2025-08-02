package com.equities.repository;

import com.equities.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findBySecurityCode(String securityCode);

    List<Position> findAllByOrderBySecurityCodeAsc();
} 