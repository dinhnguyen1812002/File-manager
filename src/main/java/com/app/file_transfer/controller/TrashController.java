package com.app.file_transfer.controller;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.Folder;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.FolderRepository;
import com.app.file_transfer.repository.UserRepository;
import com.app.file_transfer.services.DashboardService;
import com.app.file_transfer.services.FileService;
import com.app.file_transfer.services.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class TrashController {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FolderService folderService;

    @GetMapping("/files/trash")
    public String showTrash(@AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername());

        List<Folder> deletedFolders = folderRepository.findByUserAndDeletedTrue(user);
        List<File> deletedFiles = fileRepository.findByUploaderAndDeletedTrue(user);

        List<Folder> allFolders = folderRepository.findByUser(user);
        model.addAttribute("subFolders", deletedFolders);
        model.addAttribute("files", deletedFiles);
        model.addAttribute("folders", allFolders);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("activePage", "Trash");
        model.addAttribute("dashboardViewModel", dashboardService.buildDashboard(user.getUsername()));

        return "trash";
    }

    @PostMapping("/files/trash/restore-file")
    public String restoreFile(@RequestParam Long fileId, @AuthenticationPrincipal UserDetails userDetails) {
        fileService.restoreFile(fileId, userDetails.getUsername());
        return "redirect:/files/trash";
    }

    @PostMapping("/files/trash/restore-folder")
    public String restoreFolder(@RequestParam Long folderId, @AuthenticationPrincipal UserDetails userDetails) {
        folderService.restoreFolder(folderId, userDetails.getUsername());
        return "redirect:/files/trash";
    }

    @PostMapping("/files/trash/permanent-delete-file")
    public String permanentlyDeleteFile(@RequestParam Long fileId, @AuthenticationPrincipal UserDetails userDetails) {
        fileService.permanentlyDeleteFile(fileId, userDetails.getUsername());
        return "redirect:/files/trash";
    }

    @PostMapping("/files/trash/permanent-delete-folder")
    public String permanentlyDeleteFolder(@RequestParam Long folderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        folderService.permanentlyDeleteFolder(folderId, userDetails.getUsername());
        return "redirect:/files/trash";
    }

    @PostMapping("/files/trash/empty")
    public String emptyTrash(@AuthenticationPrincipal UserDetails userDetails) {
        fileService.emptyTrash(userDetails.getUsername());
        folderService.emptyTrash(userDetails.getUsername());
        return "redirect:/files/trash";
    }
}
