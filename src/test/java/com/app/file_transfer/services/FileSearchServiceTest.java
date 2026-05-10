package com.app.file_transfer.services;

import com.app.file_transfer.dto.SearchResultDto;
import com.app.file_transfer.dto.SearchSuggestionDto;
import com.app.file_transfer.model.File;
import com.app.file_transfer.model.Folder;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.FolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileSearchServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FolderRepository folderRepository;

    @InjectMocks
    private FileSearchService fileSearchService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
    }

    @Test
    void getSuggestions_ShouldReturnCombinedResults() {
        Folder folder = new Folder();
        folder.setId(1L);
        folder.setName("Test Folder");

        File file = new File();
        file.setId(1L);
        file.setFileName("test.pdf");
        file.setFileType("pdf");

        when(folderRepository.findTop5ByUserAndNameContainingIgnoreCaseAndDeletedFalse(any(), anyString(), any()))
                .thenReturn(List.of(folder));
        when(fileRepository.findTop5ByUploaderAndFileNameContainingIgnoreCaseAndDeletedFalse(any(), anyString(), any()))
                .thenReturn(List.of(file));

        List<SearchSuggestionDto> suggestions = fileSearchService.getSuggestions(user, "test");

        assertEquals(2, suggestions.size());
        assertEquals("Test Folder", suggestions.get(0).name());
        assertTrue(suggestions.get(0).isFolder());
        assertEquals("test.pdf", suggestions.get(1).name());
        assertFalse(suggestions.get(1).isFolder());
    }

    @Test
    void search_ShouldReturnPagedResults() {
        File file = new File();
        file.setId(1L);
        file.setFileName("test.txt");
        file.setFileType("text/plain");

        when(folderRepository.searchFolders(any(), anyString(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(fileRepository.searchFiles(any(), anyString(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(file)));

        Page<SearchResultDto> results = fileSearchService.search(user, "test", null, null, null, PageRequest.of(0, 10));

        assertEquals(1, results.getContent().size());
        assertEquals("test.txt", results.getContent().get(0).name());
    }
}
