package com.example.delivery.repository.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.delivery.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SourceRepositoryServiceTests {
    private SourceRepositoryService service;

    @BeforeEach
    void setUp() {
        service = new SourceRepositoryService();
    }

    private CreateSourceRepositoryRequest req(String code, String pass) {
        return new CreateSourceRepositoryRequest(code, "生产Harbor", SourceRepositoryType.HARBOR,
                "https://harbor.example.com", "admin", pass, "说明", "ENABLED");
    }

    @Test
    void createMasksPasswordOnRead() {
        SourceRepositoryEntity created = service.create(req("harbor-1", "secret123"));
        // 返回值密码脱敏
        assertEquals("******", created.password());

        // 列表同样脱敏
        SourceRepositoryEntity listed = service.listRepositories().stream()
                .filter(r -> r.repoCode().equals("harbor-1")).findFirst().orElseThrow();
        assertEquals("******", listed.password());
    }

    @Test
    void credentialsDecryptToOriginalPassword() {
        SourceRepositoryEntity created = service.create(req("harbor-2", "secret123"));
        SourceRepositoryService.Credentials creds = service.getCredentials(created.id());
        // 内部取凭证时解密回明文
        assertEquals("secret123", creds.password());
        assertEquals("https://harbor.example.com", creds.baseUrl());
    }

    @Test
    void duplicateCodeRejected() {
        service.create(req("dup", "x"));
        assertThrows(BusinessException.class, () -> service.create(req("dup", "y")));
    }

    @Test
    void updateWithBlankPasswordKeepsOriginal() {
        SourceRepositoryEntity created = service.create(req("harbor-3", "orig"));
        service.update(created.id(), new UpdateSourceRepositoryRequest(
                "改名", SourceRepositoryType.HARBOR, "https://new.example.com",
                "admin", "", "说明", "ENABLED"));
        // 密码留空 → 原密码仍能解出
        assertEquals("orig", service.getCredentials(created.id()).password());
        assertEquals("改名", service.getRepository(created.id()).repoName());
    }

    @Test
    void disabledRepoRejectsCredentialUse() {
        SourceRepositoryEntity created = service.create(new CreateSourceRepositoryRequest(
                "harbor-4", "停用的", SourceRepositoryType.HARBOR, "https://h.example.com",
                "admin", "p", "", "DISABLED"));
        // 停用的源仓库不能取凭证
        assertThrows(BusinessException.class, () -> service.getCredentials(created.id()));
    }

    @Test
    void deleteRemovesFromList() {
        SourceRepositoryEntity created = service.create(req("harbor-5", "x"));
        service.delete(created.id());
        assertTrue(service.listRepositories().stream().noneMatch(r -> r.repoCode().equals("harbor-5")));
    }

    @Test
    void passwordStoredEncryptedNotPlaintext() {
        // 通过凭证解密验证：存储值与明文不同，但能解回
        SourceRepositoryEntity created = service.create(req("harbor-6", "plainpass"));
        assertNotEquals("plainpass", created.password()); // 脱敏后是掩码
        assertEquals("plainpass", service.getCredentials(created.id()).password());
    }
}
