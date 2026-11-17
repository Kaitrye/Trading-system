package ru.trading.system.position;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.position.dto.PositionResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;

    @Transactional(readOnly = true)
    public List<PositionResponse> findForUser(UUID userId) {
        return positionRepository.findByAccountUserIdOrderByIdSeccodeAsc(userId).stream()
                .map(position -> new PositionResponse(
                        position.getAccount().getId(),
                        position.getInstrument().getSeccode(),
                        position.getInstrument().getName(),
                        position.getQuantity(),
                        position.getReservedQuantity(),
                        position.getQuantity() - position.getReservedQuantity()
                ))
                .toList();
    }
}
