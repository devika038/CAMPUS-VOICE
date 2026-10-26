package com.campusvoice.dto;

import com.campusvoice.entity.Category;

public class CategoryDto {
    private Long id;
    private String name;

    public CategoryDto() {
    }

    public CategoryDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static CategoryDto fromEntity(Category cat) {
        if (cat == null) return null;
        return new CategoryDto(cat.getId(), cat.getName());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
