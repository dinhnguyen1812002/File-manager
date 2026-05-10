package com.app.file_transfer.repository;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.User;
import com.app.file_transfer.model.Folder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;

public interface FileRepository extends JpaRepository<File, Long> {

    @Query("SELECT f FROM File f WHERE f.uploader = :user AND f.deleted = false " +
           "AND (LOWER(CAST(f.fileName as string)) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (CAST(:type AS string) IS NULL OR LOWER(CAST(f.fileType as string)) = LOWER(CAST(:type as string))) " +
           "AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR f.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR f.createdAt <= :endDate)")
    Page<File> searchFiles(
        @Param("user") User user, 
        @Param("query") String query, 
        @Param("type") String type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT f FROM File f WHERE f.uploader = :user AND f.deleted = false " +
           "AND LOWER(CAST(f.fileName as string)) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<File> findTop5ByUploaderAndFileNameContainingIgnoreCaseAndDeletedFalse(
        @Param("user") User user, 
        @Param("query") String query,
        Pageable pageable
    );
    // Soft-delete methods
    List<File> findByUploaderAndDeletedFalse(User uploader);

    List<File> findByRecipientsContainsAndDeletedFalse(User user);

    List<File> findByUploaderAndFolderIsNullAndDeletedFalse(User uploader);

    List<File> findByUploaderAndFolderAndDeletedFalse(User uploader, Folder folder);

    List<File> findByUploaderAndDeletedTrue(User uploader);

    // Old methods (for backward compatibility / internal use)
    List<File> findByUploader(User uploader);

    List<File> findByRecipientsContains(User user);

    List<File> findByUploaderAndFolderIsNull(User uploader);

    List<File> findByUploaderAndFolder(User uploader, Folder folder);

    List<File> findByUploaderAndFolderOrderByCreatedAtDesc(User uploader, Folder folder);

    List<File> findByUploaderAndFolderIsNullOrderByCreatedAtDesc(User uploader);
}