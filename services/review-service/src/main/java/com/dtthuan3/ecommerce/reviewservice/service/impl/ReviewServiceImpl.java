package com.dtthuan3.ecommerce.reviewservice.service.impl;

import com.dtthuan3.ecommerce.reviewservice.Mapper.ReviewMapper;
import com.dtthuan3.ecommerce.reviewservice.Mapper.ReviewMediaMapper;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewCreateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewMediaRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewUpdateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.SellerReplyRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.HelpfulResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewListResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewSummaryResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.entity.ReviewHelpful;
import com.dtthuan3.ecommerce.reviewservice.entity.ReviewMedia;
import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewMediaType;
import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewHelpfulRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewMediaRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewRepository;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final ReviewMediaMapper reviewMediaMapper;
    private final ReviewMapper reviewMapper;
    @Override
    public Page<ReviewListResponse> getReviewByProduct(Long productId, Integer rating, int page, int size) {
          Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
          Page<Review> reviews;
          if(rating!=null){
               reviews= reviewRepository.findByProductIdAndRatingAndStatus(productId,rating,ReviewStatus.PUBLISHED,pageable);
          }
          else{
              reviews= reviewRepository.findByProductIdAndStatus(productId,ReviewStatus.PUBLISHED,pageable);
          }
          return reviews.map(reviewMapper::toListResponse);
    }

    @Override
    public Page<ReviewResponse> getMyReview(Long userId, int page, int size) {
        Pageable pageable=PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviews=reviewRepository.findByUserId(userId,pageable);
        return reviews.map(reviewMapper::toResponse);
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewCreateRequest request) {
        if(reviewRepository.existsByUserIdAndOrderItemId(userId,request.getOrderItemId())){
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi");
        }
        // giả lập thông tin
        Long orderId = 1L;
        Long productId = 1L;
        Long variantId = null;
        Review review = reviewMapper.toEntity(request,userId,orderId,productId,variantId);
        Review save=reviewRepository.save(review);
        //chưa có minio
        if(request.getMedia()!=null){
            saveMedia(save.getId(),request.getMedia());
        }
        return reviewMapper.toResponse(save);

    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long userId, ReviewUpdateRequest request) {
        Review review=findReview(reviewId);
        if(!review.getUserId().equals(userId)){
            throw new RuntimeException("Bạn không có quyền sửa reivew này");
        }
        if(review.getStatus()!=ReviewStatus.PUBLISHED){
            throw new RuntimeException("Không thể sửa review này");
        }
        if(request.getRating()!=null)review.setRating(request.getRating());
        if(request.getComment()!=null){
            String comment = request.getComment().trim();
            review.setComment(comment.isEmpty() ? null : comment);
        }
        if(request.getAnonymous() != null)review.setAnonymous(request.getAnonymous());
        review.setEdited(true);
        Review save=reviewRepository.save(review);
        //chưa có minio
        if(request.getMedia()!=null){
            reviewMediaRepository.deleteByReviewId(reviewId);
            saveMedia(reviewId, request.getMedia());
        }
        return reviewMapper.toResponse(save);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
          Review review=findReview(reviewId);
          if(!review.getUserId().equals(userId)) {
              throw new RuntimeException("Bạn không có quyền xoá review này");
          }
          review.setStatus(ReviewStatus.DELETED);
          reviewRepository.save(review);
    }

    @Override
    public ReviewResponse getReviewById(Long reviewId) {
        Review review=findReview(reviewId);
        if(review.getStatus()!=ReviewStatus.PUBLISHED){
            throw new RuntimeException("Review không tồn tại hoặc không hiển thị");
        }
        return reviewMapper.toResponse(review);
    }

    @Override
    public ReviewSummaryResponse getReviewSummary(Long productId) {
        long[] stars = new long[6];
        for (Object[] row : reviewRepository.countRatingsByProductId(productId, ReviewStatus.PUBLISHED)) {
            int rating = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            if (rating >= 1 && rating <= 5) {
                stars[rating] = count;
            }
        }

        double averageRating = reviewRepository.findAverageRating(productId, ReviewStatus.PUBLISHED);

        long withImages = reviewMediaRepository
                .countReviewsWithMedia(productId, ReviewStatus.PUBLISHED, ReviewMediaType.IMAGE);
        long withVideos = reviewMediaRepository
                .countReviewsWithMedia(productId, ReviewStatus.PUBLISHED, ReviewMediaType.VIDEO);

        return reviewMapper.toSummaryResponse(productId, stars, averageRating, withImages, withVideos);
    }

    @Override
    @Transactional
    public ReviewResponse replyReview(Long reviewId, SellerReplyRequest request) {
        Review review=findReview(reviewId);
        if(review.getStatus() != ReviewStatus.PUBLISHED){
            throw new RuntimeException("Không thể phản hồi review này");
        }
        String reply = request.getReply() == null ? "" : request.getReply().trim();
        if(reply.isEmpty()){
            throw new RuntimeException("Nội dung phản hồi không được để trống");
        }
        review.setSellerReply(reply);
        review.setSellerRepliedAt(LocalDateTime.now());
        Review save=reviewRepository.save(review);
        return reviewMapper.toResponse(save);
    }

    @Override
    @Transactional
    public HelpfulResponse toggleHelpful(Long reviewId, Long userId) {
        Review review=findReview(reviewId);
        if(review.getStatus()!=ReviewStatus.PUBLISHED){
            throw new RuntimeException("Không thể thao tác với review này");
        }
        if(review.getUserId().equals(userId)){
            throw new RuntimeException("bạn không thể bấn hữu ích cho reivew của chính mình");
        }
        boolean existed=reviewHelpfulRepository.existsByReviewIdAndUserId(reviewId,userId);
        if(existed){
            reviewHelpfulRepository.deleteByReviewIdAndUserId(reviewId, userId);
        }
        else {
            ReviewHelpful helpful=ReviewHelpful.builder().reviewId(reviewId).userId(userId).build();
            reviewHelpfulRepository.save(helpful);
        }
        long helpfulCount=reviewHelpfulRepository.countByReviewId(reviewId);
        review.setHelpfulCount(helpfulCount);
        reviewRepository.save(review);
        return reviewMapper.toHelpfulResponse(reviewId,helpfulCount,!existed);
    }
    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(()->new RuntimeException("Không thấy review id: "+reviewId));
    }
    private void saveMedia(Long reviewId, List<ReviewMediaRequest> mediaRequests) {
        if (mediaRequests == null || mediaRequests.isEmpty()) {return;}

        List<ReviewMedia> mediaList = new ArrayList<>();
        for (int i = 0; i < mediaRequests.size(); i++) {
            ReviewMediaRequest request = mediaRequests.get(i);
            ReviewMedia media = reviewMediaMapper.toEntity(request, reviewId, i);
            mediaList.add(media);
        }

        reviewMediaRepository.saveAll(mediaList);
    }
}
