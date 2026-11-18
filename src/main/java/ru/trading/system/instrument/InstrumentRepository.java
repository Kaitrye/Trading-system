package ru.trading.system.instrument;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstrumentRepository extends JpaRepository<Instrument, String> {

  List<Instrument> findByActiveTrueOrderBySeccodeAsc();

  Optional<Instrument> findBySeccodeAndActiveTrue(String seccode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select i from Instrument i
            where i.type = :type
              and i.active = true
              and (i.nextCouponDate <= :processingDate or i.maturityDate <= :processingDate)
            """)
  List<Instrument> lockProcessableBonds(
      @Param("type") InstrumentType type, @Param("processingDate") LocalDate processingDate);
}
