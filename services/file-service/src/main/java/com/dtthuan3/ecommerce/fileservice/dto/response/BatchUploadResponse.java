package com.dtthuan3.ecommerce.fileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResponse {

    private List<FileResponse> files;

    /**
     * Partial failures — empty when all succeeded.
     */
    private List<UploadFailure> failures;

    public BatchUploadResponse(List<FileResponse> files) {
        this.files = files;
        this.failures = List.of();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadFailure {
        private String filename;
        private String reason;
    }
}
