package com.webanhang.team_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Category name is required")
    @Size(min = 1, max = 50, message = "Category name must be between 1 and 50 characters")
    @Column(unique = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Category> subCategories = new ArrayList<>();

    @Column(name = "level", nullable = false)
    private int level;

    @JsonIgnore
    @OneToMany(mappedBy = "category")
    List<Product> products;

    public Category(String name, Category parentCategory) {
        this.name = name;
        this.parentCategory = parentCategory;
        this.level = (parentCategory != null) ? parentCategory.getLevel() + 1 : 1;
    }

    public void addSubCategory(Category subCategory) {
        subCategories.add(subCategory);
        subCategory.setParentCategory(this);
    }

    public void removeSubCategory(Category subCategory) {
        subCategories.remove(subCategory);
        subCategory.setParentCategory(null);
    }
}
