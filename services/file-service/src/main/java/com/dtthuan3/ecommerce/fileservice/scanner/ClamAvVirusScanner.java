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

    private final ClamAvProperties clamAvProperties;

    @Override
    public void scan(MultipartFile file) {

        if (!clamAvProperties.enabled()) {
            log.warn("ClamAV scanning is disabled");
            return;
        }

        if (file == null || file.isEmpty()) {
            throw new VirusScanException("Không thể quét file rỗng");
        }

        try (
                Socket socket = new Socket(); InputStream fileInputStream = file.getInputStream()
        ) {

            socket.connect(
                    new InetSocketAddress(
                            clamAvProperties.host(),
                            clamAvProperties.port()
                    ),
                    clamAvProperties.connectTimeout()
            );

            socket.setSoTimeout(
                    clamAvProperties.readTimeout()
            );

            DataOutputStream output =
                    new DataOutputStream(
                            socket.getOutputStream()
                    );

            /*
             * zINSTREAM\0
             *
             * Báo cho ClamAV:
             * "Tôi sẽ stream file sang để quét"
             */
            output.write("zINSTREAM\0".getBytes(StandardCharsets.UTF_8));

            byte[] buffer = new byte[BUFFER_SIZE];

            int read;

            while ((read = fileInputStream.read(buffer)) != -1) {

                /*
                 * ClamAV yêu cầu trước mỗi chunk
                 * phải gửi kích thước 4 bytes.
                 */
                output.writeInt(read);
                output.write(buffer, 0, read);
            }

            /*
             * 0 nghĩa là kết thúc stream
             */
            output.writeInt(0);

            output.flush();

            String response = readResponse(socket.getInputStream());

            log.info(
                    "ClamAV result for file {}: {}",
                    file.getOriginalFilename(),
                    response
            );

            handleResponse(response, file.getOriginalFilename());

        } catch (VirusDetectedException e) {

            throw e;

        } catch (Exception e) {
            throw new VirusScanException(
                    "Không thể kết nối hoặc quét file bằng ClamAV",
                    e
            );
        }
    }

    private String readResponse(InputStream inputStream) throws Exception {

        StringBuilder builder = new StringBuilder();

        int value;

        while ((value = inputStream.read()) != -1) {
            if (value == 0) {break;}

            builder.append((char) value);
        }

        return builder.toString().trim();
    }

    private void handleResponse(
            String response,
            String originalFilename
    ) {

        if (response == null || response.isBlank()) {
            throw new VirusScanException("ClamAV không trả kết quả quét");
        }

        /*
         * Ví dụ:
         *
         * stream: OK
         */
        if (response.endsWith("OK")) {return;}

        /*
         * Ví dụ:
         *
         * stream: Eicar-Test-Signature FOUND
         */
        if (response.contains("FOUND")) {
            throw new VirusDetectedException("Phát hiện file nguy hiểm: " + originalFilename);
        }

        /*
         * Ví dụ ClamAV báo lỗi:
         *
         * stream: ... ERROR
         */
        throw new VirusScanException(
                "ClamAV scan lỗi: " + response
        );
    }
}