package com.dtthuan3.ecommerce.reviewservice.client;

import com.dtthuan3.ecommerce.fileservice.grpc.FileGrpcServiceGrpc;
import com.dtthuan3.ecommerce.fileservice.grpc.FileProto;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FileGrpcClientService {
    @GrpcClient("file-service")
    private FileGrpcServiceGrpc.FileGrpcServiceBlockingStub fileStub;
    public Map<String, String> getPublicMediaUrls(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Map.of();
        }

        try {
            FileProto.MediaUrlRequest request = FileProto.MediaUrlRequest.newBuilder()
                    .addAllFileIds(fileIds)
                    .build();

            // Nếu IDE vẫn báo đỏ chỗ này, hãy xóa chữ "getPublicMediaUrls",
            // gõ "fileStub." và nhấn Ctrl + Space để IDE tự gợi ý đúng tên hàm đã được generate.
            FileProto.MediaUrlResponse response = fileStub.getPublicMediaUrls(request);

            return response.getMediaUrlsMap();

        } catch (Exception e) {
            log.error("gRPC call to File-service failed: {}", e.getMessage());
            return Map.of();
        }
    }
}
