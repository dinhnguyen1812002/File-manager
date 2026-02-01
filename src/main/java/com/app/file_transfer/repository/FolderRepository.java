package com.app.file_transfer.repository;

import com.app.file_transfer.model.Folder;
import com.app.file_transfer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    // Soft-delete methods
    List<Folder> findByUserAndParentAndDeletedFalse(User user, Folder parent);

    List<Folder> findByUserAndDeletedFalse(User user);

    List<Folder> findByUserAndDeletedTrue(User user);

    // Old methods (for backward compatibility)
    List<Folder> findByUserAndParent(User user, Folder parent);

    List<Folder> findByUser(User user);
}