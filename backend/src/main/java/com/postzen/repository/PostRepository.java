package com.postzen.repository;

import com.postzen.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    Optional<Post> findBySlug(String slug);

    Page<Post> findByStatus(Post.Status status, Pageable pageable);

    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);

    Page<Post> findByAuthorIdAndStatus(UUID authorId, Post.Status status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = 'SCHEDULED' AND p.scheduledAt <= :now")
    List<Post> findScheduledPostsToPublish(@Param("now") LocalDateTime now);

    boolean existsBySlug(String slug);

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.publishedAt DESC")
    Page<Post> findPublishedPosts(Pageable pageable);
}
