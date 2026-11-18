package ru.trading.system.position;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.account.Account;
import ru.trading.system.account.AccountRepository;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.instrument.InstrumentType;
import ru.trading.system.position.dto.PositionResponse;

@Service
@RequiredArgsConstructor
public class PositionService {

  private final PositionRepository positionRepository;
  private final AccountRepository accountRepository;
  private final InstrumentRepository instrumentRepository;

  @Transactional(readOnly = true)
  public List<PositionResponse> findForUser(UUID userId) {
    return positionRepository.findByAccountUserIdOrderByIdSeccodeAsc(userId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public PositionResponse grant(UUID accountId, String ticker, long quantity) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    Instrument instrument =
        instrumentRepository
            .findById(ticker.trim().toUpperCase(Locale.ROOT))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
    if (!instrument.isActive()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrument is not active");
    }
    if (instrument.getType() == InstrumentType.CURRENCY_PAIR) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Currency pairs must be credited to an account");
    }
    if (quantity % instrument.getLotSize() != 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Quantity must be a multiple of lot size");
    }

    Position position =
        positionRepository
            .findById(new PositionId(account.getId(), instrument.getSeccode()))
            .orElseGet(() -> positionRepository.save(new Position(account, instrument)));
    position.credit(quantity);
    return toResponse(position);
  }

  private PositionResponse toResponse(Position position) {
    return new PositionResponse(
        position.getAccount().getId(),
        position.getInstrument().getSeccode(),
        position.getInstrument().getName(),
        position.getQuantity(),
        position.getReservedQuantity(),
        position.getQuantity() - position.getReservedQuantity());
  }
}
