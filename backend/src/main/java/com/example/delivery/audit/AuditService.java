package com.example.delivery.audit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务，双路径：local profile 落库，dev 内存记录。
 */
@Service
public class AuditService {

    private final AuditRepository auditRepository;
    private final AtomicLong opSeq = new AtomicLong(1);
    private final AtomicLong dlSeq = new AtomicLong(1);
    private final ConcurrentLinkedDeque<OperationLogEntity> operationLogs = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<DownloadLogEntity> downloadLogs = new ConcurrentLinkedDeque<>();

    @Autowired
    public AuditService(ObjectProvider<AuditRepository> auditRepositoryProvider) {
        this.auditRepository = auditRepositoryProvider.getIfAvailable();
    }

    public AuditService() {
        this.auditRepository = null;
    }

    private boolean useJdbc() {
        return auditRepository != null;
    }

    public List<OperationLogEntity> listOperationLogs() {
        if (useJdbc()) {
            return auditRepository.findOperationLogs();
        }
        return operationLogs.stream().toList();
    }

    public List<DownloadLogEntity> listDownloadLogs() {
        if (useJdbc()) {
            return auditRepository.findDownloadLogs();
        }
        return downloadLogs.stream().toList();
    }

    public void recordOperation(String operatorName, String module, String action,
                                String result, String ip, String paramSummary) {
        if (useJdbc()) {
            auditRepository.insertOperationLog(operatorName, module, action, result, ip, paramSummary);
            return;
        }
        operationLogs.addFirst(new OperationLogEntity(
                opSeq.getAndIncrement(), operatorName, module, action, result, LocalDateTime.now()));
    }

    public void recordDownload(String downloaderName, String targetType, String targetName,
                               Long customerId, Long environmentId, Long fileSize, String ip) {
        if (useJdbc()) {
            auditRepository.insertDownloadLog(downloaderName, targetType, targetName,
                    customerId, environmentId, fileSize, ip);
            return;
        }
        downloadLogs.addFirst(new DownloadLogEntity(
                dlSeq.getAndIncrement(), downloaderName, targetType, targetName, ip, LocalDateTime.now()));
    }
}
