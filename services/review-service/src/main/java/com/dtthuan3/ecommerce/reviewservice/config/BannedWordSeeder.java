package com.dtthuan3.ecommerce.reviewservice.config;

import com.dtthuan3.ecommerce.reviewservice.entity.BannedWord;
import com.dtthuan3.ecommerce.reviewservice.repository.BannedWordRepository;
import com.dtthuan3.ecommerce.reviewservice.service.BannedWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Seed từ cấm mặc định khi bảng còn rỗng (chạy cuối, lỗi DB thì chỉ log warn). */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class BannedWordSeeder implements ApplicationRunner {

    /** Từ cấm tiếng Việt/Anh phổ biến (lưu lowercase; so khớp substring, không phân biệt hoa thường). */
    private static final List<String> DEFAULT_WORDS = List.of(
            "đồ ngốc", "ngu ngốc", "óc chó", "não bò",
            "súc vật", "súc sinh", "khốn nạn", "chó đểu",
            "bố mày", "cút",
            "lồn", "đụ", "đéo", "đm", "vcl", "vãi", "cc", "clmm",
            "stupid", "idiot", "dumb", "fuck", "shit", "bitch");

    private final BannedWordRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (repository.count() > 0) {
                return; // đã có dữ liệu (admin tự quản) -> không seed
            }
            List<BannedWord> seeds = DEFAULT_WORDS.stream()
                    .map(BannedWordService::normalize)
                    .filter(w -> !w.isEmpty())
                    .distinct()
                    .map(w -> BannedWord.builder().word(w).createdBy("seeder").build())
                    .toList();
            repository.saveAll(seeds);
            log.info("Đã seed {} từ cấm mặc định", seeds.size());
        } catch (Exception e) {
            log.warn("Bỏ qua seed banned_words (DB chưa sẵn?): {}", e.getMessage());
        }
    }
}
