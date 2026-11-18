package ru.trading.system.instrument;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.instrument.dto.CreateInstrumentRequest;
import ru.trading.system.instrument.dto.InstrumentResponse;

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
    return instrumentRepository
        .findBySeccodeAndActiveTrue(seccode)
        .map(this::toResponse)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
  }

  @Transactional
  public InstrumentResponse create(CreateInstrumentRequest request) {
    String ticker = request.ticker().trim().toUpperCase(Locale.ROOT);
    if (instrumentRepository.existsById(ticker)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrument already exists");
    }

    String currency = normalizeCurrency(request.currency());
    String baseCurrency = nullableCurrency(request.baseCurrency());
    String quoteCurrency = nullableCurrency(request.quoteCurrency());
    validateTypeSpecificDetails(request, currency, baseCurrency, quoteCurrency);

    Instrument instrument =
        new Instrument(
            ticker,
            request.name().trim(),
            request.lotSize(),
            request.minStep(),
            currency,
            request.type(),
            request.nominal(),
            request.maturityDate(),
            request.couponRate(),
            request.couponFrequencyMonths(),
            request.nextCouponDate(),
            baseCurrency,
            quoteCurrency);
    return toResponse(instrumentRepository.save(instrument));
  }

  @Transactional
  public InstrumentResponse changeStatus(String ticker, boolean active) {
    Instrument instrument =
        instrumentRepository
            .findById(ticker.trim().toUpperCase(Locale.ROOT))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
    instrument.changeStatus(active);
    return toResponse(instrument);
  }

  private void validateTypeSpecificDetails(
      CreateInstrumentRequest request, String currency, String baseCurrency, String quoteCurrency) {
    boolean hasBondDetails =
        request.nominal() != null
            || request.maturityDate() != null
            || request.couponRate() != null
            || request.couponFrequencyMonths() != null
            || request.nextCouponDate() != null;
    boolean hasCurrencyPairDetails = baseCurrency != null || quoteCurrency != null;

    if (request.type() == InstrumentType.STOCK) {
      if (hasBondDetails || hasCurrencyPairDetails) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Stock instrument must not contain type-specific details");
      }
      return;
    }

    if (request.type() == InstrumentType.BOND) {
      if (hasCurrencyPairDetails) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Bond instrument must not contain currency pair details");
      }
      if (!hasBondDetails
          || request.nominal() == null
          || request.maturityDate() == null
          || request.couponRate() == null
          || request.couponFrequencyMonths() == null
          || request.nextCouponDate() == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Bond details are required for bond instruments");
      }
      if (request.nextCouponDate().isAfter(request.maturityDate())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Coupon date must not be after maturity date");
      }
      return;
    }

    if (hasBondDetails) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Currency pair must not contain bond details");
    }
    if (baseCurrency == null || quoteCurrency == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Base and quote currencies are required for currency pair");
    }
    if (baseCurrency.equals(quoteCurrency)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Base and quote currencies must be different");
    }
    if (!currency.equals(quoteCurrency)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Instrument currency must match quote currency");
    }
  }

  private String normalizeCurrency(String currency) {
    return currency.trim().toUpperCase(Locale.ROOT);
  }

  private String nullableCurrency(String currency) {
    return currency == null ? null : normalizeCurrency(currency);
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
        instrument.getNextCouponDate(),
        instrument.getBaseCurrency(),
        instrument.getQuoteCurrency());
  }
}
