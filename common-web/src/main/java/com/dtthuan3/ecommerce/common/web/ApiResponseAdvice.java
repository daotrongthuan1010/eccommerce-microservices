package com.dtthuan3.ecommerce.common.web;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Wrap successful JSON controller responses in the shared ApiResponse envelope. */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null || body instanceof ApiResponse) {
            return body;
        }

        HttpStatusCode status = response.getStatusCode();
        if (status != null && (!status.is2xxSuccessful() || status.value() == 204)) {
            return body;
        }

        if (body instanceof Page<?> page) {
            PageResponse<?> pageResponse = PageResponse.of(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
            return withTraceId(ApiResponse.paged(pageResponse));
        }
        if (status != null && status.value() == 201) {
            return withTraceId(ApiResponse.created(body));
        }
        if (status != null && status.value() == 202) {
            return withTraceId(ApiResponse.accepted(body));
        }
        if (body instanceof java.util.List<?> items) {
            return withTraceId(ApiResponse.list(items));
        }
        return withTraceId(ApiResponse.ok(body));
    }

    private ApiResponse<?> withTraceId(ApiResponse<?> response) {
        response.setTraceId(MDC.get(CorrelationIdFilter.MDC_KEY));
        return response;
    }
}
