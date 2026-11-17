package ru.trading.system.position;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, PositionId> {

    List<Position> findByAccountUserIdOrderByIdSeccodeAsc(UUID userId);
}
