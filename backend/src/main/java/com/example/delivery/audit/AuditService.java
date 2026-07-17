package com.example.delivery.audit;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    public List<OperationLogEntity> listOperationLogs() {
        return List.of(new OperationLogEntity(1L, "admin", "PACKAGE", "BUILD", "SUCCESS", LocalDateTime.now()));
    }

    public List<DownloadLogEntity> listDownloadLogs() {
        return List.of(new DownloadLogEntity(1L, "admin", "PACKAGE", "PKG202607170001", "127.0.0.1", LocalDateTime.now()));
    }
}
