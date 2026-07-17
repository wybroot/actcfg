package com.example.delivery.packagebuild;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PackageBuildService {

    public List<PackageBuildEntity> listPackages() {
        return List.of(new PackageBuildEntity(
                1L,
                "PKG202607170001",
                1L,
                1L,
                1L,
                "1.0.0",
                PackageBuildStatus.SUCCESS,
                true,
                "packages/PKG202607170001.zip",
                "sha256-placeholder",
                LocalDateTime.now()
        ));
    }

    public PackageManifest getManifest(Long packageBuildId) {
        String manifestJson = "{\"packageCode\":\"PKG202607170001\",\"environment\":\"PROD\",\"steps\":[\"CHECK_ENV\",\"DEPLOY\",\"HEALTH_CHECK\"]}";
        return new PackageManifest(packageBuildId, manifestJson, "sha256-placeholder");
    }
}
