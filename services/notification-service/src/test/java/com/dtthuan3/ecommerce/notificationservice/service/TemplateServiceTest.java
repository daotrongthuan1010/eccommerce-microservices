package com.dtthuan3.ecommerce.notificationservice.service;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationTemplate;
import com.dtthuan3.ecommerce.notificationservice.service.TemplateService.RenderResult;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NOTI-02/UC04: template render — bien thay dung, bien thieu khong gui,
 * version cu van render duoc noi dung da duyet.
 */
@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock private com.dtthuan3.ecommerce.notificationservice.repository.NotificationTemplateRepository templates;
    private TemplateService templateService;

    private NotificationTemplate orderConfirmedTemplate() {
        NotificationTemplate t = new NotificationTemplate();
        t.setTemplateKey("order-confirmed");
        t.setVersion(1);
        t.setChannel("EMAIL");
        t.setRequiredVariables("orderCode,displayName,totalAmount");
        t.setSubjectTemplate("Đơn hàng {{orderCode}} đã được xác nhận");
        t.setBodyTemplate("Xin chào {{displayName}},\nĐơn {{orderCode}} tổng {{totalAmount}}.");
        t.setStatus("ACTIVE");
        return t;
    }

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templates);
    }

    private Map<String, Object> fullVars() {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("orderCode", "ORD-1");
        vars.put("displayName", "Nguyễn Văn A");
        vars.put("totalAmount", "250,000 VND");
        return vars;
    }

    @Test
    @DisplayName("Render du bien: subject + body thay dung, co dau tieng Viet")
    void renderFull() {
        RenderResult result = templateService.render(orderConfirmedTemplate(), fullVars());
        assertThat(result).isInstanceOf(RenderResult.Rendered.class);
        RenderResult.Rendered ok = (RenderResult.Rendered) result;
        assertThat(ok.subject()).isEqualTo("Đơn hàng ORD-1 đã được xác nhận");
        assertThat(ok.body()).contains("Xin chào Nguyễn Văn A");
        assertThat(ok.body()).contains("tổng 250,000 VND.");
    }

    @Test
    @DisplayName("Thieu bien bat buoc (displayName): MissingVariables, KHONG render gui sai")
    void missingVariable() {
        Map<String, Object> vars = fullVars();
        vars.remove("displayName");
        RenderResult result = templateService.render(orderConfirmedTemplate(), vars);
        assertThat(result).isInstanceOf(RenderResult.MissingVariables.class);
        assertThat(((RenderResult.MissingVariables) result).missing()).containsExactly("displayName");
    }

    @Test
    @DisplayName("Bien khong bat buoc khong co: giu placeholder, khong coi la thieu")
    void optionalVariableKeptPlaceholder() {
        NotificationTemplate t = orderConfirmedTemplate();
        t.setRequiredVariables("orderCode"); // chi orderCode bat buoc
        Map<String, Object> vars = Map.of("orderCode", "ORD-1");
        RenderResult result = templateService.render(t, vars);
        assertThat(result).isInstanceOf(RenderResult.Rendered.class);
        assertThat(((RenderResult.Rendered) result).body()).contains("{{displayName}}");
    }

    @Test
    @DisplayName("Bien rong/gia tri blank coi la thieu (khong gui 'Xin chào ,')")
    void blankVariableIsMissing() {
        Map<String, Object> vars = fullVars();
        vars.put("displayName", "  ");
        RenderResult result = templateService.render(orderConfirmedTemplate(), vars);
        assertThat(result).isInstanceOf(RenderResult.MissingVariables.class);
    }

    @Test
    @DisplayName("requiredVariables rong: khong yeu cau bien nao, render binh thuong")
    void noRequiredVariables() {
        NotificationTemplate t = orderConfirmedTemplate();
        t.setRequiredVariables("");
        RenderResult result = templateService.render(t, Map.of());
        assertThat(result).isInstanceOf(RenderResult.Rendered.class);
    }

    @Test
    @DisplayName("Bien null trong map: khong lam render fail, giu placeholder")
    void nullValueKept() {
        Map<String, Object> vars = new LinkedHashMap<>(fullVars());
        vars.put("displayName", null);
        NotificationTemplate t = orderConfirmedTemplate();
        t.setRequiredVariables("orderCode,totalAmount");
        RenderResult result = templateService.render(t, vars);
        assertThat(result).isInstanceOf(RenderResult.Rendered.class);
        assertThat(((RenderResult.Rendered) result).body()).contains("{{displayName}}");
    }
}
