package com.dtthuan3.ecommerce.catalogservice.service;


import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    String upload(MultipartFile file, String folder);

    String getPresignedUrl(String objectKey);

    void delete(String objectKey);
}