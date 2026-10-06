package com.dtthuan3.ecommerce.reviewservice.grpc;

import com.dtthuan3.ecommerce.grpc.FileServiceGrpc;
import com.dtthuan3.ecommerce.grpc.UploadFileRequest;
import com.dtthuan3.ecommerce.grpc.UploadFileResponse;
import io.grpc.ManagedChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileGrpcClient {

    private final FileServiceGrpc.FileServiceBlockingStub fileServiceStub;

    public FileGrpcClient(ManagedChannel fileServiceChannel) {
        this.fileServiceStub =
                FileServiceGrpc.newBlockingStub(fileServiceChannel);
    }

    public UploadFileResponse upload(
            MultipartFile file,
            String folder
    ) throws Exception {

        UploadFileRequest request =
                UploadFileRequest.newBuilder()
                        .setFilename(file.getOriginalFilename())
                        .setContentType(file.getContentType())
                        .setData(
                                com.google.protobuf.ByteString
                                        .copyFrom(file.getBytes())
                        )
                        .setFolder(folder)
                        .build();

        return fileServiceStub.uploadFile(request);
    }
}