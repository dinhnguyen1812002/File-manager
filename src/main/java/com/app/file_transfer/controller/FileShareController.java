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

import java.util.Collections;
import java.util.List;

@Controller
public class FileShareController {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/files/shared")
    public String showSharedFiles(@AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername());

        // Files shared with me
        List<File> files = fileRepository.findByRecipientsContainsAndDeletedFalse(user);

        List<Folder> allFolders = folderRepository.findByUser(user);
        model.addAttribute("subFolders", Collections.emptyList()); // Shared folders not implemented yet
        model.addAttribute("files", files);
        model.addAttribute("folders", allFolders);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("activePage", "Shared Files");
        model.addAttribute("dashboardViewModel", dashboardService.buildDashboard(user.getUsername()));

        return "shared-files";
    }
}
