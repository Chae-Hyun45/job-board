package com.jobboard.jobposting;

import com.jobboard.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void PDF_파일을_저장하고_경로를_반환한다() throws Exception {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "posting.pdf", "application/pdf", "dummy-content".getBytes());

        String storedName = service.store(file);

        assertThat(storedName).endsWith(".pdf");
        assertThat(Files.exists(service.resolve(storedName))).isTrue();
    }

    @Test
    void PDF가_아닌_파일이면_예외를_던진다() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "posting.txt", "text/plain", "dummy".getBytes());

        assertThatThrownBy(() -> service.store(file)).isInstanceOf(ApiException.class);
    }
}
