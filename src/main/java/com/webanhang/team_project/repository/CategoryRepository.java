package com.webanhang.team_project.repository;


import com.webanhang.team_project.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Category findByName(String name);

    boolean existsByName(String name);
}
