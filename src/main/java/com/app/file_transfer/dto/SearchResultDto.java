package com.app.file_transfer.dto;

import java.time.LocalDateTime;

public record SearchResultDto(
    Long id,
    String name,
    String type,
    long size,
    LocalDateTime modifiedDate,
    String path,
    boolean isFolder
) {}
