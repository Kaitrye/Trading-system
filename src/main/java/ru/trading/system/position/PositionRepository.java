package ru.trading.system.position;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, PositionId> {

    List<Position> findByAccountUserIdOrderByIdSeccodeAsc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Position p
            join fetch p.account
            where p.instrument.seccode = :ticker and p.quantity > 0
            """)
    List<Position> lockPositivePositionsByInstrument(@Param("ticker") String ticker);
}
