package com.app.file_transfer.controller;

import com.app.file_transfer.dto.SearchResultDto;
import com.app.file_transfer.model.User;
import com.app.file_transfer.services.FileSearchService;
import com.app.file_transfer.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class FileSearchController {

    private final FileSearchService fileSearchService;
    private final UserService userService;

    @GetMapping
    public String search(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(name = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        User user = userService.getUserByUsername(userDetails.getUsername());
        
        if (query == null || query.isBlank()) {
            return "redirect:/";
        }

        Page<SearchResultDto> results = fileSearchService.search(user, query, type, start, end, PageRequest.of(page, size));
        
        model.addAttribute("results", results);
        model.addAttribute("query", query);
        model.addAttribute("type", type);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        
        return "search-results";
    }
}
