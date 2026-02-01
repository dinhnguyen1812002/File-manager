package com.app.file_transfer.repository;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.User;
import com.app.file_transfer.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
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