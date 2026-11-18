package ru.trading.system.instrument;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.instrument.dto.CreateInstrumentRequest;
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

    @Transactional
    public InstrumentResponse create(CreateInstrumentRequest request) {
        String ticker = request.ticker().trim().toUpperCase(Locale.ROOT);
        if (instrumentRepository.existsById(ticker)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrument already exists");
        }
        validateTypeSpecificDetails(request);

        Instrument instrument = new Instrument(
                ticker,
                request.name().trim(),
                request.lotSize(),
                request.minStep(),
                request.currency().trim().toUpperCase(Locale.ROOT),
                request.type(),
                request.nominal(),
                request.maturityDate(),
                request.couponRate(),
                request.couponFrequencyMonths(),
                request.nextCouponDate()
        );
        return toResponse(instrumentRepository.save(instrument));
    }

    @Transactional
    public InstrumentResponse changeStatus(String ticker, boolean active) {
        Instrument instrument = instrumentRepository.findById(ticker.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
        instrument.changeStatus(active);
        return toResponse(instrument);
    }

    private void validateTypeSpecificDetails(CreateInstrumentRequest request) {
        boolean hasBondDetails = request.nominal() != null
                || request.maturityDate() != null
                || request.couponRate() != null
                || request.couponFrequencyMonths() != null
                || request.nextCouponDate() != null;

        if (request.type() == InstrumentType.STOCK) {
            if (hasBondDetails) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock instrument must not contain bond details");
            }
            return;
        }

        if (!hasBondDetails
                || request.nominal() == null
                || request.maturityDate() == null
                || request.couponRate() == null
                || request.couponFrequencyMonths() == null
                || request.nextCouponDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bond details are required for bond instruments");
        }
        if (request.nextCouponDate().isAfter(request.maturityDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon date must not be after maturity date");
        }
    }

    private InstrumentResponse toResponse(Instrument instrument) {
        return new InstrumentResponse(
                instrument.getSeccode(),
                instrument.getName(),
                instrument.getLotSize(),
                instrument.getMinStep(),
                instrument.getCurrency(),
                instrument.getType(),
                instrument.getNominal(),
                instrument.getMaturityDate(),
                instrument.getCouponRate(),
                instrument.getCouponFrequencyMonths(),
                instrument.getNextCouponDate()
        );
    }
}
