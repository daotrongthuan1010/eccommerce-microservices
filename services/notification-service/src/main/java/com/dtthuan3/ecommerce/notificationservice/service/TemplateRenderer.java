package com.dtthuan3.ecommerce.notificationservice.service;

import java.util.Map;

/**
 * Template engine toi thieu (NOTI-02): thay bien {{key}} tu event payload.
 * Bien khong co gia tri thi giu nguyen placeholder de nhan vien thay duoc (khong gui noi dung sai).
 */
public final class TemplateRenderer {

    private TemplateRenderer() {}

    public static String render(String template, Map<String, Object> variables) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() == null) continue;
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
