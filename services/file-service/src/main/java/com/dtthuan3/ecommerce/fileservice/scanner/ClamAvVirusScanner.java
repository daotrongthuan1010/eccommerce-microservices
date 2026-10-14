package com.dtthuan3.ecommerce.fileservice.scanner;

import com.dtthuan3.ecommerce.fileservice.config.ClamAvProperties;
import com.dtthuan3.ecommerce.fileservice.exception.VirusDetectedException;
import com.dtthuan3.ecommerce.fileservice.exception.VirusScanException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClamAvVirusScanner implements VirusScanner {

    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_RETRIES = 1;

    private final ClamAvProperties clamAvProperties;

    @Override
    public void scan(MultipartFile file) {
        if (!clamAvProperties.enabled()) {
            log.warn("ClamAV scanning is disabled — skip scan for {}", file != null ? file.getOriginalFilename() : "null");
            return;
        }
        if (file == null || file.isEmpty()) {
            throw new VirusScanException("Không thể quét file rỗng");
        }
        // Cache bytes once: caller (MinioFileStorageService) already caches, but gRPC/standalone also benefits
        byte[] data;
        try {
            data = file.getBytes();
        } catch (Exception e) {
            throw new VirusScanException("Không thể đọc file để quét virus", e);
        }
        log.debug("ClamAV scan start file={}, size={}, host={}:{}", file.getOriginalFilename(), data.length, clamAvProperties.host(), clamAvProperties.port());

        VirusScanException last = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                doScan(data, file.getOriginalFilename());
                return;
            } catch (VirusDetectedException e) {
                throw e;
            } catch (VirusScanException e) {
                last = e;
                if (attempt < MAX_RETRIES) {
                    log.warn("ClamAV scan failed attempt {}/{} for {}: {} — retrying", attempt + 1, MAX_RETRIES + 1, file.getOriginalFilename(), e.getMessage());
                    try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw last != null ? last : new VirusScanException("Không thể quét file bằng ClamAV");
    }

    private void doScan(byte[] data, String filename) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(clamAvProperties.host(), clamAvProperties.port()), clamAvProperties.connectTimeout());
            socket.setSoTimeout(clamAvProperties.readTimeout());

            try (DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                output.write("zINSTREAM\0".getBytes(StandardCharsets.UTF_8));

                int offset = 0;
                while (offset < data.length) {
                    int chunk = Math.min(BUFFER_SIZE, data.length - offset);
                    output.writeInt(chunk);
                    output.write(data, offset, chunk);
                    offset += chunk;
                }
                output.writeInt(0);
                output.flush();

                String response = readResponse(socket.getInputStream());
                log.info("ClamAV result for file {}: {}", filename, response);
                handleResponse(response, filename);
            }
        } catch (VirusDetectedException e) {
            throw e;
        } catch (Exception e) {
            throw new VirusScanException("Không thể kết nối hoặc quét file bằng ClamAV (" + clamAvProperties.host() + ":" + clamAvProperties.port() + ")", e);
        }
    }

    private String readResponse(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        int value;
        while ((value = inputStream.read()) != -1) {
            if (value == 0) break;
            builder.append((char) value);
        }
        return builder.toString().trim();
    }

    private void handleResponse(String response, String originalFilename) {
        if (response == null || response.isBlank()) {
            throw new VirusScanException("ClamAV không trả kết quả quét");
        }
        if (response.endsWith("OK")) return;
        if (response.contains("FOUND")) {
            throw new VirusDetectedException("Phát hiện file nguy hiểm: " + originalFilename + " (" + response + ")");
        }
        throw new VirusScanException("ClamAV scan lỗi: " + response);
    }
}
