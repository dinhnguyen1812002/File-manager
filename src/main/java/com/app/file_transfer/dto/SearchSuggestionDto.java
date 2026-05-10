package com.app.file_transfer.dto;

public record SearchSuggestionDto(
    Long id,
    String name,
    boolean isFolder,
    String type
) {}
