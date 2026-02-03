package com.postzen.service;

import com.postzen.dto.request.CreatePostRequest;
import com.postzen.dto.request.UpdatePostRequest;
import com.postzen.dto.response.PostResponse;
import com.postzen.dto.response.PostSummaryResponse;
import com.postzen.entity.Post;
import com.postzen.entity.User;
import com.postzen.exception.ForbiddenException;
import com.postzen.exception.ResourceNotFoundException;
import com.postzen.repository.PostRepository;
import com.postzen.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final SecurityUtils securityUtils;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_PREFIX = "post:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public Page<PostSummaryResponse> getPublishedPosts(Pageable pageable) {
        return postRepository.findPublishedPosts(pageable)
                .map(PostSummaryResponse::fromEntity);
    }

    public Page<PostSummaryResponse> getPostsByAuthor(UUID authorId, Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser != null && (currentUser.getId().equals(authorId) || securityUtils.isAdmin())) {
            // Show all posts for owner or admin
            return postRepository.findByAuthorId(authorId, pageable)
                    .map(PostSummaryResponse::fromEntity);
        }
        // Show only published posts for others
        return postRepository.findByAuthorIdAndStatus(authorId, Post.Status.PUBLISHED, pageable)
                .map(PostSummaryResponse::fromEntity);
    }

    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        // Check visibility
        if (post.getStatus() != Post.Status.PUBLISHED) {
            User currentUser = securityUtils.getCurrentUser();
            if (currentUser == null ||
                    (!currentUser.getId().equals(post.getAuthor().getId()) && !securityUtils.isAdmin())) {
                throw new ResourceNotFoundException("Post not found");
            }
        }

        return PostResponse.fromEntity(post);
    }

    public PostResponse getPostById(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return PostResponse.fromEntity(post);
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("Authentication required");
        }

        String slug = generateSlug(request.getTitle());

        Post post = Post.builder()
                .author(currentUser)
                .title(request.getTitle())
                .content(request.getContent())
                .slug(slug)
                .status(request.getStatus())
                .scheduledAt(request.getScheduledAt())
                .build();

        if (request.getStatus() == Post.Status.PUBLISHED) {
            post.setPublishedAt(LocalDateTime.now());
        }

        post = postRepository.save(post);
        log.info("Post created: {} by {}", post.getSlug(), currentUser.getEmail());

        return PostResponse.fromEntity(post);
    }

    @Transactional
    public PostResponse updatePost(UUID id, UpdatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!securityUtils.canModifyResource(post.getAuthor().getId())) {
            throw new ForbiddenException("You don't have permission to update this post");
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
            post.setSlug(generateSlug(request.getTitle()));
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getStatus() != null) {
            Post.Status oldStatus = post.getStatus();
            post.setStatus(request.getStatus());

            if (request.getStatus() == Post.Status.PUBLISHED && oldStatus != Post.Status.PUBLISHED) {
                post.setPublishedAt(LocalDateTime.now());
            }
        }
        if (request.getScheduledAt() != null) {
            post.setScheduledAt(request.getScheduledAt());
        }

        post = postRepository.save(post);

        // Invalidate cache
        redisTemplate.delete(CACHE_PREFIX + post.getSlug());

        log.info("Post updated: {}", post.getSlug());
        return PostResponse.fromEntity(post);
    }

    @Transactional
    public void deletePost(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!securityUtils.canModifyResource(post.getAuthor().getId())) {
            throw new ForbiddenException("You don't have permission to delete this post");
        }

        // Invalidate cache
        redisTemplate.delete(CACHE_PREFIX + post.getSlug());

        postRepository.delete(post);
        log.info("Post deleted: {}", post.getSlug());
    }

    @Scheduled(fixedRate = 60000) // Check every minute
    @Transactional
    public void publishScheduledPosts() {
        List<Post> scheduledPosts = postRepository.findScheduledPostsToPublish(LocalDateTime.now());

        for (Post post : scheduledPosts) {
            post.setStatus(Post.Status.PUBLISHED);
            post.setPublishedAt(LocalDateTime.now());
            postRepository.save(post);
            log.info("Scheduled post published: {}", post.getSlug());
        }
    }

    private String generateSlug(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NONLATIN.matcher(slug).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-");
        slug = slug.replaceAll("^-|-$", "");

        // Ensure uniqueness
        String baseSlug = slug;
        int counter = 1;
        while (postRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }
}
