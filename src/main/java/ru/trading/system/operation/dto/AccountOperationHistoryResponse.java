package ru.trading.system.operation.dto;

import java.util.List;

public record AccountOperationHistoryResponse(
        List<AccountOperationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
