package com.dtthuan3.ecommerce.reviewservice.grpc;

import com.dtthuan3.ecommerce.grpc.file.FileGrpcServiceGrpc;
import com.dtthuan3.ecommerce.grpc.file.UploadFileRequest;
import com.dtthuan3.ecommerce.grpc.file.UploadFileResponse;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.google.protobuf.ByteString;
import java.io.IOException;
@Component
public class FileGrpcClient {
    @GrpcClient("file-service")
    private FileGrpcServiceGrpc.FileGrpcServiceBlockingStub fileStub;

    public UploadFileResponse upload(
            MultipartFile file,
            String folder
    ) {

        try {

            String filename =
                    file.getOriginalFilename() == null
                            ? "file"
                            : file.getOriginalFilename();

            String contentType =
                    file.getContentType() == null
                            ? "application/octet-stream"
                            : file.getContentType();

            UploadFileRequest request =
                    UploadFileRequest.newBuilder()

                            .setData(
                                    ByteString.copyFrom(
                                            file.getBytes()
                                    )
                            )

                            .setFilename(filename)

                            .setContentType(contentType)

                            .setFolder(
                                    folder == null
                                            ? ""
                                            : folder
                            )

                            .build();

            return fileStub.uploadFile(request);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Không thể đọc file upload",
                    e
            );

        } catch (StatusRuntimeException e) {

            throw new RuntimeException(
                    "Không thể gọi file-service qua gRPC: "
                            + e.getStatus(),
                    e
            );
        }
    }
}