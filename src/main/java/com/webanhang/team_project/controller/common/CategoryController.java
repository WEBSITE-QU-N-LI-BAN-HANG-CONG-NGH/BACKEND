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

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addCategory(@RequestBody Category category) {
        Category theCategory = categoryService.addCategory(category);
        return ResponseEntity.ok().body(ApiResponse.success(theCategory, "Add success"));
    }

    @GetMapping("/category/{id}")
    public ResponseEntity<ApiResponse> getCategoryById(@PathVariable int id) {
        Category category = categoryService.findCategoryById(id);
        return ResponseEntity.ok().body(ApiResponse.success(category, "Success"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse> updateCategory(@PathVariable int id) {
        Category category = categoryService.findCategoryById(id);
        Category updatedCategory = categoryService.updateCategory(category, id);
        return ResponseEntity.ok().body(ApiResponse.success(updatedCategory, "Update category id: " + id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable int id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().body(ApiResponse.success(null, "Delete category id: " + id));
    }

    @GetMapping("/category/{name}")
    public ResponseEntity<ApiResponse> getCategoryByName(@PathVariable String name) {
        Category category = categoryService.findCategoryByName(name);
        return ResponseEntity.ok().body(ApiResponse.success(category, "Found "));
    }
}
