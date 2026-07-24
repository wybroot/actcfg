package com.example.delivery.security.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.Map;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class SecretCipherTests {

    private static String key() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        return Base64.getEncoder().encodeToString(raw);
    }

    @Test
    void encryptDecryptRoundtrip() {
        SecretCipher cipher = new SecretCipher(null); // 进程内临时密钥
        String enc = cipher.encrypt("secret123");

        assertTrue(cipher.isEncrypted(enc));
        assertNotEquals("secret123", enc);
        assertEquals("secret123", cipher.decrypt(enc));
    }

    @Test
    void plainValuePassesThroughDecrypt() {
        SecretCipher cipher = new SecretCipher(null);
        // 历史明文（无 enc: 前缀）解密时原样返回，保证向后兼容
        assertFalse(cipher.isEncrypted("plainValue"));
        assertEquals("plainValue", cipher.decrypt("plainValue"));
    }

    @Test
    void emptyAndNullUnchanged() {
        SecretCipher cipher = new SecretCipher(null);
        assertNull(cipher.encrypt(null));
        assertEquals("", cipher.encrypt(""));
    }

    @Test
    void rotationReencryptsWithActiveKey() {
        String k1 = key();
        String k2 = key();
        // 先用 v1 加密
        SecretCipher v1 = new SecretCipher(new EncryptionProperties("v1", Map.of("v1", k1)));
        String encV1 = v1.encrypt("topsecret");

        // 轮换到 v2（保留 v1 用于解密历史密文）
        SecretCipher v2 = new SecretCipher(new EncryptionProperties("v2", Map.of("v1", k1, "v2", k2)));
        String rotated = v2.rotate(encV1);

        // 轮换后密文用 v2，且能解出原文
        assertNotEquals(encV1, rotated);
        assertTrue(rotated.startsWith("enc:v2:"));
        assertEquals("topsecret", v2.decrypt(rotated));
    }

    @Test
    void rotationNoopWhenAlreadyActiveKey() {
        String k1 = key();
        SecretCipher v1 = new SecretCipher(new EncryptionProperties("v1", Map.of("v1", k1)));
        String enc = v1.encrypt("x");
        // 已是活跃密钥，rotate 返回 null 表示无需变更
        assertNull(v1.rotate(enc));
    }
}
