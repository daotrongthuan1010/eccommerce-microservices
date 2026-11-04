package com.dtthuan3.ecommerce.inventoryservice.grpc;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CommitInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.DispatchInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReleaseInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryReservationResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.StockMovementResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReservationService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryWarehouseAuthorizationService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Đầu mối xử lý các RPC Inventory được service nội bộ gọi qua gRPC.
 * Class này kiểm tra request, role, chuyển protobuf sang DTO nghiệp vụ, gọi InventoryService hoặc
 * InventoryReservationService, rồi đổi kết quả và lỗi sang định dạng gRPC. Class không tự truy vấn DB
 * và không tự thực hiện quy tắc cập nhật tồn; các phần đó thuộc service nghiệp vụ và repository.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryService inventoryService;
    private final InventoryReservationService reservationService;
    private final InventoryWarehouseAuthorizationService warehouseAuthorizationService;

    @Value("${inventory.grpc.max-page-size}")
    private int maxPageSize;

    /** Tra cứu một bucket tồn theo ID; toàn bộ quy tắc tìm kiếm vẫn dùng InventoryService hiện có. */
    @Override
    public void getInventoryById(
            GetInventoryByIdRequest request,
            StreamObserver<GetInventoryByIdResponse> responseObserver) {
        if (!requireRoles(responseObserver, "INVENTORY_READ", "ADMIN")) return;
        if (isBlank(request.getInventoryId())) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("inventory_id là bắt buộc")
                    .asRuntimeException());
            return;
        }
        if (request.getInventoryId().length() > 100) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("inventory_id không được vượt quá 100 ký tự")
                    .asRuntimeException());
            return;
        }

        try {
            InventoryResponse inventory = inventoryService.getById(request.getInventoryId().trim());
            warehouseAuthorizationService.requireWarehouseAccess(inventory.warehouseId(), currentAuthentication());
            responseObserver.onNext(GetInventoryByIdResponse.newBuilder()
                    .setInventory(toProto(inventory))
                    .build());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception, "Không thể tra cứu Inventory theo ID"));
        }
    }

    /** Tra cứu tồn theo bộ lọc tùy chọn và giới hạn kích thước trang để bảo vệ database. */
    @Override
    public void getAvailableInventory(
            GetAvailableInventoryRequest request,
            StreamObserver<GetAvailableInventoryResponse> responseObserver) {
        if (!requireRoles(responseObserver, "INVENTORY_READ", "ADMIN")) return;
        if (request.getPage() < 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("page phải lớn hơn hoặc bằng 0")
                    .asRuntimeException());
            return;
        }
        if (request.getPageSize() < 1 || request.getPageSize() > maxPageSize) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("page_size phải nằm trong khoảng từ 1 đến " + maxPageSize)
                    .asRuntimeException());
            return;
        }

        try {
            Page<InventoryResponse> result = inventoryService.available(
                    emptyToNull(request.getSku()),
                    emptyToNull(request.getWarehouseId()),
                    emptyToNull(request.getLocationId()),
                    PageRequest.of(request.getPage(), request.getPageSize()));

            GetAvailableInventoryResponse.Builder response = GetAvailableInventoryResponse.newBuilder()
                    .setTotalElements(result.getTotalElements())
                    .setTotalPages(result.getTotalPages())
                    .setPage(result.getNumber())
                    .setPageSize(result.getSize())
                    .setHasNext(result.hasNext());

            result.getContent().forEach(item -> warehouseAuthorizationService.requireWarehouseAccess(
                    item.warehouseId(), currentAuthentication()));
            result.getContent().stream()
                    .map(InventoryGrpcService::toProto)
                    .forEach(response::addItems);

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception, "Không thể tra cứu tồn khả dụng"));
        }
    }

    /** Tra cứu đúng một bucket; đủ bốn thành phần khóa để không trả nhầm tồn của vị trí khác. */
    @Override
    public void getInventoryByPosition(
            GetInventoryByPositionRequest request,
            StreamObserver<GetInventoryByPositionResponse> responseObserver) {
        if (!requireRoles(responseObserver, "INVENTORY_READ", "ADMIN")) return;
        if (isBlank(request.getEnterpriseId()) || isBlank(request.getWarehouseId())
                || isBlank(request.getLocationId()) || isBlank(request.getSku())) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("enterprise_id, warehouse_id, location_id và sku là bắt buộc")
                    .asRuntimeException());
            return;
        }

        try {
            warehouseAuthorizationService.requireWarehouseAccess(request.getWarehouseId().trim(), currentAuthentication());
            InventoryResponse inventory = inventoryService.getByPosition(
                    request.getEnterpriseId().trim(),
                    request.getWarehouseId().trim(),
                    request.getLocationId().trim(),
                    request.getSku().trim());
            responseObserver.onNext(GetInventoryByPositionResponse.newBuilder()
                    .setInventory(toProto(inventory))
                    .build());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception, "Không thể tra cứu Inventory theo vị trí"));
        }
    }

    /** Đọc trạng thái reservation để caller đối soát sau timeout mà không gửi lại lệnh ghi tồn. */
    @Override
    public void getReservation(
            GetReservationRequest request,
            StreamObserver<GetReservationResponse> responseObserver) {
        if (!requireRoles(responseObserver, "INVENTORY_READ_ALL", "ADMIN")) return;
        if (isBlank(request.getReservationId())) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("reservation_id là bắt buộc")
                    .asRuntimeException());
            return;
        }

        try {
            InventoryReservationResponse reservation = reservationService.getById(request.getReservationId().trim());
            requireReservationWarehouseAccess(reservation);
            responseObserver.onNext(GetReservationResponse.newBuilder()
                    .setReservation(toProto(reservation))
                    .build());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception, "Không thể tra cứu reservation"));
        }
    }

    /** Tra cứu lịch sử movement với giới hạn trang và các bộ lọc tùy chọn. */
    @Override
    public void getMovementHistory(
            GetMovementHistoryRequest request,
            StreamObserver<GetMovementHistoryResponse> responseObserver) {
        if (!requireRoles(responseObserver, "INVENTORY_AUDIT_READ", "ADMIN")) return;
        if (request.getPage() < 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("page phải lớn hơn hoặc bằng 0")
                    .asRuntimeException());
            return;
        }
        if (request.getPageSize() < 1 || request.getPageSize() > maxPageSize) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("page_size phải nằm trong khoảng từ 1 đến " + maxPageSize)
                    .asRuntimeException());
            return;
        }
        if (request.getInventoryId().length() > 100 || request.getReferenceId().length() > 120
                || request.getFrom().length() > 32 || request.getTo().length() > 32) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("inventory_id, reference_id, from hoặc to vượt độ dài cho phép")
                    .asRuntimeException());
            return;
        }

        try {
            if (isBlank(request.getInventoryId())) {
                if (!requireRoles(responseObserver, "INVENTORY_READ_ALL", "ADMIN")) return;
            } else {
                warehouseAuthorizationService.requireWarehouseAccess(
                        inventoryService.getById(request.getInventoryId().trim()).warehouseId(), currentAuthentication());
            }
            MovementType movementType = parseMovementType(request.getMovementType());
            LocalDateTime from = parseLocalDateTime(request.getFrom(), "from");
            LocalDateTime to = parseLocalDateTime(request.getTo(), "to");
            Page<StockMovementResponse> result = inventoryService.movementHistory(
                    emptyToNull(request.getInventoryId()), movementType,
                    emptyToNull(request.getReferenceId()), from, to,
                    PageRequest.of(request.getPage(), request.getPageSize()));

            GetMovementHistoryResponse.Builder response = GetMovementHistoryResponse.newBuilder()
                    .setTotalElements(result.getTotalElements())
                    .setTotalPages(result.getTotalPages())
                    .setPage(result.getNumber())
                    .setPageSize(result.getSize())
                    .setHasNext(result.hasNext());
            result.getContent().stream()
                    .map(InventoryGrpcService::toProto)
                    .forEach(response::addMovements);

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception, "Không thể tra cứu lịch sử biến động tồn kho"));
        }
    }

    /** Giữ đồng thời các dòng trong một giao dịch; operation_id giúp retry không giữ hàng lần hai. */
    @Override
    public void reserveInventory(
            ReserveInventoryRequest request,
            StreamObserver<ReserveInventoryResponse> responseObserver) {
        if (!requireRoles(responseObserver, "INVENTORY_FULFILLMENT", "ADMIN")) return;
        if (isBlank(request.getReservationId()) || isBlank(request.getOrderId())
                || isBlank(request.getOperationId()) || isBlank(request.getExpiresAt())) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("reservation_id, order_id, operation_id và expires_at là bắt buộc")
                    .asRuntimeException());
            return;
        }
        if (request.getReservationId().length() > 100 || request.getOrderId().length() > 100
                || request.getOperationId().length() > 120) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("reservation_id/order_id/operation_id vượt độ dài cho phép")
                    .asRuntimeException());
            return;
        }
        if (request.getLinesCount() < 1 || request.getLinesCount() > 200) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("lines phải có từ 1 đến 200 phần tử")
                    .asRuntimeException());
            return;
        }

        try {
            Instant expiresAt = Instant.parse(request.getExpiresAt().trim());
            List<CreateInventoryReservationRequest.Line> lines = request.getLinesList().stream()
                    .map(line -> new CreateInventoryReservationRequest.Line(
                            requireText(line.getInventoryId(), "inventory_id"),
                            positiveDecimal(line.getQuantity())))
                    .toList();
            CreateInventoryReservationRequest command = new CreateInventoryReservationRequest(
                    request.getReservationId().trim(),
                    request.getOrderId().trim(),
                    request.getOperationId().trim(),
                    expiresAt,
                    lines);

            lines.stream().map(line -> inventoryService.getById(line.inventoryId()).warehouseId()).distinct()
                    .forEach(warehouseId -> warehouseAuthorizationService.requireWarehouseAccess(
                            warehouseId, currentAuthentication()));

            Authentication authentication = currentAuthentication();
            InventoryReservationResponse reservation = reservationService.reserve(command, authentication.getName());
            responseObserver.onNext(ReserveInventoryResponse.newBuilder()
                    .setReservation(toProto(reservation))
                    .build());
            responseObserver.onCompleted();
        } catch (DateTimeParseException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("expires_at phải là thời gian ISO-8601 UTC hợp lệ")
                    .withCause(exception).asRuntimeException());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception).asRuntimeException());
        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception, "Không thể tạo reservation"));
        }
    }

    /** Commit reservation HELD sau khi Order đã kiểm chứng thanh toán và gửi đúng version hiện tại. */
    @Override
    public void commitReservation(
            CommitReservationRequest request,
            StreamObserver<CommitReservationResponse> responseObserver) {
        if (!requireRoles(responseObserver, "INVENTORY_FULFILLMENT", "ADMIN")) return;
        if (isBlank(request.getReservationId()) || isBlank(request.getOperationId())
                || isBlank(request.getOrderId()) || isBlank(request.getVerifiedPaymentReference())) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("reservation_id, operation_id, order_id và verified_payment_reference là bắt buộc")
                    .asRuntimeException());
            return;
        }
        if (request.getReservationId().length() > 100 || request.getOperationId().length() > 120
                || request.getOrderId().length() > 100 || request.getVerifiedPaymentReference().length() > 150) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Một trong các trường ID hoặc payment reference vượt độ dài cho phép")
                    .asRuntimeException());
            return;
        }
        if (request.getExpectedVersion() < 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("expected_version phải lớn hơn hoặc bằng 0")
                    .asRuntimeException());
            return;
        }

        try {
            requireReservationWarehouseAccess(reservationService.getById(request.getReservationId().trim()));
            CommitInventoryReservationRequest command = new CommitInventoryReservationRequest(
                    request.getOperationId().trim(),
                    request.getOrderId().trim(),
                    request.getVerifiedPaymentReference().trim(),
                    request.getExpectedVersion());
            InventoryReservationResponse reservation = reservationService.commit(
                    request.getReservationId().trim(), command, currentAuthentication().getName());
            responseObserver.onNext(CommitReservationResponse.newBuilder()
                    .setReservation(toProto(reservation))
                    .build());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception, "Không thể commit reservation"));
        }
    }

    /** Nhả lượng giữ còn lại hoặc lượng chỉ định; lines rỗng nghĩa là nhả toàn bộ residual. */
    @Override
    public void releaseReservation(
            ReleaseReservationRequest request,
            StreamObserver<ReleaseReservationResponse> responseObserver) {
        if (!requireRoles(responseObserver, "INVENTORY_FULFILLMENT", "ADMIN")) return;
        if (isBlank(request.getReservationId()) || isBlank(request.getOperationId()) || isBlank(request.getReason())) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("reservation_id, operation_id và reason là bắt buộc")
                    .asRuntimeException());
            return;
        }
        if (request.getReservationId().length() > 100 || request.getOperationId().length() > 120
                || request.getReason().length() > 200) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("reservation_id, operation_id hoặc reason vượt độ dài cho phép")
                    .asRuntimeException());
            return;
        }
        if (request.getExpectedVersion() < 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("expected_version phải lớn hơn hoặc bằng 0")
                    .asRuntimeException());
            return;
        }
        if (request.getLinesCount() > 200) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("lines không được vượt quá 200 phần tử")
                    .asRuntimeException());
            return;
        }

        try {
            requireReservationWarehouseAccess(reservationService.getById(request.getReservationId().trim()));
            List<ReleaseInventoryReservationRequest.Line> lines = request.getLinesList().stream()
                    .map(line -> new ReleaseInventoryReservationRequest.Line(
                            requireText(line.getReservationLineId(), "reservation_line_id"),
                            positiveDecimal(line.getQuantity())))
                    .toList();
            ReleaseInventoryReservationRequest command = new ReleaseInventoryReservationRequest(
                    request.getOperationId().trim(), request.getReason().trim(), request.getExpectedVersion(),
                    lines.isEmpty() ? null : lines);
            InventoryReservationResponse reservation = reservationService.release(
                    request.getReservationId().trim(), command, currentAuthentication().getName());
            responseObserver.onNext(ReleaseReservationResponse.newBuilder()
                    .setReservation(toProto(reservation))
                    .build());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception, "Không thể release reservation"));
        }
    }

    /** Xuất lượng đã commit cho shipment; service kiểm tra ownership, residual và idempotency. */
    @Override
    public void dispatchReservation(
            DispatchReservationRequest request,
            StreamObserver<DispatchReservationResponse> responseObserver) {
        if (!requireRoles(responseObserver, "INVENTORY_FULFILLMENT", "ADMIN")) return;
        if (isBlank(request.getReservationId()) || isBlank(request.getOperationId())
                || isBlank(request.getShipmentId()) || isBlank(request.getOrderId())) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("reservation_id, operation_id, shipment_id và order_id là bắt buộc")
                    .asRuntimeException());
            return;
        }
        if (request.getReservationId().length() > 100 || request.getOperationId().length() > 120
                || request.getShipmentId().length() > 120 || request.getOrderId().length() > 100) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Một trong các ID vượt độ dài cho phép")
                    .asRuntimeException());
            return;
        }
        if (request.getExpectedVersion() < 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("expected_version phải lớn hơn hoặc bằng 0")
                    .asRuntimeException());
            return;
        }
        if (request.getLinesCount() == 0 || request.getLinesCount() > 200) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("lines phải có từ 1 đến 200 phần tử")
                    .asRuntimeException());
            return;
        }

        try {
            requireReservationWarehouseAccess(reservationService.getById(request.getReservationId().trim()));
            List<DispatchInventoryReservationRequest.Line> lines = request.getLinesList().stream()
                    .map(line -> {
                        String reservationLineId = requireText(line.getReservationLineId(), "reservation_line_id");
                        if (reservationLineId.length() > 100) {
                            throw new IllegalArgumentException("reservation_line_id vượt quá 100 ký tự");
                        }
                        return new DispatchInventoryReservationRequest.Line(
                                reservationLineId, positiveDecimal(line.getQuantity()));
                    })
                    .toList();
            DispatchInventoryReservationRequest command = new DispatchInventoryReservationRequest(
                    request.getOperationId().trim(), request.getShipmentId().trim(), request.getOrderId().trim(),
                    request.getExpectedVersion(), lines);
            InventoryReservationResponse reservation = reservationService.dispatch(
                    request.getReservationId().trim(), command, currentAuthentication().getName());
            responseObserver.onNext(DispatchReservationResponse.newBuilder()
                    .setReservation(toProto(reservation))
                    .build());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception, "Không thể dispatch reservation"));
        }
    }

    /** Chỉ cho phép role tương ứng với chính sách REST; gRPC không tự áp dụng @PreAuthorize. */
    private static boolean requireRoles(StreamObserver<?> responseObserver, String... allowedRoles) {
        // Interceptor đảm nhiệm authentication; bước này là authorization theo role của từng RPC cụ thể.
        Authentication authentication = currentAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("Yêu cầu Bearer token đã xác thực")
                    .asRuntimeException());
            return false;
        }
        boolean allowed = Arrays.stream(allowedRoles).anyMatch(role -> authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role)));
        if (!allowed) {
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription("Tài khoản không có quyền thực hiện RPC này")
                    .asRuntimeException());
        }
        return allowed;
    }

    private static Authentication currentAuthentication() {
        return InventoryGrpcJwtAuthenticationInterceptor.AUTHENTICATION_CONTEXT.get();
    }

    /** Kiểm tra mọi kho chứa các dòng hàng của reservation trước khi đọc hoặc thay đổi trạng thái. */
    private void requireReservationWarehouseAccess(InventoryReservationResponse reservation) {
        reservation.lines().stream().map(line -> inventoryService.getById(line.inventoryId()).warehouseId())
                .distinct().forEach(warehouseId -> warehouseAuthorizationService.requireWarehouseAccess(
                        warehouseId, currentAuthentication()));
    }

    private static String requireText(String value, String fieldName) {
        if (isBlank(value)) throw new IllegalArgumentException(fieldName + " không được để trống");
        return value.trim();
    }

    private static BigDecimal positiveDecimal(String value) {
        try {
            BigDecimal quantity = new BigDecimal(value.trim());
            if (quantity.signum() <= 0) throw new IllegalArgumentException("quantity phải lớn hơn 0");
            return quantity;
        } catch (NumberFormatException | NullPointerException exception) {
            throw new IllegalArgumentException("quantity phải là số thập phân hợp lệ", exception);
        }
    }

    /** Chuyển enum filter theo tên không phân biệt chữ hoa/thường; chuỗi rỗng là không lọc. */
    private static MovementType parseMovementType(String value) {
        String normalized = emptyToNull(value);
        if (normalized == null) return null;
        try {
            return MovementType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("movement_type không hợp lệ: " + normalized, exception);
        }
    }

    /** Parse thời gian theo kiểu LocalDateTime mà repository đang dùng, không tự đổi timezone. */
    private static LocalDateTime parseLocalDateTime(String value, String fieldName) {
        String normalized = emptyToNull(value);
        if (normalized == null) return null;
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " phải theo ISO LocalDateTime, ví dụ 2026-10-26T10:30:00", exception);
        }
    }

    /** Chuyển exception dùng chung của dự án thành status gRPC phù hợp, không làm lộ lỗi server. */
    private static StatusRuntimeException toGrpcError(RuntimeException exception, String internalMessage) {
        if (exception instanceof BusinessException businessException) {
            // Giữ nhóm lỗi nghiệp vụ cho caller xử lý; lỗi nội bộ chỉ trả thông báo chung để không lộ SQL/stack trace.
            HttpStatus httpStatus = businessException.getStatus();
            Status status = switch (httpStatus.value()) {
                case 400, 422 -> Status.INVALID_ARGUMENT;
                case 401 -> Status.UNAUTHENTICATED;
                case 403 -> Status.PERMISSION_DENIED;
                case 404 -> Status.NOT_FOUND;
                case 409 -> Status.FAILED_PRECONDITION;
                case 429 -> Status.RESOURCE_EXHAUSTED;
                case 503 -> Status.UNAVAILABLE;
                default -> Status.INTERNAL;
            };
            String description = status.getCode() == Status.Code.INTERNAL
                    ? internalMessage : businessException.getMessage();
            if (status.getCode() == Status.Code.INTERNAL) {
                log.error("Lỗi nghiệp vụ nội bộ khi xử lý gRPC Inventory", exception);
            }
            return status.withDescription(description).withCause(exception).asRuntimeException();
        }
        if (exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(exception.getMessage())
                    .withCause(exception).asRuntimeException();
        }
        // Ghi đầy đủ lỗi ở server để truy vết; client chỉ nhận mô tả chung, không lộ nội bộ.
        log.error("Lỗi không dự kiến khi xử lý gRPC Inventory", exception);
        return Status.INTERNAL.withDescription(internalMessage).withCause(exception).asRuntimeException();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Chuyển chuỗi trống của protobuf thành null để giữ quy ước bộ lọc tùy chọn của REST/service. */
    private static String emptyToNull(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    /** Chuyển DTO nội bộ sang protobuf; số lượng BigDecimal được xuất dưới dạng chuỗi chính xác. */
    private static InventoryItem toProto(InventoryResponse inventory) {
        InventoryItem.Builder item = InventoryItem.newBuilder()
                .setId(valueOrEmpty(inventory.id()))
                .setEnterpriseId(valueOrEmpty(inventory.enterpriseId()))
                .setWarehouseId(valueOrEmpty(inventory.warehouseId()))
                .setLocationId(valueOrEmpty(inventory.locationId()))
                .setSku(valueOrEmpty(inventory.sku()))
                .setPhysicalQuantity(decimalOrEmpty(inventory.physicalQuantity()))
                .setReservedQuantity(decimalOrEmpty(inventory.reservedQuantity()))
                .setDefectiveQuantity(decimalOrEmpty(inventory.defectiveQuantity()))
                .setPendingQuantity(decimalOrEmpty(inventory.pendingQuantity()))
                .setInTransitQuantity(decimalOrEmpty(inventory.inTransitQuantity()))
                .setAvailableQuantity(decimalOrEmpty(inventory.availableQuantity()))
                .setCreatedAt(valueOrEmpty(inventory.createdAt()))
                .setUpdatedAt(valueOrEmpty(inventory.updatedAt()));
        if (inventory.version() != null) {
            item.setVersion(inventory.version());
        }
        return item.build();
    }

    /** Chuyển bản ghi movement sang protobuf, giữ các giá trị decimal chính xác dưới dạng chuỗi. */
    private static StockMovementItem toProto(StockMovementResponse movement) {
        return StockMovementItem.newBuilder()
                .setId(valueOrEmpty(movement.id()))
                .setInventoryId(valueOrEmpty(movement.inventoryId()))
                .setMovementType(valueOrEmpty(movement.movementType()))
                .setQuantity(decimalOrEmpty(movement.quantity()))
                .setPhysicalBefore(decimalOrEmpty(movement.physicalBefore()))
                .setPhysicalAfter(decimalOrEmpty(movement.physicalAfter()))
                .setReservedBefore(decimalOrEmpty(movement.reservedBefore()))
                .setReservedAfter(decimalOrEmpty(movement.reservedAfter()))
                .setReferenceType(valueOrEmpty(movement.referenceType()))
                .setReferenceId(valueOrEmpty(movement.referenceId()))
                .setReason(valueOrEmpty(movement.reason()))
                .setPerformedBy(valueOrEmpty(movement.performedBy()))
                .setPerformedBySubject(valueOrEmpty(movement.performedBySubject()))
                .setCreatedAt(valueOrEmpty(movement.createdAt()))
                .setDefectiveBefore(decimalOrEmpty(movement.defectiveBefore()))
                .setDefectiveAfter(decimalOrEmpty(movement.defectiveAfter()))
                .setPendingBefore(decimalOrEmpty(movement.pendingBefore()))
                .setPendingAfter(decimalOrEmpty(movement.pendingAfter()))
                .setInTransitBefore(decimalOrEmpty(movement.inTransitBefore()))
                .setInTransitAfter(decimalOrEmpty(movement.inTransitAfter()))
                .setInventoryVersionBefore(valueOrEmpty(movement.inventoryVersionBefore() == null
                        ? null : movement.inventoryVersionBefore().toString()))
                .setInventoryVersionAfter(valueOrEmpty(movement.inventoryVersionAfter() == null
                        ? null : movement.inventoryVersionAfter().toString()))
                .build();
    }

    /** Chuyển header và tiến độ từng dòng reservation sang hợp đồng protobuf. */
    private static InventoryReservation toProto(InventoryReservationResponse reservation) {
        InventoryReservation.Builder result = InventoryReservation.newBuilder()
                .setReservationId(valueOrEmpty(reservation.reservationId()))
                .setOrderId(valueOrEmpty(reservation.orderId()))
                .setEnterpriseId(valueOrEmpty(reservation.enterpriseId()))
                .setStatus(valueOrEmpty(reservation.status()))
                .setExpiresAt(valueOrEmpty(reservation.expiresAt()))
                .setVersion(reservation.version())
                .setCreatedBy(valueOrEmpty(reservation.createdBy()))
                .setCreatedAt(valueOrEmpty(reservation.createdAt()))
                .setCommittedBy(valueOrEmpty(reservation.committedBy()))
                .setCommittedAt(valueOrEmpty(reservation.committedAt()));

        reservation.lines().stream()
                .map(InventoryGrpcService::toProto)
                .forEach(result::addLines);
        return result.build();
    }

    /** Chuyển số lượng trong một dòng reservation thành chuỗi decimal chính xác. */
    private static InventoryReservationLine toProto(InventoryReservationResponse.Line line) {
        return InventoryReservationLine.newBuilder()
                .setReservationLineId(valueOrEmpty(line.reservationLineId()))
                .setLineNumber(line.lineNumber())
                .setInventoryId(valueOrEmpty(line.inventoryId()))
                .setSku(valueOrEmpty(line.sku()))
                .setRequestedQuantity(decimalOrEmpty(line.requestedQuantity()))
                .setDispatchedQuantity(decimalOrEmpty(line.dispatchedQuantity()))
                .setReleasedQuantity(decimalOrEmpty(line.releasedQuantity()))
                .setRemainingQuantity(decimalOrEmpty(line.remainingQuantity()))
                .build();
    }

    private static String decimalOrEmpty(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static String valueOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }
}
