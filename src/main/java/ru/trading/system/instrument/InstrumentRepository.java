package ru.trading.system.instrument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, String> {

    List<Instrument> findByActiveTrueOrderBySeccodeAsc();

    Optional<Instrument> findBySeccodeAndActiveTrue(String seccode);
}
