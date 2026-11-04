package com.dtthuan3.ecommerce.notificationservice.service;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationTemplate;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Template engine (NOTI-02): render {{bien}} tu bien da chot tren request.
 * Bien bat buoc thieu -> render fail (khong gui noi dung sai, NOTI-UC04 nhanh 1).
 * Bien khong co gia tri: giu nguyen placeholder de nhan vien thay duoc.
 */
@Service
public class TemplateService {

    private final NotificationTemplateRepository templates;

    public TemplateService(NotificationTemplateRepository templates) {
        this.templates = templates;
    }

    /** Ket qua render: subject + body, hoac danh sach bien thieu. */
    public sealed interface RenderResult {
        record Rendered(String subject, String body) implements RenderResult {}
        record MissingVariables(List<String> missing) implements RenderResult {}
        record TemplateUnavailable(String reason) implements RenderResult {}
    }

    /** Lay dung version da chot tren request de render (NOTI-UC04: noi dung khong doi). */
    public RenderResult render(NotificationTemplate template, Map<String, Object> variables) {
        List<String> missing = new ArrayList<>();
        for (String required : template.getRequiredVariables().split(",")) {
            String name = required.trim();
            if (name.isEmpty()) continue;
            Object value = variables.get(name);
            if (value == null || String.valueOf(value).isBlank()) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            return new RenderResult.MissingVariables(missing);
        }
        return new RenderResult.Rendered(
                render(template.getSubjectTemplate(), variables),
                render(template.getBodyTemplate(), variables));
    }

    static String render(String template, Map<String, Object> variables) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() == null) continue;
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
