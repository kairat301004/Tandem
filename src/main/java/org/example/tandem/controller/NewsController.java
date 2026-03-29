package org.example.tandem.controller;

import jakarta.validation.Valid;
import org.example.tandem.dto.news.NewsRequest;
import org.example.tandem.dto.news.NewsResponse;
import org.example.tandem.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/news")
public class NewsController {


    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_NEWS')")
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody NewsRequest request) {
        NewsResponse response = newsService.createNews(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
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
