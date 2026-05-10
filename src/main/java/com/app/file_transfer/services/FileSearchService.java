package com.app.file_transfer.services;

import com.app.file_transfer.dto.SearchResultDto;
import com.app.file_transfer.dto.SearchSuggestionDto;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileSearchService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;

    @Cacheable(value = "searchSuggestions", key = "#user.id + '_' + #query")
    public List<SearchSuggestionDto> getSuggestions(User user, String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        Pageable topFive = PageRequest.of(0, 5);
        
        List<SearchSuggestionDto> suggestions = new ArrayList<>();
        
        folderRepository.findTop5ByUserAndNameContainingIgnoreCaseAndDeletedFalse(user, query, topFive)
                .forEach(f -> suggestions.add(new SearchSuggestionDto(f.getId(), f.getName(), true, "folder")));
        
        fileRepository.findTop5ByUploaderAndFileNameContainingIgnoreCaseAndDeletedFalse(user, query, topFive)
                .forEach(f -> suggestions.add(new SearchSuggestionDto(f.getId(), f.getFileName(), false, f.getFileType())));

        return suggestions.stream().limit(10).collect(Collectors.toList());
    }

    public Page<SearchResultDto> search(User user, String query, String type, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        List<SearchResultDto> allResults = new ArrayList<>();

        // If type is not specified or it's "folder", search folders
        if (type == null || type.equalsIgnoreCase("folder")) {
            folderRepository.searchFolders(user, query, start, end, Pageable.unpaged())
                    .forEach(f -> allResults.add(new SearchResultDto(
                            f.getId(), f.getName(), "Folder", 0, f.getUpdatedAt(), 
                            f.getParent() != null ? f.getParent().getName() : "Root", true)));
        }

        // Search files
        fileRepository.searchFiles(user, query, type, start, end, Pageable.unpaged())
                .forEach(f -> allResults.add(new SearchResultDto(
                        f.getId(), f.getFileName(), f.getFileType(), f.getFileSize(), f.getUpdatedAt(),
                        f.getFolder() != null ? f.getFolder().getName() : "Root", false)));

        // Manual sorting and pagination for combined results (simplified for now)
        int startIdx = (int) pageable.getOffset();
        int endIdx = Math.min((startIdx + pageable.getPageSize()), allResults.size());
        
        List<SearchResultDto> pagedResults = (startIdx < allResults.size()) 
                ? allResults.subList(startIdx, endIdx) 
                : new ArrayList<>();

        return new PageImpl<>(pagedResults, pageable, allResults.size());
    }
}
