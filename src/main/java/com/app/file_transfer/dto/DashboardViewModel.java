package com.app.file_transfer.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardViewModel(
        DashboardMetricsDto metrics,
        List<ChartPointDto> storageBreakdown,
        List<RecentFileDto> recentFiles,
        LocalDateTime lastUpdated
) {
}
