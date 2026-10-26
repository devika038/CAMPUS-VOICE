package com.campusvoice.dto;

import com.campusvoice.entity.User;

public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String department;

    public UserDto() {
    }

    public UserDto(Long id, String name, String email, String role, String department) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.department = department;
    }

    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : "";
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getDisplayName(),
                deptName
        );
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
