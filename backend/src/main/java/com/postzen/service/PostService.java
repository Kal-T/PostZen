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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "post:";
    private static final String CACHE_FEED_KEY = "posts:feed:page:0";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    // --- Caching Helper Methods ---

    private void cachePage0(Page<PostSummaryResponse> page) {
        try {
            com.postzen.dto.response.PagedResponse<PostSummaryResponse> wrapper = com.postzen.dto.response.PagedResponse
                    .from(page);
            String json = objectMapper.writeValueAsString(wrapper);
            redisTemplate.opsForValue().set(CACHE_FEED_KEY, json, CACHE_TTL);
        } catch (Exception e) {
            log.error("Failed to cache feed", e);
        }
    }

    private void invalidateFeedCache() {
        redisTemplate.delete(CACHE_FEED_KEY);
    }

    private void updateSinglePostCache(Post post) {
        if (post.getStatus() == Post.Status.PUBLISHED) {
            try {
                String json = objectMapper.writeValueAsString(PostResponse.fromEntity(post));
                redisTemplate.opsForValue().set(CACHE_PREFIX + post.getSlug(), json, CACHE_TTL);
            } catch (Exception e) {
                log.error("Failed to update cache for post: {}", post.getSlug(), e);
            }
        }
    }

    private void invalidateSinglePostCache(String slug) {
        redisTemplate.delete(CACHE_PREFIX + slug);
    }

    // --- Core Logic ---

    public Page<PostSummaryResponse> getPublishedPosts(Pageable pageable) {
        // Smart Caching: Only cache the first page (index 0)
        if (pageable.getPageNumber() == 0) {
            try {
                String cachedFeed = redisTemplate.opsForValue().get(CACHE_FEED_KEY);
                if (cachedFeed != null) {
                    com.postzen.dto.response.PagedResponse<PostSummaryResponse> wrapper = objectMapper.readValue(
                            cachedFeed,
                            new com.fasterxml.jackson.core.type.TypeReference<com.postzen.dto.response.PagedResponse<PostSummaryResponse>>() {
                            });
                    return new PageImpl<>(wrapper.getContent(), pageable, wrapper.getTotalElements());
                }
            } catch (Exception e) {
                log.error("Feed cache miss/error", e);
            }
        }

        Page<PostSummaryResponse> page = postRepository.findPublishedPosts(pageable)
                .map(PostSummaryResponse::fromEntity);

        if (pageable.getPageNumber() == 0) {
            cachePage0(page);
        }

        return page;
    }

    public Page<PostSummaryResponse> getPostsByAuthor(UUID authorId, Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        // Show all posts for owner or admin, otherwise only published
        if (currentUser != null && (currentUser.getId().equals(authorId) || securityUtils.isAdmin())) {
            return postRepository.findByAuthorId(authorId, pageable)
                    .map(PostSummaryResponse::fromEntity);
        }
        return postRepository.findByAuthorIdAndStatus(authorId, Post.Status.PUBLISHED, pageable)
                .map(PostSummaryResponse::fromEntity);
    }

    public PostResponse getPostBySlug(String slug) {
        // 1. Try to fetch from cache
        try {
            String cachedPost = redisTemplate.opsForValue().get(CACHE_PREFIX + slug);
            if (cachedPost != null) {
                return objectMapper.readValue(cachedPost, PostResponse.class);
            }
        } catch (Exception e) {
            log.error("Cache miss or error for slug: {}", slug, e);
        }

        // 2. Fetch from DB
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getStatus() != Post.Status.PUBLISHED) {
            User currentUser = securityUtils.getCurrentUser();
            if (currentUser == null
                    || (!currentUser.getId().equals(post.getAuthor().getId()) && !securityUtils.isAdmin())) {
                throw new ResourceNotFoundException("Post not found");
            }
        }

        PostResponse response = PostResponse.fromEntity(post);
        updateSinglePostCache(post); // Will only cache if published
        return response;
    }

    public PostResponse getPostById(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return PostResponse.fromEntity(post);
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null)
            throw new ForbiddenException("Authentication required");

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

        if (post.getStatus() == Post.Status.PUBLISHED) {
            invalidateFeedCache();
            updateSinglePostCache(post);
        }

        return PostResponse.fromEntity(post);
    }

    @Transactional
    public PostResponse updatePost(UUID id, UpdatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!securityUtils.canModifyResource(post.getAuthor().getId())) {
            throw new ForbiddenException("You don't have permission to update this post");
        }

        String oldSlug = post.getSlug();
        Post.Status oldStatus = post.getStatus();

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
            post.setSlug(generateSlug(request.getTitle()));
        }
        if (request.getContent() != null)
            post.setContent(request.getContent());
        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
            if (request.getStatus() == Post.Status.PUBLISHED && oldStatus != Post.Status.PUBLISHED) {
                post.setPublishedAt(LocalDateTime.now());
            }
        }
        if (request.getScheduledAt() != null)
            post.setScheduledAt(request.getScheduledAt());

        post = postRepository.save(post);

        // Cache Management
        if (!oldSlug.equals(post.getSlug())) {
            invalidateSinglePostCache(oldSlug);
        }

        if (post.getStatus() == Post.Status.PUBLISHED || oldStatus == Post.Status.PUBLISHED) {
            invalidateFeedCache();
        }

        if (post.getStatus() == Post.Status.PUBLISHED) {
            updateSinglePostCache(post);
        } else {
            // Ensure no stale cache if un-published
            invalidateSinglePostCache(post.getSlug());
        }

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

        invalidateSinglePostCache(post.getSlug());
        if (post.getStatus() == Post.Status.PUBLISHED) {
            invalidateFeedCache();
        }

        postRepository.delete(post);
        log.info("Post deleted: {}", post.getSlug());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void publishScheduledPosts() {
        List<Post> scheduledPosts = postRepository.findScheduledPostsToPublish(LocalDateTime.now());
        boolean anyPublished = false;

        for (Post post : scheduledPosts) {
            post.setStatus(Post.Status.PUBLISHED);
            post.setPublishedAt(LocalDateTime.now());
            postRepository.save(post);
            log.info("Scheduled post published: {}", post.getSlug());
            anyPublished = true;
        }

        if (anyPublished) {
            invalidateFeedCache();
        }
    }

    private String generateSlug(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NONLATIN.matcher(slug).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-");
        slug = slug.replaceAll("^-|-$", "");

        String baseSlug = slug;
        int counter = 1;
        while (postRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}
