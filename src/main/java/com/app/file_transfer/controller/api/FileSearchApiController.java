package com.app.file_transfer.controller.api;

import com.app.file_transfer.dto.SearchSuggestionDto;
import com.app.file_transfer.model.User;
import com.app.file_transfer.services.FileSearchService;
import com.app.file_transfer.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class FileSearchApiController {

    private final FileSearchService fileSearchService;
    private final UserService userService;

    @GetMapping("/suggestions")
    public ResponseEntity<List<SearchSuggestionDto>> getSuggestions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("q") String query) {
        
        User user = userService.getUserByUsername(userDetails.getUsername());
        List<SearchSuggestionDto> suggestions = fileSearchService.getSuggestions(user, query);
        
        return ResponseEntity.ok(suggestions);
    }
}
