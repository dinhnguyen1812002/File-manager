package com.app.file_transfer.repository;

import com.app.file_transfer.model.Folder;
import com.app.file_transfer.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.deleted = false " +
           "AND (LOWER(CAST(f.name as string)) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR f.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR f.createdAt <= :endDate)")
    Page<Folder> searchFolders(
        @Param("user") User user, 
        @Param("query") String query, 
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.deleted = false " +
           "AND LOWER(CAST(f.name as string)) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Folder> findTop5ByUserAndNameContainingIgnoreCaseAndDeletedFalse(
        @Param("user") User user, 
        @Param("query") String query,
        Pageable pageable
    );
    // Soft-delete methods
    List<Folder> findByUserAndParentAndDeletedFalse(User user, Folder parent);

    List<Folder> findByUserAndDeletedFalse(User user);

    List<Folder> findByUserAndDeletedTrue(User user);

    // Old methods (for backward compatibility)
    List<Folder> findByUserAndParent(User user, Folder parent);

    List<Folder> findByUser(User user);
}