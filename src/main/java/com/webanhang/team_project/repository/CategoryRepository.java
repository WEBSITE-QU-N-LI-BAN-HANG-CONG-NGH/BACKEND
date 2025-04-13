package com.webanhang.team_project.repository;


import com.webanhang.team_project.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long>{

    Category findByName(String name);
    boolean existsByName(String name);

    // Tìm kiếm danh mục con có tên và cha là danh mục cha đã cho
    @Query("SELECT c FROM Category c WHERE c.name = :name AND c.parentCategory.name = :parentCategoryName")
    Category findByNameAndParent(@Param("name") String name, @Param("parentCategory") String parentCategoryName);
}