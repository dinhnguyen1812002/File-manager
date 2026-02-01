package com.app.file_transfer.services;

import com.app.file_transfer.dto.ChartPointDto;
import com.app.file_transfer.dto.DashboardMetricsDto;
import com.app.file_transfer.dto.DashboardViewModel;
import com.app.file_transfer.dto.RecentFileDto;
import com.app.file_transfer.model.File;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.FolderRepository;
import com.app.file_transfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

@Service
public class DashboardService {

        private static final int RECENT_FILES_LIMIT = 6;

        @Autowired
        private FileRepository fileRepository;

        @Autowired
        private FolderRepository folderRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private StorageUsageService storageUsageService;

        public DashboardViewModel buildDashboard(String username) {
                User user = userRepository.findByUsername(username);
                if (user == null) {
                        throw new IllegalArgumentException("User not found");
                }

                StorageUsageService.StorageUsageStats stats = storageUsageService.getStorageUsageStats(username);
                long totalFolders = folderRepository.findByUserAndDeletedFalse(user).size();
                DashboardMetricsDto metrics = new DashboardMetricsDto(
                                stats.getTotalFiles(),
                                totalFolders,
                                stats.getSharedFiles(),
                                storageUsageService.formatBytes(stats.getTotalUsedBytes()),
                                storageUsageService.formatBytes(stats.getStorageLimitBytes()),
                                stats.getUsagePercentage());

                List<ChartPointDto> breakdown = storageUsageService.getStorageBreakdown(username)
                                .entrySet()
                                .stream()
                                .map(entry -> new ChartPointDto(entry.getKey(), entry.getValue()))
                                .toList();

                List<RecentFileDto> recentFiles = fileRepository.findByUploaderAndDeletedFalse(user)
                                .stream()
                                .sorted(Comparator
                                                .comparing(File::getCreatedAt,
                                                                Comparator.nullsLast(Comparator.naturalOrder()))
                                                .reversed())
                                .limit(RECENT_FILES_LIMIT)
                                .map(this::mapRecentFile)
                                .toList();

                return new DashboardViewModel(metrics, breakdown, recentFiles, LocalDateTime.now());
        }

        public DashboardMetricsDto getMetrics(String username) {
                StorageUsageService.StorageUsageStats stats = storageUsageService.getStorageUsageStats(username);
                User user = userRepository.findByUsername(username);
                long totalFolders = folderRepository.findByUserAndDeletedFalse(user).size();
                return new DashboardMetricsDto(
                                stats.getTotalFiles(),
                                totalFolders,
                                stats.getSharedFiles(),
                                storageUsageService.formatBytes(stats.getTotalUsedBytes()),
                                storageUsageService.formatBytes(stats.getStorageLimitBytes()),
                                stats.getUsagePercentage());
        }

        private RecentFileDto mapRecentFile(File file) {
                return new RecentFileDto(
                                file.getId(),
                                file.getFileName(),
                                file.getFileType(),
                                storageUsageService.formatBytes(file.getFileSize()),
                                file.getCreatedAt());
        }
}
