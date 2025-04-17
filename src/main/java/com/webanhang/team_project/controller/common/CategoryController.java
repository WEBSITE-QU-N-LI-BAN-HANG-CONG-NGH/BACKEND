package com.webanhang.team_project.controller.common;


import com.webanhang.team_project.model.Category;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.service.category.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/categories")
public class CategoryController {
    private final ICategoryService categoryService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok().body(ApiResponse.success(categories,"Get success"));
    }

    @GetMapping("/parent")
    public ResponseEntity<ApiResponse> getAllParentCategories() {
        List<Category> parentCategories = categoryService.getAllParentCategories();
        return ResponseEntity.ok().body(ApiResponse.success(parentCategories,"Get parent categories success"));
    }

    @GetMapping("/sub/{parentId}")
    public ResponseEntity<ApiResponse> getSubCategoriesByParentId(@PathVariable Long parentId) {
        List<Category> subCategories = categoryService.getSubCategoriesByParentId(parentId);
        return ResponseEntity.ok().body(ApiResponse.success(subCategories,"Get sub categories success"));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addCategory(@RequestBody Category category) {
        // Đảm bảo level không vượt quá 2
        if (category.getParentCategory() != null) {
            category.setLevel(2);
            category.setParent(false);
        } else {
            category.setLevel(1);
            category.setParent(true);
        }

        Category theCategory = categoryService.addCategory(category);
        return ResponseEntity.ok().body(ApiResponse.success(theCategory, "Add success"));
    }

    @GetMapping("/category/{id}")
    public ResponseEntity<ApiResponse> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.findCategoryById(id);
        return ResponseEntity.ok().body(ApiResponse.success(category, "Success"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        // Đảm bảo cấp bậc không vượt quá 2
        if (category.getParentCategory() != null) {
            Category parentCategory = categoryService.findCategoryById(category.getParentCategory().getId());
            if (parentCategory.getParentCategory() != null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Cannot create category with depth > 2"));
            }
            category.setLevel(2);
            category.setParent(false);
        } else {
            category.setLevel(1);
            category.setParent(true);
        }

        Category updatedCategory = categoryService.updateCategory(category, id);
        return ResponseEntity.ok().body(ApiResponse.success(updatedCategory, "Update category id: " + id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().body(ApiResponse.success(null, "Delete category id: " + id));
    }

}
