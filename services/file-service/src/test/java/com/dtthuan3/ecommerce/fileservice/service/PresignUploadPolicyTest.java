package com.dtthuan3.ecommerce.fileservice.service;

import com.dtthuan3.ecommerce.fileservice.config.MinioProperties;
import com.dtthuan3.ecommerce.fileservice.config.UploadProperties;
import com.dtthuan3.ecommerce.fileservice.dto.request.PresignUploadRequest;
import com.dtthuan3.ecommerce.fileservice.dto.response.PresignUploadResponse;
import com.dtthuan3.ecommerce.fileservice.exception.FileValidationException;
import com.dtthuan3.ecommerce.fileservice.scanner.VirusScanner;
import com.dtthuan3.ecommerce.fileservice.service.impl.MinioFileStorageService;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Chinh sach presign upload (ch.14 §385-391 + FILE-07):
 * size du kien bat buoc, gioi han 50MB, PostPolicy gioi content-length-range.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresignUploadPolicyTest {

    @Mock private MinioClient minioClient;
    @Mock private MinioClient presignMinioClient;
    @Mock private VirusScanner virusScanner;
    @Mock private Executor fileUploadExecutor;

    private MinioFileStorageService service;

    @BeforeEach
    void setUp() {
        MinioProperties minioProps = new MinioProperties(
                "http://minio:9000", "https://files.example.com", "access", "secret",
                "ecommerce-files", "us-east-1", true);
        UploadProperties uploadProps = new UploadProperties(900, 86400,
                List.of("image/*", "video/*", "application/pdf"));
        service = new MinioFileStorageService(minioClient, presignMinioClient,
                minioProps, uploadProps, virusScanner, fileUploadExecutor);
    }

    private PresignUploadRequest request(long sizeBytes) {
        PresignUploadRequest r = new PresignUploadRequest();
        r.setFilename("photo.jpg");
        r.setContentType("image/jpeg");
        r.setSizeBytes(sizeBytes);
        r.setFolder("products");
        return r;
    }

    @Test
    @DisplayName("Presign hop le: tra formData PostPolicy + size da duyet + URL bucket")
    void presignOk() throws Exception {
        when(presignMinioClient.getPresignedPostFormData(any(PostPolicy.class)))
                .thenReturn(Map.of("policy", "abc", "x-amz-signature", "sig"));

        PresignUploadResponse resp = service.presignUpload(request(1024));

        assertThat(resp.getKey()).startsWith("products/");
        assertThat(resp.getSizeBytes()).isEqualTo(1024);
        assertThat(resp.getFormData()).containsKeys("policy", "x-amz-signature");
        assertThat(resp.getUploadUrl()).isEqualTo("https://files.example.com/ecommerce-files");
        // PostPolicy thuc su duoc dung (khong con presign PUT khong gioi han)
        verify(presignMinioClient).getPresignedPostFormData(any(PostPolicy.class));
    }

    @Test
    @DisplayName("Size du kien thieu: tu choi — khong cap URL mù (FILE-07 + ch.14 §385)")
    void missingSizeRejected() throws Exception {
        PresignUploadRequest r = request(0);
        assertThatThrownBy(() -> service.presignUpload(r))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("sizeBytes");
    }

    @Test
    @DisplayName("Size du kien vuot 50MB: tu choi — bypass REST limit khong duoc phep")
    void oversizeRejected() {
        assertThatThrownBy(() -> service.presignUpload(request(51L * 1024 * 1024)))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("50MB");
    }

    @Test
    @DisplayName("Content-type khong duoc phep qua presign: tu choi nhu REST")
    void badContentTypeRejected() {
        PresignUploadRequest r = request(1024);
        r.setContentType("application/x-msdownload");
        assertThatThrownBy(() -> service.presignUpload(r))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("không được hỗ trợ");
    }

    @Test
    @DisplayName("Folder chua path traversal: tu choi")
    void traversalFolderRejected() {
        PresignUploadRequest r = request(1024);
        r.setFolder("../secret");
        assertThatThrownBy(() -> service.presignUpload(r))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("không hợp lệ");
    }
}
