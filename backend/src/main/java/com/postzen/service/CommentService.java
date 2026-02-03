package com.postzen.service;

import com.postzen.dto.request.CreateCommentRequest;
import com.postzen.dto.response.CommentResponse;
import com.postzen.entity.Comment;
import com.postzen.entity.Post;
import com.postzen.entity.User;
import com.postzen.exception.ForbiddenException;
import com.postzen.exception.ResourceNotFoundException;
import com.postzen.repository.CommentRepository;
import com.postzen.repository.PostRepository;
import com.postzen.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final SecurityUtils securityUtils;

    public Page<CommentResponse> getCommentsByPostId(UUID postId, Pageable pageable) {
        // Verify post exists
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found");
        }

        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable)
                .map(CommentResponse::fromEntity);
    }

    @Transactional
    public CommentResponse createComment(UUID postId, CreateCommentRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("Authentication required");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        // Only allow comments on published posts
        if (post.getStatus() != Post.Status.PUBLISHED) {
            throw new ForbiddenException("Cannot comment on unpublished posts");
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(currentUser)
                .content(request.getContent())
                .build();

        comment = commentRepository.save(comment);
        log.info("Comment created on post {} by {}", post.getSlug(), currentUser.getEmail());

        return CommentResponse.fromEntity(comment);
    }

    @Transactional
    public void deleteComment(UUID id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!securityUtils.canModifyResource(comment.getAuthor().getId())) {
            throw new ForbiddenException("You don't have permission to delete this comment");
        }

        commentRepository.delete(comment);
        log.info("Comment deleted: {}", id);
    }
}
