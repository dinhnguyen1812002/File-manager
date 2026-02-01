package com.app.file_transfer.dto;

public record DashboardMetricsDto(
        long totalFiles,
        long totalFolders,
        long sharedFiles,
        String storageUsed,
        String storageLimit,
        double storageUsagePercentage
) {
}
