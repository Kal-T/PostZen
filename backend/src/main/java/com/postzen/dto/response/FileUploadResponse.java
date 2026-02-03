package com.postzen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private UUID id;
    private String filename;
    private String originalName;
    private String contentType;
    private long size;
    private String url;

    public static FileUploadResponse of(UUID id, String filename, String originalName,
            String contentType, long size) {
        return FileUploadResponse.builder()
                .id(id)
                .filename(filename)
                .originalName(originalName)
                .contentType(contentType)
                .size(size)
                .url("/api/files/" + filename)
                .build();
    }
}
