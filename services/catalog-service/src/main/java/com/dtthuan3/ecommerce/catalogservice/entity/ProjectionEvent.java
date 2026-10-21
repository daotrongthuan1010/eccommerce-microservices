//package com.dtthuan3.ecommerce.catalogservice.entity;
//
//import com.dtthuan3.ecommerce.catalogservice.contstant.ProjectionEventStatus;
//import com.dtthuan3.ecommerce.catalogservice.contstant.SourceType;
//import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.annotations.GenericGenerator;
//import org.hibernate.annotations.Parameter;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(
//        name = "projection_events",
//        indexes = {
//                @Index(name = "idx_projection_events_event_id", columnList = "event_id"),
//                @Index(name = "idx_projection_events_aggregate", columnList = "aggregate_id"),
//                @Index(name = "idx_projection_events_status", columnList = "status")
//        }
//)
//@Getter
//@Setter
//public class ProjectionEvent extends BaseEntity {
//
//    /** ID duy nhất của bản ghi projection event. */
//    @Id
//    @GeneratedValue(generator = "projection_event_uuid_generator")
//    @GenericGenerator(
//            name = "projection_event_uuid_generator",
//            type = StringUuidSequenceGenerator.class,
//            parameters = {
//                    @Parameter(
//                            name = "sequence_name",
//                            value = "projection_events_id_seq"
//                    )
//            }
//    )
//    @Column(name = "id", nullable = false, updatable = false, length = 36)
//    private String id;
//
//    /** ID duy nhất của event Kafka hoặc domain event. */
//    @Column(name = "event_id", nullable = false, length = 36)
//    private String eventId;
//
//    /** Nguồn dữ liệu phát sinh event. */
//    @Enumerated(EnumType.STRING)
//    @Column(name = "source_type", nullable = false, length = 50)
//    private SourceType sourceType;
//
//    /** ID aggregate mà event tác động đến. */
//    @Column(name = "aggregate_id", nullable = false, length = 36)
//    private String aggregateId;
//
//    /** Version của aggregate tại thời điểm phát sinh event. */
//    @Column(name = "source_version", nullable = false)
//    private Long sourceVersion;
//
//    /** Tên loại event, ví dụ ProductCreated hoặc CategoryUpdated. */
//    @Column(name = "event_type", nullable = false, length = 150)
//    private String eventType;
//
//    /** Trạng thái xử lý projection event. */
//    @Enumerated(EnumType.STRING)
//    @Column(name = "status", nullable = false, length = 30)
//    private ProjectionEventStatus status;
//
//    /** Số lần xử lý lại event khi thất bại. */
//    @Column(name = "retry_count", nullable = false)
//    private Integer retryCount = 0;
//
//    /** Lỗi gần nhất khi xử lý event. */
//    @Column(name = "last_error", columnDefinition = "TEXT")
//    private String lastError;
//
//    /** Thời điểm event được phát sinh. */
//    @Column(name = "occurred_at", nullable = false)
//    private LocalDateTime occurredAt;
//
//    /** Thời điểm projection xử lý event thành công. */
//    @Column(name = "applied_at")
//    private LocalDateTime appliedAt;
//}
