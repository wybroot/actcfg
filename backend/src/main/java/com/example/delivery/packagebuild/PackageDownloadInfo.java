package com.example.delivery.packagebuild;

public record PackageDownloadInfo(
        Long packageBuildId,
        String packageCode,
        String filePath,
        String checksum,
        String manifestJson
) {
}
