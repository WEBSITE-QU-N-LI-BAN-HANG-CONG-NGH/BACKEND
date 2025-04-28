package com.webanhang.team_project.service.review;



import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.repository.ReviewRepository;
import com.webanhang.team_project.dto.review.ReviewRequest;
import com.webanhang.team_project.model.Review;
import com.webanhang.team_project.service.product.IProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService{

    private final ReviewRepository reviewRepository;
    private final IProductService productService;
    private final ProductRepository productRepository;

    @Override
    @Transactional // Đảm bảo các thao tác DB (save review, update product) là một khối duy nhất
    public Review createReview(User user, ReviewRequest reviewRequest) {
        // Tìm sản phẩm bằng ProductRepository
        Product product = productRepository.findById(reviewRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + reviewRequest.getProductId()));

        Review review = new Review();
        review.setContent(reviewRequest.getContent());
        review.setProduct(product);
        review.setUser(user); // User đã được truyền vào từ controller/service gọi đến
        review.setRating(reviewRequest.getRating()); // Nên có validation cho rating (1-5) ở DTO hoặc service
        review.setCreatedAt(LocalDateTime.now());

        // Lưu review mới trước
        Review savedReview = reviewRepository.save(review);

        // Cập nhật thông tin rating của sản phẩm
        updateProductRating(product.getId());

        return savedReview; // Trả về review vừa tạo
    }

    @Override
    public List<Review> getReviewsByProductId(Long productId) {
        // Đảm bảo product tồn tại trước khi lấy review (tùy chọn)
        // productRepository.findById(productId).orElseThrow(() -> new ProductException("Product not found with id: " + productId));
        return reviewRepository.findAllByProductId(productId);
    }

    @Override
    @Transactional
    public Review updateReview(Long reviewId, ReviewRequest reviewRequest, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        // Kiểm tra xem user hiện tại có phải là người tạo review không (hoặc là admin)
        if (!review.getUser().getId().equals(user.getId()) /* && !user.isAdmin() */ ) {
            throw new RuntimeException("User not authorized to update this review");
        }

        // Chỉ cập nhật rating và content
        review.setRating(reviewRequest.getRating()); // Nên validate rating
        review.setContent(reviewRequest.getContent());
        // Không cập nhật createdAt hoặc user, product

        Review updatedReview = reviewRepository.save(review);

        // Cập nhật lại rating của sản phẩm vì rating của review này đã thay đổi
        updateProductRating(review.getProduct().getId());

        return updatedReview;
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        // Kiểm tra quyền xóa
        if (!review.getUser().getId().equals(user.getId()) /* && !user.isAdmin() */) {
            throw new RuntimeException("User not authorized to delete this review");
        }

        Long productId = review.getProduct().getId(); // Lấy productId trước khi xóa

        reviewRepository.delete(review);

        // Cập nhật lại rating của sản phẩm sau khi xóa
        updateProductRating(productId);
    }

    @Override
    public Review getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));
    }

    // --- Hàm private để cập nhật rating cho Product ---
    private void updateProductRating(Long productId) {
        // Tìm lại product để đảm bảo lấy bản mới nhất (quan trọng trong môi trường đa luồng)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id for rating update: " + productId));

        // Tính toán lại từ DB bằng các phương thức trong ReviewRepository
        // (Cần định nghĩa các phương thức này trong ReviewRepository interface)
        Integer newNumRatings = reviewRepository.countByProductId(productId);
        Double newAverageRating = reviewRepository.calculateAverageRatingByProductId(productId);

        // Cập nhật các trường trong Product
        product.setNumRatings(newNumRatings != null ? newNumRatings : 0);
        product.setAverageRating(newAverageRating != null ? newAverageRating : 0.0);

        // Lưu lại Product đã cập nhật
        productRepository.save(product);
        System.out.println("Updated product rating for ID " + productId + ": num=" + product.getNumRatings() + ", avg=" + product.getAverageRating()); // Log để kiểm tra
    }
}
