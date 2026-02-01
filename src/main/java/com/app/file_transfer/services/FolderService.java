package com.app.file_transfer.services;

import com.app.file_transfer.model.Folder;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FolderRepository;
import com.app.file_transfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FolderService {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Folder createFolder(String name, Long parentId, String username, String password) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name is required");
        }
        User user = userRepository.findByUsername(username);
        Folder parentFolder = null;
        if (parentId != null) {
            parentFolder = folderRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
            // Ensure the user owns the parent folder
            if (!parentFolder.getUser().equals(user)) {
                throw new SecurityException("User does not have permission to create a folder here.");
            }
        }

        Folder newFolder = new Folder();
        newFolder.setName(name.trim());
        newFolder.setUser(user);
        if (password != null && !password.trim().isEmpty()) {
            newFolder.setPassword(passwordEncoder.encode(password));
        } else {
            newFolder.setPassword(null);
        }
        newFolder.setParent(parentFolder);
        return folderRepository.save(newFolder);
    }

    public List<Folder> getSubFolders(Long parentId, String username) {
        User user = userRepository.findByUsername(username);
        Folder parentFolder = (parentId == null) ? null
                : folderRepository.findById(parentId)
                        .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));

        if (parentId != null && !parentFolder.getUser().equals(user)) {
            throw new SecurityException("User does not have permission to view this folder.");
        }

        return folderRepository.findByUserAndParentAndDeletedFalse(user, parentFolder);
    }

    // Set or update password for a folder
    public void setFolderPassword(Long folderId, String password, String username) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        User user = userRepository.findByUsername(username);

        if (!folder.getUser().equals(user)) {
            throw new SecurityException("User does not have permission to set password for this folder.");
        }

        if (password != null && !password.trim().isEmpty()) {
            folder.setPassword(passwordEncoder.encode(password));
        } else {
            folder.setPassword(null);
        }
        folderRepository.save(folder);
    }

    // Verify folder password
    public boolean verifyFolderPassword(Long folderId, String password) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        if (folder.getPassword() == null || folder.getPassword().isEmpty()) {
            return true; // No password set
        }

        return passwordEncoder.matches(password, folder.getPassword());
    }

    // Soft delete a folder
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long folderId, String username) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        User user = userRepository.findByUsername(username);

        if (!folder.getUser().equals(user)) {
            throw new SecurityException("User does not have permission to delete this folder.");
        }

        softDeleteFolderRecursive(folder);
    }

    private void softDeleteFolderRecursive(Folder folder) {
        folder.setDeleted(true);
        // Soft delete all subfolders
        for (Folder subFolder : folder.getSubFolders()) {
            softDeleteFolderRecursive(subFolder);
        }
        // Soft delete all files in this folder
        for (com.app.file_transfer.model.File file : folder.getFiles()) {
            file.setDeleted(true);
        }
        folderRepository.save(folder);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long deleteFolderAndGetParentId(Long folderId, String username) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        User user = userRepository.findByUsername(username);

        if (!folder.getUser().equals(user)) {
            throw new SecurityException("User does not have permission to delete this folder.");
        }

        Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
        softDeleteFolderRecursive(folder);
        return parentId;
    }

    // Restore a soft-deleted folder
    @Transactional(rollbackFor = Exception.class)
    public void restoreFolder(Long folderId, String username) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        User user = userRepository.findByUsername(username);

        if (!folder.getUser().equals(user)) {
            throw new SecurityException("User does not have permission to restore this folder.");
        }

        restoreFolderRecursive(folder);
    }

    private void restoreFolderRecursive(Folder folder) {
        folder.setDeleted(false);
        for (Folder subFolder : folder.getSubFolders()) {
            restoreFolderRecursive(subFolder);
        }
        for (com.app.file_transfer.model.File file : folder.getFiles()) {
            file.setDeleted(false);
        }
        folderRepository.save(folder);
    }

    // Permanently delete a folder
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteFolder(Long folderId, String username) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        User user = userRepository.findByUsername(username);

        if (!folder.getUser().equals(user)) {
            throw new SecurityException("User does not have permission to permanently delete this folder.");
        }

        // JPA Cascade will handle subfolders and files if configured,
        // but we need to ensure physical files are deleted too.
        deletePhysicalFilesRecursive(folder);
        folderRepository.delete(folder);
        folderRepository.flush();
    }

    @Autowired
    private FileStorageService fileStorageService;

    private void deletePhysicalFilesRecursive(Folder folder) {
        for (com.app.file_transfer.model.File file : folder.getFiles()) {
            fileStorageService.deleteFile(file.getFileName());
        }
        for (Folder subFolder : folder.getSubFolders()) {
            deletePhysicalFilesRecursive(subFolder);
        }
    }

    // Delete multiple folders (soft delete)
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolders(List<Long> folderIds, String username) {
        for (Long folderId : folderIds) {
            deleteFolder(folderId, username);
        }
    }

    // Empty trash for a user (permanently delete all soft-deleted folders)
    @Transactional(rollbackFor = Exception.class)
    public void emptyTrash(String username) {
        User user = userRepository.findByUsername(username);
        List<Folder> deletedFolders = folderRepository.findByUserAndDeletedTrue(user);
        for (Folder folder : deletedFolders) {
            // Check if it's a top-level deleted folder (parent is not deleted or is null)
            // This prevents redundant deletion attempts in the same transaction
            if (folder.getParent() == null || !folder.getParent().isDeleted()) {
                permanentlyDeleteFolder(folder.getId(), username);
            }
        }
    }
}
