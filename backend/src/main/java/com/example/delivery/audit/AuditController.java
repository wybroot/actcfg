package com.example.delivery.audit;

import com.example.delivery.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/operation-logs")
    public ApiResponse<List<OperationLogEntity>> listOperationLogs() {
        return ApiResponse.ok(auditService.listOperationLogs());
    }

    @GetMapping("/download-logs")
    public ApiResponse<List<DownloadLogEntity>> listDownloadLogs() {
        return ApiResponse.ok(auditService.listDownloadLogs());
    }

    @GetMapping("/login-logs")
    public ApiResponse<List<LoginLogEntity>> listLoginLogs() {
        return ApiResponse.ok(auditService.listLoginLogs());
    }
}
