package com.dtthuan3.ecommerce.inventoryservice.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * Cổng xác thực chạy trước mọi RPC của Inventory.
 * Class lấy Bearer token từ metadata, xác minh token bằng JwtDecoder và JwtAuthenticationConverter dùng chung
 * với REST, rồi đưa Authentication hợp lệ vào gRPC Context để handler đọc thông tin user/role. Token thiếu
 * hoặc không hợp lệ bị từ chối ngay tại đây; class này không quyết định quyền nghiệp vụ cụ thể của từng RPC.
 */
@Component
@GrpcGlobalServerInterceptor
@RequiredArgsConstructor
public class InventoryGrpcJwtAuthenticationInterceptor implements ServerInterceptor {

    /** Metadata key chuẩn của HTTP Authorization được gửi cùng gRPC call. */
    private static final Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key.of(
            "authorization", Metadata.ASCII_STRING_MARSHALLER);

    /** Chuyển principal đã xác thực qua gRPC Context để service kiểm tra role và audit actor. */
    static final Context.Key<Authentication> AUTHENTICATION_CONTEXT = Context.key("inventory-authentication");

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public <RequestT, ResponseT> ServerCall.Listener<RequestT> interceptCall(
            ServerCall<RequestT, ResponseT> call,
            Metadata headers,
            ServerCallHandler<RequestT, ResponseT> next) {
        // Xác thực tại interceptor chung để mọi RPC có cùng chính sách, không phụ thuộc từng handler tự nhớ kiểm
        // tra token.
        String authorization = headers.get(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return reject(call, Status.UNAUTHENTICATED.withDescription("Thiếu Bearer token hợp lệ"));
        }

        try {
            String tokenValue = authorization.substring("Bearer ".length()).trim();
            if (tokenValue.isEmpty()) {
                return reject(call, Status.UNAUTHENTICATED.withDescription("Bearer token không được để trống"));
            }
            Jwt jwt = jwtDecoder.decode(tokenValue);
            Authentication authentication = jwtAuthenticationConverter.convert(jwt);
            if (authentication == null || !authentication.isAuthenticated()) {
                return reject(call, Status.UNAUTHENTICATED.withDescription("Không xác thực được JWT"));
            }

            // Context gắn principal vào đúng RPC và theo luồng khi gRPC đổi thread; không cần ThreadLocal dùng chung.
            Context authenticatedContext = Context.current().withValue(AUTHENTICATION_CONTEXT, authentication);
            return Contexts.interceptCall(authenticatedContext, call, headers, next);
        } catch (JwtException exception) {
            // Không ghi token vào log hoặc trả chi tiết xác minh chữ ký cho client.
            return reject(call, Status.UNAUTHENTICATED.withDescription("Bearer token không hợp lệ hoặc đã hết hạn"));
        }
    }

    private static <RequestT, ResponseT> ServerCall.Listener<RequestT> reject(
            ServerCall<RequestT, ResponseT> call, Status status) {
        // Từ chối trước khi gọi handler và không trả chi tiết chữ ký/token ra client.
        call.close(status, new Metadata());
        return new ServerCall.Listener<>() {};
    }
}
