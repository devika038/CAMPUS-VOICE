package com.campusvoice.service;

import com.campusvoice.dto.DepartmentDto;
import com.campusvoice.entity.Department;
import com.campusvoice.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(DepartmentDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Department getByName(String name) {
        return departmentRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + name));
    }
}
