package com.postzen.repository;

import com.postzen.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {

    List<UploadedFile> findByPostId(UUID postId);

    Optional<UploadedFile> findByFilename(String filename);
}
