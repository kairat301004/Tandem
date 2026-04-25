package org.example.tandem.service;

import org.example.tandem.dto.file.FileResponse;
import org.example.tandem.dto.news.NewsRequest;
import org.example.tandem.dto.news.NewsResponse;
import org.example.tandem.entity.News;
import org.example.tandem.entity.User;
import org.example.tandem.repository.NewsRepository;
import org.example.tandem.repository.UserRepository;
import org.example.tandem.security.CustomUserDetails;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NewsService {


    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final NotificationService notificationService;

    public NewsService(NewsRepository newsRepository, UserRepository userRepository, FileService fileService, NotificationService notificationService) {
        this.newsRepository = newsRepository;
        this.userRepository = userRepository;

        this.fileService = fileService;
        this.notificationService = notificationService;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new AccessDeniedException("User not authenticated");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }

    // CREATE
    @Transactional
    @CacheEvict(value = "allNews", allEntries = true)
    public NewsResponse createNews(NewsRequest request) {
        User currentUser = getCurrentUser();

        News savedNews = buildAndSaveNews(request, currentUser);

        return mapToResponse(savedNews);
    }

    @Transactional
    @CacheEvict(value = "allNews", allEntries = true)
    public NewsResponse createNews(NewsRequest request, List<MultipartFile> files) {
        User currentUser = getCurrentUser();

        News savedNews = buildAndSaveNews(request, currentUser);

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                fileService.uploadFileForNews(file, savedNews);
            }
        }

        // Уведомление всем пользователям
        String title = "Новая новость";
        String content = currentUser.getFirstName() + " " + currentUser.getLastName() +
                " опубликовал(а) " + request.getTitle();
        String link = "/news/" + savedNews.getId();

        notificationService.sendNotificationToAllUsers("NEWS", title, content, link);

        return mapToResponse(savedNews);
    }



    private News buildAndSaveNews(NewsRequest request, User currentUser) {
        News news = News.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(currentUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isPinned(false)
                .build();

        return newsRepository.save(news);
    }

    // READ ALL (с пагинацией)
    @Cacheable(value = "allNews", key = "{#page, #size}")
    public Page<NewsResponse> getAllNews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<News> newsPage = newsRepository.findAllByOrderByCreatedAtDesc(pageable);

        return newsPage.map(this::mapToResponse);
    }

    // READ BY ID
    @Cacheable(value = "news", key = "#id")
    public NewsResponse getNewsById(UUID id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
        return mapToResponse(news);
    }

    // UPDATE
    @Transactional
    @CachePut(value = "news", key = "#id")
    @CacheEvict(value = "allNews", allEntries = true)
    public NewsResponse updateNews(UUID id, NewsRequest request) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));

        User currentUser = getCurrentUser();

        // Проверка прав: автор или имеет право EDIT_NEWS
        boolean isAuthor = news.getAuthor().getId().equals(currentUser.getId());
        boolean hasPermissions = currentUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(perm -> perm.name().equals("UPDATE_NEWS"));

        if (!isAuthor && !hasPermissions) {
            throw new AccessDeniedException("У вас недостаточно прав для редактирвания этой новости");
        }

        news.setTitle(request.getTitle());
        news.setContent(request.getContent());
        news.setUpdatedAt(LocalDateTime.now());

        News updatedNews = newsRepository.save(news);
        return mapToResponse(updatedNews);
    }

    // DELETE
    @Transactional
    @CacheEvict(value = {"news", "allNews"}, allEntries = true)
    public void deleteNews(UUID id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Новость с id: " + id + " не найдена"));

        User currentUser = getCurrentUser();

        boolean isAuthor = news.getAuthor().getId().equals(currentUser.getId());
        boolean hasPermission = currentUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(perm -> perm.name().equals("DELETE_NEWS"));
        if (!isAuthor && !hasPermission) {
            throw new AccessDeniedException("У вас недостаточно прав для удаления этой новости");
        }

        //Сначала удаляем файлы, связанные с новостью
        fileService.deleteFilesByNewsId(id);

        //Затем удаляем саму новость
        newsRepository.delete(news);
    }


    // Вспомогательный метод для маппинга
    private NewsResponse mapToResponse(News news) {
        // Получаем файлы, прикрепленные к новости
        List<FileResponse> files = fileService.getFilesByNewsId(news.getId());

        return new NewsResponse(
                news.getId(),
                news.getTitle(),
                news.getContent(),
                news.getAuthor().getFirstName() + " " + news.getAuthor().getLastName(),
                news.getCreatedAt(),
                news.getIsPinned(),
                files  // <-- ДОБАВИТЬ
        );
    }

    public boolean isAuthor(UUID newsId, UUID userId) {
        return newsRepository.isAuthor(newsId, userId);
    }
}
