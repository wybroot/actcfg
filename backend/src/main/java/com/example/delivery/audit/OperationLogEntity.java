package com.example.delivery.audit;

import java.time.LocalDateTime;

public record OperationLogEntity(Long id, String operatorName, String module, String action, String result, LocalDateTime createdAt) {
}
