package com.jobboard.jobposting;

import com.jobboard.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Component
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir);
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String store(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PDF 파일만 업로드할 수 있습니다.");
        }
        String storedName = UUID.randomUUID() + ".pdf";
        Path target = uploadDir.resolve(storedName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return storedName;
    }

    public Path resolve(String storedName) {
        return uploadDir.resolve(storedName);
    }
}
