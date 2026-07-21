package com.example.delivery.repository.harbor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Harbor 镜像仓库连接配置。未配置 baseUrl 时，同步接口退化为按传入坐标直接登记（不校验 digest）。
 */
@ConfigurationProperties(prefix = "app.harbor")
public record HarborProperties(
        String baseUrl,
        String username,
        String password
) {
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
