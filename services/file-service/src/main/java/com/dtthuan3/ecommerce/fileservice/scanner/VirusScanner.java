package com.dtthuan3.ecommerce.fileservice.scanner;

import org.springframework.web.multipart.MultipartFile;

public interface VirusScanner {

    void scan(MultipartFile file);
}