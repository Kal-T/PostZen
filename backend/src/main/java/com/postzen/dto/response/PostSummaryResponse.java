package com.postzen.dto.response;

import com.postzen.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSummaryResponse {
    private UUID id;
    private String title;
    private String slug;
    private String excerpt;
    private Post.Status status;
    private LocalDateTime publishedAt;
    private PostResponse.AuthorDto author;
    private int commentCount;

    public static PostSummaryResponse fromEntity(Post post) {
        String content = post.getContent();
        String excerpt = content.length() > 200 ? content.substring(0, 200) + "..." : content;

        return PostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .excerpt(excerpt)
                .status(post.getStatus())
                .publishedAt(post.getPublishedAt())
                .author(PostResponse.AuthorDto.builder()
                        .id(post.getAuthor().getId())
                        .username(post.getAuthor().getUsername())
                        .build())
                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                .build();
    }
}
