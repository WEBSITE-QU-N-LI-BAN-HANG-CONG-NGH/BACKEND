package com.webanhang.team_project.service.category;


import com.webanhang.team_project.model.Category;

import java.util.List;

public interface ICategoryService {
    Category addCategory(Category category);
    Category updateCategory(Category category, int categoryId);
    void deleteCategory(int categoryId);


    List<Category> getAllCategories();
    Category findCategoryByName(String name);
    Category findCategoryById(int categoryId);
}
