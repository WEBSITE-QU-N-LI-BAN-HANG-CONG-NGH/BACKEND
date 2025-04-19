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

    @GetMapping("/{topCategory}")
    public ResponseEntity<ApiResponse> getChildTopCategories(@PathVariable("topCategory") String topCategory) {
        List<Category> childCategories = categoryService.getChildTopCategories(topCategory);
        return ResponseEntity.ok().body(ApiResponse.success(childCategories,"Get child categories success"));
    }
}
