package com.example.delivery.security.crypto;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 敏感配置加密密钥配置。支持多密钥并存以实现密钥轮换：
 * <pre>
 * app.encryption.active-key-id: v2          # 新数据使用的当前密钥
 * app.encryption.keys:
 *   v1: &lt;base64 32字节旧密钥&gt;              # 保留用于解密历史数据
 *   v2: &lt;base64 32字节新密钥&gt;
 * </pre>
 * 未配置时 {@link SecretCipher} 退化为进程内临时密钥（仅 dev/测试用，重启即失效）。
 */
@ConfigurationProperties(prefix = "app.encryption")
public record EncryptionProperties(
        String activeKeyId,
        Map<String, String> keys
) {
}
