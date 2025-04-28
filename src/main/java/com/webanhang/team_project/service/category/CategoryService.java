package com.webanhang.team_project.service.category;


import com.webanhang.team_project.dto.category.CategoryDTO;
import com.webanhang.team_project.model.Category;
import com.webanhang.team_project.repository.CategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {
   private final CategoryRepository categoryRepository;

   @Override
   public Category addCategory(Category category) {
       return Optional.of(category)
               .filter(c -> !categoryRepository.existsByName(c.getName()))
               .map(categoryRepository :: save)
               .orElseThrow(() -> new EntityExistsException(category.getName() + "already exists"));
   }

   @Override
   public Category updateCategory(Category category) {
       return Optional.ofNullable(findCategoryById(category.getId()))
               .map(oldCategory -> {
                   oldCategory.setName(category.getName());
                   return categoryRepository.save(oldCategory);
               }).orElseThrow(() -> new EntityNotFoundException("Category not found"));
   }


   @Override
   public List<Category> getAllCategories() {
       return categoryRepository.findAll();
   }

   @Override
   public Category findCategoryByName(String name) {
       return categoryRepository.findByName(name);
   }

   @Override
   public Category findCategoryById(Long categoryId) {
       return categoryRepository.findById(categoryId)
               .orElseThrow(() -> new EntityNotFoundException("Category not found"));
   }

    @Override
    public List<Category> getAllParentCategories() {
        return categoryRepository.findByLevel(1);
    }

    @Override
    public List<Category> getChildTopCategories(String topCategory) {
        // Tạo đối tượng Pageable yêu cầu trang đầu tiên (index 0) với 5 phần tử
        Pageable limit = PageRequest.of(0, 5);
        // Gọi phương thức repository đã sửa đổi
        return categoryRepository.findByParentCategoryNameIgnoreCase(topCategory, limit);
    }

//    @Override
//    public List<Category> getAllCategories() {
//        return categoryRepository.findAll();
//    }

}
