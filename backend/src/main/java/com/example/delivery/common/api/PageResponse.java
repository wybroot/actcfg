package com.example.delivery.common.api;

import java.util.List;

public record PageResponse<T>(long total, int pageNo, int pageSize, List<T> records) {
}
