package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.model.Category;
import com.webanhang.team_project.model.OrderItem;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.repository.CategoryRepository;
import com.webanhang.team_project.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminCategoryService implements IAdminCategoryService {
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional
    public Category createCategory(Category category) {
        // Kiểm tra xem category đã tồn tại chưa
        if (categoryRepository.findByName(category.getName()) != null) {
            throw new RuntimeException("Category already exists");
        }

        // Cấu hình level và isParent dựa trên parentCategory
        if (category.getParentCategory() != null) {
            category.setLevel(2);
            category.setParent(false);
        } else {
            category.setLevel(1);
            category.setParent(true);
        }

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, Category category) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Cấu hình level và isParent dựa trên parentCategory
        if (category.getParentCategory() != null) {
            Category parentCategory = categoryRepository.findById(category.getParentCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));

            if (parentCategory.getParentCategory() != null) {
                throw new RuntimeException("Cannot create category with depth > 2");
            }

            category.setLevel(2);
            category.setParent(false);
        } else {
            category.setLevel(1);
            category.setParent(true);
        }

        // Cập nhật thông tin
        existingCategory.setName(category.getName());
        if (category.getParentCategory() != null) {
            existingCategory.setParentCategory(category.getParentCategory());
        }
        existingCategory.setLevel(category.getLevel());
        existingCategory.setParent(category.isParent());

        return categoryRepository.save(existingCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Kiểm tra xem category có sản phẩm không
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new RuntimeException("Cannot delete category with products");
        }

        categoryRepository.delete(category);
    }

    @Override
    public List<Map<String, Object>> getCategoryRevenue() {
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

        return result;
    }
}
