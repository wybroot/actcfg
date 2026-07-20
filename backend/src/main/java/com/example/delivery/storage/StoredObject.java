package com.example.delivery.storage;

public record StoredObject(
        String bucket,
        String objectName,
        String objectUrl,
        String checksum
) {
}
