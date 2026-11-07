package com.dtthuan3.ecommerce.reviewservice.util;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewMediaRequest;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewMediaType;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Quy tắc nội dung review: giới hạn media và bộ lọc tự động quyết định "đăng ngay" hay "chờ duyệt". */
@Component
public class ReviewRules {

    /** Mặc định: tối đa 5 ảnh, không video, chưa có từ cấm (thêm vào List.of(...) nếu cần). */
    private final int maxImages;
    private final int maxVideos;
    private final List<String> bannedWords;

    public ReviewRules() {
        this(5, 1, List.of());
    }

    /** Dùng cho test với giới hạn tùy ý. */
    public ReviewRules(int maxImages, int maxVideos, List<String> bannedWords) {
        this.maxImages = maxImages;
        this.maxVideos = maxVideos;
        this.bannedWords = bannedWords == null ? List.of() : List.copyOf(bannedWords);
    }

    private static final Pattern URL = Pattern.compile("(?i)(https?://|www\\.)\\S+");
    /** Số điện thoại VN: 0xxxxxxxxx hoặc +84/84xxxxxxxxx. */
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?84|0)\\d{9,10}(?!\\d)");


    /** Kiểm tra media: không trùng file, tối đa N ảnh + M video. Thứ tự hiển thị = thứ tự trong mảng. */
    public List<ReviewMediaRequest> validateMedia(List<ReviewMediaRequest> media) {
        if (media == null || media.isEmpty()) {
            return List.of();
        }
        Set<String> fileIds = new HashSet<>();
        int images = 0;
        int videos = 0;
        for (ReviewMediaRequest item : media) {
            if (item == null || item.getFileId() == null || item.getFileId().isBlank()
                    || item.getMediaType() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thông tin media không hợp lệ");
            }
            if (!fileIds.add(item.getFileId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File không được trùng lặp");
            }
            if (item.getMediaType() == ReviewMediaType.VIDEO) {
                videos++;
            } else {
                images++;
            }
        }
        if (images > maxImages) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY, "Tối đa " + maxImages + " ảnh cho mỗi đánh giá");
        }
        if (videos > maxVideos) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY, "Tối đa " + maxVideos + " video cho mỗi đánh giá");
        }
        return media;
    }

    /**
     * true nếu nội dung cần người kiểm duyệt xem trước (có link, số điện thoại hoặc từ khóa cấm).
     * Nội dung sạch sẽ được đăng ngay (giống Shopee) khi ReviewServiceImpl.AUTO_PUBLISH=true.
     * Dùng danh sách từ cấm tĩnh (cho test); luồng thật dùng overload có List bên dưới.
     */
    public boolean needsModeration(String content) {
        return needsModeration(content, bannedWords);
    }

    /**
     * Overload cho luồng thật: từ cấm lấy động từ DB (qua BannedWordService).
     * So khớp substring, không phân biệt hoa thường (giữ nguyên như bản tĩnh).
     */
    public boolean needsModeration(String content, List<String> dynamicBannedWords) {
        if (!hasText(content)) {
            return false;
        }
        if (URL.matcher(content).find() || PHONE.matcher(content).find()) {
            return true;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        List<String> words = dynamicBannedWords == null ? List.of() : dynamicBannedWords;
        return words.stream()
                .filter(w -> w != null && !w.isBlank())
                .anyMatch(w -> lower.contains(w.toLowerCase(Locale.ROOT)));
    }

    public static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /** Cắt khoảng trắng; chuỗi rỗng -> null. */
    public static String normalize(String s) {
        return hasText(s) ? s.trim() : null;
    }
}
