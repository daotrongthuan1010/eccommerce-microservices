package com.dtthuan3.ecommerce.inventoryservice.grpc;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryReservationResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReservationService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Adapter gRPC cho các service nội bộ cần đọc tồn và trạng thái reservation.
 * Lớp này chỉ chuyển đổi hợp đồng protobuf; quy tắc nghiệp vụ vẫn thuộc các service hiện có.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private static final int MAX_PAGE_SIZE = 100;

    private final InventoryService inventoryService;
    private final InventoryReservationService reservationService;

    /** Tra cứu tồn theo bộ lọc tùy chọn và giới hạn kích thước trang để bảo vệ database. */
    @Override
    public void getAvailableInventory(
            GetAvailableInventoryRequest request,
            StreamObserver<GetAvailableInventoryResponse> responseObserver) {
        if (request.getPage() < 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("page phải lớn hơn hoặc bằng 0")
                    .asRuntimeException());
            return;
        }
        if (request.getPageSize() < 1 || request.getPageSize() > MAX_PAGE_SIZE) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("page_size phải nằm trong khoảng từ 1 đến " + MAX_PAGE_SIZE)
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
        if (isBlank(request.getEnterpriseId()) || isBlank(request.getWarehouseId())
                || isBlank(request.getLocationId()) || isBlank(request.getSku())) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("enterprise_id, warehouse_id, location_id và sku là bắt buộc")
                    .asRuntimeException());
            return;
        }

        try {
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
        if (isBlank(request.getReservationId())) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("reservation_id là bắt buộc")
                    .asRuntimeException());
            return;
        }

        try {
            InventoryReservationResponse reservation = reservationService.getById(request.getReservationId().trim());
            responseObserver.onNext(GetReservationResponse.newBuilder()
                    .setReservation(toProto(reservation))
                    .build());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception, "Không thể tra cứu reservation"));
        }
    }

    /** Chuyển exception dùng chung của dự án thành status gRPC phù hợp, không làm lộ lỗi server. */
    private static StatusRuntimeException toGrpcError(RuntimeException exception, String internalMessage) {
        if (exception instanceof BusinessException businessException) {
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
