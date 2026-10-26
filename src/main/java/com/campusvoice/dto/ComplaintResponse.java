package com.campusvoice.dto;

import com.campusvoice.entity.Complaint;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ComplaintResponse {
    private String id;
    private Long ownerId;
    private String ownerName;
    private String title;
    private String category;
    private String department;
    private String description;
    private String createdAt;
    private String updatedAt;
    private String priority;
    private String status;
    private List<ComplaintHistoryDto> audit;

    public ComplaintResponse() {
    }

    public static ComplaintResponse fromEntity(Complaint entity) {
        if (entity == null) return null;
        ComplaintResponse dto = new ComplaintResponse();
        dto.setId(entity.getId());
        dto.setOwnerId(entity.getUser().getId());
        dto.setOwnerName(entity.getUser().getName());
        dto.setTitle(entity.getTitle());
        dto.setCategory(entity.getCategory().getName());
        dto.setDepartment(entity.getDepartment().getName());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : "");
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : "");
        dto.setPriority(entity.getPriority().name());
        dto.setStatus(entity.getStatus().name());
        if (entity.getHistory() != null) {
            dto.setAudit(entity.getHistory().stream()
                    .map(ComplaintHistoryDto::fromEntity)
                    .collect(Collectors.toList()));
        } else {
            dto.setAudit(Collections.emptyList());
        }
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ComplaintHistoryDto> getAudit() {
        return audit;
    }

    public void setAudit(List<ComplaintHistoryDto> audit) {
        this.audit = audit;
    }
}
