package com.dtthuan3.ecommerce.reviewservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "review_helpful",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_helpful_review_user",
                        columnNames = {
                                "review_id",
                                "user_id"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewHelpful {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}