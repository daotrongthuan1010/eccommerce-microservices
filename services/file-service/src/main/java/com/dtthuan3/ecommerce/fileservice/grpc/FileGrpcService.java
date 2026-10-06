package com.dtthuan3.ecommerce.fileservice.grpc;

import com.dtthuan3.ecommerce.fileservice.dto.response.FileResponse;
import com.dtthuan3.ecommerce.fileservice.service.FileStorageService;

import com.dtthuan3.ecommerce.grpc.file.FileGrpcServiceGrpc;
import com.dtthuan3.ecommerce.grpc.file.UploadFileRequest;
import com.dtthuan3.ecommerce.grpc.file.UploadFileResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class FileGrpcService
        extends FileGrpcServiceGrpc.FileGrpcServiceImplBase {

    private final FileStorageService fileStorageService;

    @Override
    public void uploadFile(
            UploadFileRequest request,
            StreamObserver<UploadFileResponse> responseObserver
    ) {
        try {

            byte[] data = request.getData().toByteArray();

            FileResponse fileResponse =
                    fileStorageService.upload(
                            data,
                            request.getFilename(),
                            request.getContentType(),
                            request.getFolder()
                    );

            UploadFileResponse response =
                    UploadFileResponse.newBuilder()
                            .setKey(fileResponse.getKey())
                            .setBucket(fileResponse.getBucket())
                            .setOriginalName(fileResponse.getOriginalName())
                            .setContentType(fileResponse.getContentType())
                            .setSize(fileResponse.getSize())
                            .setUrl(
                                    fileResponse.getUrl() == null
                                            ? ""
                                            : fileResponse.getUrl()
                            )
                            .setUrlExpiresInSeconds(
                                    fileResponse.getUrlExpiresInSeconds()
                            )
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}