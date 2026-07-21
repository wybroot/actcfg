package com.example.delivery.security.crypto;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * 敏感配置加解密（AES-256-GCM）。支持密钥轮换：
 * 密文自带密钥版本前缀，格式 {@code enc:<keyId>:<ivBase64>:<cipherBase64>}，
 * 解密时按前缀选择对应历史密钥，加密始终用当前活跃密钥。
 */
@Component
public class SecretCipher {
    private static final String PREFIX = "enc:";
    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, SecretKeySpec> keys = new HashMap<>();
    private final String activeKeyId;

    public SecretCipher(EncryptionProperties props) {
        if (props != null && props.activeKeyId() != null && props.keys() != null
                && props.keys().containsKey(props.activeKeyId())) {
            props.keys().forEach((id, b64) -> keys.put(id, toKey(Base64.getDecoder().decode(b64))));
            this.activeKeyId = props.activeKeyId();
        } else {
            // 未配置：生成进程内临时密钥（dev/测试用，重启失效）
            byte[] ephemeral = new byte[32];
            new SecureRandom().nextBytes(ephemeral);
            this.activeKeyId = "dev";
            keys.put("dev", toKey(ephemeral));
        }
    }

    /** 当前活跃密钥版本 ID。 */
    public String activeKeyId() {
        return activeKeyId;
    }

    /** 是否为已加密密文（enc: 前缀）。 */
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** 用活跃密钥加密明文，返回自带密钥版本的密文；null/空原样返回。 */
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeKeyId), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return PREFIX + activeKeyId + ":"
                    + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(ct);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "敏感配置加密失败");
        }
    }

    /** 解密密文；非 enc: 前缀（历史明文）原样返回，保证向后兼容。 */
    public String decrypt(String stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        try {
            String[] parts = stored.substring(PREFIX.length()).split(":", 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("密文格式非法");
            }
            SecretKeySpec key = keys.get(parts[0]);
            if (key == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "缺少密钥版本 " + parts[0] + "，无法解密");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ct = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "敏感配置解密失败");
        }
    }

    /**
     * 轮换：若密文不是用活跃密钥加密的，则解密后用活跃密钥重新加密；已是活跃密钥则原样返回。
     * @return 重新加密后的密文，或 null 表示无需变更
     */
    public String rotate(String stored) {
        if (!isEncrypted(stored)) {
            return null;
        }
        String usedKeyId = stored.substring(PREFIX.length()).split(":", 3)[0];
        if (activeKeyId.equals(usedKeyId)) {
            return null;
        }
        return encrypt(decrypt(stored));
    }

    private SecretKeySpec toKey(byte[] raw) {
        if (raw.length != 32) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "加密密钥必须为 32 字节（AES-256）");
        }
        return new SecretKeySpec(raw, "AES");
    }
}
