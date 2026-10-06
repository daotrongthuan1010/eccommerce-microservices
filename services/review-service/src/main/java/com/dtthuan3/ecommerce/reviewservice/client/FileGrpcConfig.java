package com.dtthuan3.ecommerce.reviewservice.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileGrpcConfig {

    @Bean
    public ManagedChannel fileServiceChannel() {
        return ManagedChannelBuilder
                .forAddress("file-service", 9090)
                .usePlaintext()
                .build();
    }
}