package com.webanhang.team_project.controller.admin;

import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.model.Category;
import com.webanhang.team_project.model.OrderItem;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.CategoryRepository;
import com.webanhang.team_project.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/categories")
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * Lấy tất cả danh mục
     *
     * @return Danh sách tất cả danh mục
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(categories, "Get all categories success"));
    }

    /**
     * Tạo danh mục mới
     *
     * @param category Thông tin danh mục cần tạo
     * @return Thông tin danh mục đã tạo
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createCategory(@RequestBody Category category) {
        // Kiểm tra xem category đã tồn tại chưa
        if (categoryRepository.findByName(category.getName()) != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Category already exists"));
        }

        Category savedCategory = categoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success(savedCategory, "Create category success"));
    }

    /**
     * Cập nhật thông tin danh mục
     *
     * @param id ID của danh mục cần cập nhật
     * @param category Thông tin mới của danh mục
     * @return Thông tin danh mục sau khi cập nhật
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Cập nhật thông tin
        existingCategory.setName(category.getName());
        if (category.getParentCategory() != null) {
            existingCategory.setParentCategory(category.getParentCategory());
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        return ResponseEntity.ok(ApiResponse.success(updatedCategory, "Update category success"));
    }

    /**
     * Xóa danh mục
     *
     * @param id ID của danh mục cần xóa
     * @return Thông báo kết quả
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Kiểm tra xem category có sản phẩm không
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot delete category with products"));
        }

        categoryRepository.delete(category);
        return ResponseEntity.ok(ApiResponse.success(null, "Delete category success"));
    }

    /**
     * Lấy doanh thu theo danh mục
     *
     * @return Thông tin doanh thu của từng danh mục
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse> getCategoryRevenue() {
        List<Category> categories = categoryRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Category category : categories) {
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("id", category.getId());
            categoryData.put("name", category.getName());

            // Tính tổng sản phẩm
            int totalProducts = category.getProducts() != null ? category.getProducts().size() : 0;
            categoryData.put("totalProducts", totalProducts);

            // Tính doanh thu
            int revenue = 0;
            if (category.getProducts() != null) {
                for (Product product : category.getProducts()) {
                    List<OrderItem> orderItems = orderItemRepository.findByProductId(product.getId());
                    for (OrderItem item : orderItems) {
                        if (item.getOrder() != null &&
                                item.getOrder().getOrderStatus() == OrderStatus.DELIVERED) {
                            revenue += item.getPrice() * item.getQuantity();
                        }
                    }
                }
            }
            categoryData.put("revenue", revenue);

            result.add(categoryData);
        }

        return ResponseEntity.ok(ApiResponse.success(result, "Get category revenue success"));
    }
}
