package com.postzen.dto.request;

import com.postzen.entity.Post;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdatePostRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String content;

    private Post.Status status;

    private LocalDateTime scheduledAt;
}
