package com.app.file_transfer.dto;

import java.time.LocalDateTime;

public record RecentFileDto(
        Long id,
        String fileName,
        String fileType,
        String sizeLabel,
        LocalDateTime createdAt
) {
}
