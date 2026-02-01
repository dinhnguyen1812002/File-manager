package com.app.file_transfer.controller;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.Folder;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.FolderRepository;
import com.app.file_transfer.repository.UserRepository;
import com.app.file_transfer.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class MyFilesController {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/files/my-files")
    public String showMyFiles(@RequestParam(required = false) Long folderId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername());

        Folder currentFolder = null;
        if (folderId != null) {
            currentFolder = folderRepository.findById(folderId).orElse(null);
            if (currentFolder != null && !currentFolder.getUser().equals(user)) {
                return "redirect:/files/my-files";
            }
        }

        List<Folder> subFolders = folderRepository.findByUserAndParentAndDeletedFalse(user, currentFolder);
        List<File> files = fileRepository.findByUploaderAndFolderAndDeletedFalse(user, currentFolder);

        List<Folder> allFolders = folderRepository.findByUser(user);
        model.addAttribute("subFolders", subFolders);
        model.addAttribute("files", files);
        model.addAttribute("currentFolder", currentFolder);
        model.addAttribute("folders", allFolders);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("activePage", "My Files");
        model.addAttribute("dashboardViewModel", dashboardService.buildDashboard(user.getUsername()));

        return "my-files";
    }
}
