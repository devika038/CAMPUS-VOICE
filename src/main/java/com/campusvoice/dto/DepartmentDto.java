package com.campusvoice.dto;

import com.campusvoice.entity.Department;

public class DepartmentDto {
    private Long id;
    private String name;
    private String description;

    public DepartmentDto() {
    }

    public DepartmentDto(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static DepartmentDto fromEntity(Department dept) {
        if (dept == null) return null;
        return new DepartmentDto(dept.getId(), dept.getName(), dept.getDescription());
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
