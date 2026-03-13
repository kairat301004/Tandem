package org.example.tandem.repository;

import org.example.tandem.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NewsRepository extends JpaRepository<News, UUID> {

    // Для пагинации и сортировки (все новости с сортировкой по дате)
    // Используем EntityGraph для подгрузки автора сразу
    @EntityGraph(attributePaths = {"author"})
    Page<News> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Проверка, является ли пользователь автором
    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END " +
    "FROM News n WHERE n.id = :newsId AND n.author.id = :userId")
    boolean isAuthor(@Param("newsId") UUID newsId, @Param("userId") UUID userId);
}
