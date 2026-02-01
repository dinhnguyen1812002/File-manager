package com.app.file_transfer.services;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.Folder;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.FolderRepository;
import com.app.file_transfer.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    // Get all files uploaded by a specific user
    public List<File> getFilesUploadedByUser(String username) {
        User user = userRepository.findByUsername(username);
        return fileRepository.findByUploader(user);
    }

    // Get all files shared with a specific user
    public List<File> getFilesReceivedByUser(String username) {
        User user = userRepository.findByUsername(username);
        return fileRepository.findByRecipientsContains(user);
    }

    // Set or update password for a file
    public void setFilePassword(Long fileId, String password, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        User user = userRepository.findByUsername(username);

        if (!file.getUploader().equals(user)) {
            throw new SecurityException("User does not have permission to set password for this file.");
        }

        file.setPassword(passwordEncoder.encode(password));
        fileRepository.save(file);
    }

    // Verify file password
    public boolean verifyFilePassword(Long fileId, String password) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        if (file.getPassword() == null || file.getPassword().isEmpty()) {
            return true; // No password set
        }

        return passwordEncoder.matches(password, file.getPassword());
    }

    // Share a file with multiple users
    public void shareFile(Long fileId, List<String> recipientUsernames, String senderUsername) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        User sender = userRepository.findByUsername(senderUsername);

        if (!file.getUploader().equals(sender)) {
            throw new SecurityException("User does not have permission to share this file.");
        }

        for (String username : recipientUsernames) {
            User recipient = userRepository.findByUsername(username);
            if (recipient != null && !file.getRecipients().contains(recipient)) {
                file.getRecipients().add(recipient);
                recipient.getReceivedFiles().add(file);
                userRepository.save(recipient);
            }
        }
        fileRepository.save(file);
    }

    // Soft delete a file
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        User user = userRepository.findByUsername(username);

        if (!file.getUploader().equals(user)) {
            throw new SecurityException("User does not have permission to delete this file.");
        }

        file.setDeleted(true);
        fileRepository.save(file);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long deleteFileAndGetParentId(Long fileId, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        User user = userRepository.findByUsername(username);

        if (!file.getUploader().equals(user)) {
            throw new SecurityException("User does not have permission to delete this file.");
        }

        Long folderId = file.getFolder() != null ? file.getFolder().getId() : null;
        file.setDeleted(true);
        fileRepository.save(file);
        return folderId;
    }

    // Restore a soft-deleted file
    @Transactional(rollbackFor = Exception.class)
    public void restoreFile(Long fileId, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        User user = userRepository.findByUsername(username);

        if (!file.getUploader().equals(user)) {
            throw new SecurityException("User does not have permission to restore this file.");
        }

        file.setDeleted(false);
        fileRepository.save(file);
    }

    // Permanently delete a file
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteFile(Long fileId, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        User user = userRepository.findByUsername(username);

        if (!file.getUploader().equals(user)) {
            throw new SecurityException("User does not have permission to permanently delete this file.");
        }

        String fileName = file.getFileName();

        // Remove file from recipients
        List<User> recipients = new ArrayList<>(file.getRecipients());
        for (User recipient : recipients) {
            recipient.getReceivedFiles().remove(file);
            file.getRecipients().remove(recipient);
            userRepository.save(recipient);
        }

        // Remove from folder
        if (file.getFolder() != null) {
            file.getFolder().getFiles().remove(file);
        }

        fileRepository.delete(file);
        fileRepository.flush();
        fileStorageService.deleteFile(fileName);
    }

    // Delete multiple files (soft delete)
    @Transactional(rollbackFor = Exception.class)
    public void deleteFiles(List<Long> fileIds, String username) {
        for (Long fileId : fileIds) {
            deleteFile(fileId, username);
        }
    }

    // Move file to another folder or to root
    @Transactional
    public void moveFile(Long fileId, Long targetFolderId, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        User user = userRepository.findByUsername(username);

        // Check if user has permission to move this file
        if (!file.getUploader().equals(user)) {
            throw new SecurityException("User does not have permission to move this file.");
        }

        Folder targetFolder = null;
        if (targetFolderId != null) {
            targetFolder = folderRepository.findById(targetFolderId)
                    .orElseThrow(() -> new IllegalArgumentException("Target folder not found"));

            // Check if user owns the target folder
            if (!targetFolder.getUser().equals(user)) {
                throw new SecurityException("User does not have permission to move file to this folder.");
            }
        }

        // Remove file from current folder if it's in one
        if (file.getFolder() != null) {
            file.getFolder().getFiles().remove(file);
        }

        // Set new folder (null means root)
        file.setFolder(targetFolder);

        // Add file to target folder if not null
        if (targetFolder != null) {
            targetFolder.getFiles().add(file);
        }

        fileRepository.save(file);
    }

    // Empty trash for a user
    @Transactional(rollbackFor = Exception.class)
    public void emptyTrash(String username) {
        User user = userRepository.findByUsername(username);
        List<File> deletedFiles = fileRepository.findByUploaderAndDeletedTrue(user);
        for (File file : deletedFiles) {
            permanentlyDeleteFile(file.getId(), username);
        }
    }
}
