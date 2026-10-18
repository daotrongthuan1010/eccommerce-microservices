//package com.dtthuan3.ecommerce.catalogservice.entity;
//
//import com.dtthuan3.ecommerce.catalogservice.contstant.ReindexRunStatus;
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
//        name = "reindex_runs",
//        indexes = {
//                @Index(name = "idx_reindex_runs_status", columnList = "status")
//        }
//)
//@Getter
//@Setter
//public class ReindexRun extends BaseEntity {
//
//    /** ID duy nhất của lần reindex. */
//    @Id
//    @GeneratedValue(generator = "reindex_run_uuid_generator")
//    @GenericGenerator(
//            name = "reindex_run_uuid_generator",
//            type = StringUuidSequenceGenerator.class,
//            parameters = {
//                    @Parameter(
//                            name = "sequence_name",
//                            value = "reindex_runs_id_seq"
//                    )
//            }
//    )
//    @Column(name = "id", nullable = false, updatable = false, length = 36)
//    private String id;
//
//    /** Tên Elasticsearch index nguồn. */
//    @Column(name = "source_index", nullable = false, length = 255)
//    private String sourceIndex;
//
//    /** Tên Elasticsearch index đích. */
//    @Column(name = "target_index", nullable = false, length = 255)
//    private String targetIndex;
//
//    /** ID checkpoint dùng cho lần reindex. */
//    @Column(name = "checkpoint_id", length = 36)
//    private String checkpointId;
//
//    /** Trạng thái của quá trình reindex. */
//    @Enumerated(EnumType.STRING)
//    @Column(name = "status", nullable = false, length = 30)
//    private ReindexRunStatus status;
//
//    /** Thời điểm bắt đầu tạo snapshot. */
//    @Column(name = "snapshot_started_at")
//    private LocalDateTime snapshotStartedAt;
//
//    /** Thời điểm hoàn thành snapshot. */
//    @Column(name = "snapshot_finished_at")
//    private LocalDateTime snapshotFinishedAt;
//
//    /** Thời điểm chuyển alias sang index mới. */
//    @Column(name = "alias_switched_at")
//    private LocalDateTime aliasSwitchedAt;
//
//    /** Tổng số document cần reindex. */
//    @Column(name = "total_documents")
//    private Long totalDocuments;
//
//    /** Số document đã kiểm tra thành công. */
//    @Column(name = "verified_documents")
//    private Long verifiedDocuments;
//
//    /** Thông báo lỗi của quá trình reindex. */
//    @Column(name = "error_message", columnDefinition = "TEXT")
//    private String errorMessage;
//}