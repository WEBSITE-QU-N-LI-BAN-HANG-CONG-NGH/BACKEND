package com.webanhang.team_project.service.admin;


import com.webanhang.team_project.model.Category;

import java.util.List;
import java.util.Map;

public interface IAdminCategoryService {
    List<Category> getAllCategories();
    Category createCategory(Category category);
    Category updateCategory(Long id, Category category);
    void deleteCategory(Long id);
    List<Map<String, Object>> getCategoryRevenue();
}
