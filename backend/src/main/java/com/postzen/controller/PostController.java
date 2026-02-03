package com.postzen.controller;

import com.postzen.dto.request.CreatePostRequest;
import com.postzen.dto.request.UpdatePostRequest;
import com.postzen.dto.response.MessageResponse;
import com.postzen.dto.response.PostResponse;
import com.postzen.dto.response.PostSummaryResponse;
import com.postzen.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Post management endpoints")
public class PostController {

    private final PostService postService;

    @GetMapping
    @Operation(summary = "Get published posts with pagination")
    public ResponseEntity<Page<PostSummaryResponse>> getPosts(
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postService.getPublishedPosts(pageable));
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "Get posts by author")
    public ResponseEntity<Page<PostSummaryResponse>> getPostsByAuthor(
            @PathVariable UUID authorId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postService.getPostsByAuthor(authorId, pageable));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get post by slug")
    public ResponseEntity<PostResponse> getPostBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(postService.getPostBySlug(slug));
    }

    @PostMapping
    @Operation(summary = "Create a new post")
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.ok(postService.createPost(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a post")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(postService.updatePost(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a post")
    public ResponseEntity<MessageResponse> deletePost(@PathVariable UUID id) {
        postService.deletePost(id);
        return ResponseEntity.ok(MessageResponse.of("Post deleted successfully"));
    }
}
