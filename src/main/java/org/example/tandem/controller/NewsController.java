package org.example.tandem.controller;

import jakarta.validation.Valid;
import org.example.tandem.dto.news.NewsRequest;
import org.example.tandem.dto.news.NewsResponse;
import org.example.tandem.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/news")
public class NewsController {


    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

//    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
//    @PreAuthorize("hasAuthority('CREATE_NEWS')")
//    public ResponseEntity<NewsResponse> createNews(
//            @RequestPart(value = "news", required = false) @Valid NewsRequest multipartRequest,
//            @RequestPart(value = "files", required = false) List<MultipartFile> files,
//            @RequestBody(required = false) NewsRequest jsonRequest) {
//
//        // Определяем источник данных
//        NewsRequest request = multipartRequest != null ? multipartRequest : jsonRequest;
//
//        if (request == null) {
//            throw new IllegalArgumentException("Request body is required");
//        }
//
//        NewsResponse response = newsService.createNews(request, files != null ? files : List.of());
//        return new ResponseEntity<>(response, HttpStatus.CREATED);
//    }

    // Для запросов БЕЗ файлов (простой JSON)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('CREATE_NEWS')")
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody NewsRequest request) {
        return new ResponseEntity<>(newsService.createNews(request, List.of()), HttpStatus.CREATED);
    }

    // Для запросов С файлами (Multipart)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATE_NEWS')")
    public ResponseEntity<NewsResponse> createNews(
            @RequestPart("news") NewsRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return new ResponseEntity<>(newsService.createNews(request, files), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NewsResponse> newsPage = newsService.getAllNews(page, size);
        return ResponseEntity.ok(newsPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable UUID id) {
        NewsResponse response = newsService.getNewsById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE_NEWS') or @newsService.isAuthor(#id, authentication.principal.user.id)")
    public ResponseEntity<NewsResponse> updateNews(
            @PathVariable UUID id,
            @Valid @RequestBody NewsRequest request) {
        NewsResponse response = newsService.updateNews(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_NEWS') or @newsService.isAuthor(#id, authentication.principal.user.id)")
    public ResponseEntity<Void> deleteNews(
            @PathVariable UUID id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }
}
