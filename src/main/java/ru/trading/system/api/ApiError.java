package ru.trading.system.api;

import java.time.Instant;
import java.util.Map;

public record ApiError(
    int status, String code, String message, Instant timestamp, Map<String, String> fieldErrors) {}
