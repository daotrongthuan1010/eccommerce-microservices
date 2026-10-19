package com.dtthuan3.ecommerce.productservice.grpc.client;

import com.dtthuan3.ecommerce.grpc.file.FileGrpcServiceGrpc;
import com.dtthuan3.ecommerce.grpc.file.UploadFileRequest;
import com.dtthuan3.ecommerce.grpc.file.UploadFileResponse;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class FileGrpcClient {

    @GrpcClient("file-service")
    private FileGrpcServiceGrpc.FileGrpcServiceBlockingStub fileStub;

    public UploadFileResponse upload(
            MultipartFile file,
            String folder
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File tải lên không được để trống"
            );
        }

        try {
            String filename = file.getOriginalFilename();

            UploadFileRequest request = UploadFileRequest.newBuilder()
                    .setData(ByteString.copyFrom(file.getBytes()))
                    .setFilename(
                            filename == null || filename.isBlank()
                                    ? "upload"
                                    : filename
                    )
                    .setContentType(
                            file.getContentType() == null
                                    ? "application/octet-stream"
                                    : file.getContentType()
                    )
                    .setFolder(folder == null ? "" : folder)
                    .build();

            return fileStub.uploadFile(request);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Không thể đọc file tải lên",
                    exception
            );
        } catch (StatusRuntimeException exception) {
            throw new IllegalStateException(
                    "File-service upload thất bại: "
                            + exception.getStatus(),
                    exception
            );
        }
    }
}
