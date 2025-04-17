package com.webanhang.team_project.dto.product;

import com.webanhang.team_project.model.Category;
import com.webanhang.team_project.model.Image;
import com.webanhang.team_project.model.Product;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private Long id;
    private String title;
    private String description;
    private int price;
    private int discountedPrice;
    private int quantity;
    private String brand;
    private String color;
    private List<String> sizes;
    private String imageUrl;
    private List<String> imageUrls;
    private int averageRating;
    private int numRatings;
    private String topLevelCategory;
    private String secondLevelCategory;
    private long quantitySold;

    // Constructor để chuyển đổi từ Product entity
    public ProductDTO(Product product) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.discountedPrice = product.getDiscountedPrice();
        this.quantity = product.getQuantity();
        this.brand = product.getBrand();
        this.color = product.getColor();
        this.quantitySold = product.getQuantitySold();

        // --- SỬA LỖI SIZES ---
        // Lấy danh sách tên size từ List<ProductSize>
        if (product.getSizes() != null) {
            // Giả sử ProductSize có phương thức getName() hoặc tương tự để lấy tên size
            this.sizes = product.getSizes().stream()
                    // .map(ProductSize::getName) // Thay getName() bằng phương thức đúng trong ProductSize
                    .map(ps -> ps.getName()) // Hoặc nếu trường tên là size, dùng lambda
                    .collect(Collectors.toList());
        } else {
            this.sizes = Collections.emptyList(); // Trả về list rỗng nếu null
        }

//        this.imageUrls = product.getImageUrl();
        // Xử lý danh sách hình ảnh
        this.imageUrls = new ArrayList<>();

        // Ưu tiên lấy từ quan hệ mới (images)
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            this.imageUrls = product.getImages().stream()
                    .map(Image::getDownloadUrl)
                    .collect(Collectors.toList());
        }

        // --- SỬA LỖI AVERAGE RATING ---
        // Tính toán averageRating từ list ratings.
        // Lưu ý: Việc tính toán này nên thực hiện ở Service layer thì tốt hơn.
        //        Để tạm ở đây cho mục đích chuyển đổi DTO.
        if (product.getRatings() != null && !product.getRatings().isEmpty()) {
            double avg = product.getRatings().stream()
                    .mapToInt(rating -> rating.getRating())
                    .average()
                    .orElse(0.0);
            this.averageRating = (int) Math.round(avg); // Làm tròn đến số nguyên gần nhất
        } else {
            this.averageRating = 0;
        }
        // Làm tròn nếu cần, hoặc giữ kiểu double trong DTO
        // this.averageRating = Math.round(avg * 10.0) / 10.0; // Làm tròn 1 chữ số thập phân

        // --- SỬA LỖI NUM RATINGS ---
        // Product entity có trường numRating (số ít)
        this.numRatings = product.getNumRating();

        // --- SỬA LỖI CATEGORIES ---
        // Product entity chỉ có 1 trường category, cần phân tách ra top và second level
        Category category = product.getCategory();
        if (category != null) {
            if (category.getLevel() == 1) {
                this.topLevelCategory = category.getName();
                this.secondLevelCategory = null; // Hoặc ""
            } else if (category.getLevel() == 2) {
                // Cần kiểm tra null cho parentCategory để phòng trường hợp dữ liệu không nhất quán
                if (category.getParentCategory() != null) {
                    this.topLevelCategory = category.getParentCategory().getName();
                } else {
                    this.topLevelCategory = null; // Hoặc "Unknown" nếu parent bị thiếu
                }
                this.secondLevelCategory = category.getName();
            } else {
                // Xử lý trường hợp level không hợp lệ (nếu có thể xảy ra)
                this.topLevelCategory = null;
                this.secondLevelCategory = null;
            }
        } else {
            this.topLevelCategory = null;
            this.secondLevelCategory = null;
        }
    }
}
