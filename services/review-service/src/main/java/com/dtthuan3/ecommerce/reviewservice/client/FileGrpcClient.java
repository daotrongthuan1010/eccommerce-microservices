//package com.dtthuan3.ecommerce.reviewservice.client;
//
//import com.dtthuan3.ecommerce.common.exception.BusinessException;
//import com.dtthuan3.ecommerce.common.exception.ErrorCode;
//import com.dtthuan3.ecommerce.grpc.file.FileGrpcServiceGrpc;
//import com.dtthuan3.ecommerce.grpc.file.UploadFileRequest;
//import com.dtthuan3.ecommerce.grpc.file.UploadFileResponse;
//import com.google.protobuf.ByteString;
//import io.grpc.StatusRuntimeException;
//import lombok.extern.slf4j.Slf4j;
//import net.devh.boot.grpc.client.inject.GrpcClient;
//import org.springframework.stereotype.Component;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//
///**
// * Client gRPC gọi file-service. CHỈ dùng RPC UploadFile đã có sẵn ở file-service (không cần sửa file-service).
// * Giới hạn của file-service: mỗi file tối đa 10MB; URL trả về là presigned, hết hạn sau ~15 phút.
// *
// * Yêu cầu: review-service/src/main/proto/FileService.proto phải GIỐNG file-service
// * (package file, java_package com.dtthuan3.ecommerce.grpc.file, chỉ có rpc UploadFile).
// * Địa chỉ: grpc.client.file-service.address trong application.yml.
// */
//@Slf4j
//@Component
//public class FileGrpcClient {
//
//    /** key = định danh object trong MinIO (lưu lại để dùng sau); url = presigned GET tạm thời. */
//    public record StoredFile(String key, String bucket, String contentType, long size,
//                             String url, long urlExpiresInSeconds) {
//    }
//
//    private static final long DEADLINE_SECONDS = 30;
//
//    @GrpcClient("file-service")
//    private FileGrpcServiceGrpc.FileGrpcServiceBlockingStub fileStub;
//
//    public StoredFile upload(MultipartFile file, String folder) {
//        try {
//            return upload(file.getBytes(), file.getOriginalFilename(), file.getContentType(), folder);
//        } catch (IOException e) {
//            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không đọc được tệp upload");
//        }
//    }
//
//    public StoredFile upload(byte[] data, String filename, String contentType, String folder) {
//        try {
//            UploadFileResponse r = fileStub
//                    .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS) // mốc tuyệt đối: gắn lại mỗi lần gọi
//                    .uploadFile(UploadFileRequest.newBuilder()
//                            .setData(ByteString.copyFrom(data))
//                            .setFilename(filename == null || filename.isBlank() ? "file" : filename)
//                            .setContentType(contentType == null ? "application/octet-stream" : contentType)
//                            .setFolder(folder == null ? "" : folder)
//                            .build());
//            return new StoredFile(r.getKey(), r.getBucket(), r.getContentType(), r.getSize(),
//                    r.getUrl(), r.getUrlExpiresInSeconds());
//        } catch (StatusRuntimeException e) {
//            log.warn("gRPC file-service.uploadFile thất bại: {}", e.getStatus());
//            throw switch (e.getStatus().getCode()) {
//                case INVALID_ARGUMENT -> new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
//                        e.getStatus().getDescription() != null ? e.getStatus().getDescription() : "File không hợp lệ");
//                case UNAVAILABLE, DEADLINE_EXCEEDED -> new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
//                        "Dịch vụ lưu trữ tạm thời không khả dụng, vui lòng thử lại sau");
//                default -> new BusinessException(ErrorCode.DEPENDENCY_ERROR,
//                        "Không thể upload tệp, vui lòng thử lại sau");
//            };
//        }
//    }
//}