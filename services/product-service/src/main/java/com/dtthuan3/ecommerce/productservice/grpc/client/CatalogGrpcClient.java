package com.dtthuan3.ecommerce.productservice.grpc.client;

import com.dtthuan3.ecommerce.grpc.catalog.ProductCatalogGrpcServiceGrpc;
import com.dtthuan3.ecommerce.grpc.catalog.SyncProductRequest;
import com.dtthuan3.ecommerce.grpc.catalog.SyncProductResponse;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class CatalogGrpcClient {

    @GrpcClient("catalog-service")
    private ProductCatalogGrpcServiceGrpc.ProductCatalogGrpcServiceBlockingStub catalogStub;

    public SyncProductResponse syncProduct(SyncProductRequest request) {
        try {
            return catalogStub
                    .withDeadlineAfter(3, java.util.concurrent.TimeUnit.SECONDS)
                    .syncProduct(request);
        } catch (StatusRuntimeException exception) {
            throw new IllegalStateException(
                    "Không thể đồng bộ sản phẩm sang Catalog Service: "
                            + exception.getStatus(),
                    exception
            );
        }
    }
}