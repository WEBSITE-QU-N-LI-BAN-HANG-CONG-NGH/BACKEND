package com.webanhang.team_project.controller.admin;

import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.model.Category;
import com.webanhang.team_project.model.OrderItem;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.CategoryRepository;
import com.webanhang.team_project.repository.OrderItemRepository;
import com.webanhang.team_project.service.admin.AdminCategoryService;
import com.webanhang.team_project.service.category.CategoryService;
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

    private final AdminCategoryService adminCategoryService;
    private final CategoryService categoryService;

    /**
     * Lấy tất cả danh mục
     *
     * @return Danh sách tất cả danh mục
     */
    @GetMapping("/")
    public ResponseEntity<ApiResponse> getAllCategories() {
        List<Category> categories = adminCategoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Get all categories success"));
    }

    /**
     * Cập nhật thông tin danh mục
     *
     *
     * @param category Thông tin mới của danh mục
     * @return Thông tin danh mục sau khi cập nhật
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse> updateCategory(@RequestBody Category category) {

        Category updatedCategory = categoryService.updateCategory(category);
        return ResponseEntity.ok().body(ApiResponse.success(updatedCategory, "Success. Update category id: " + category.getId()));
    }


    /**
     * Lấy doanh thu theo danh mục
     *
     * @return Thông tin doanh thu của từng danh mục
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse> getCategoryRevenue() {
        List<Map<String, Object>> result = adminCategoryService.getCategoryRevenue();
        return ResponseEntity.ok(ApiResponse.success(result, "Get category revenue success"));
    }
}
