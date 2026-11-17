package ru.trading.system.instrument;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.instrument.dto.InstrumentResponse;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public List<InstrumentResponse> findAll() {
        return instrumentRepository.findByActiveTrueOrderBySeccodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public InstrumentResponse findByTicker(String ticker) {
        String seccode = ticker.trim().toUpperCase(Locale.ROOT);

        return instrumentRepository.findBySeccodeAndActiveTrue(seccode)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
    }

    private InstrumentResponse toResponse(Instrument instrument) {
        return new InstrumentResponse(
                instrument.getSeccode(),
                instrument.getName(),
                instrument.getLotSize(),
                instrument.getMinStep(),
                instrument.getCurrency()
        );
    }
}
