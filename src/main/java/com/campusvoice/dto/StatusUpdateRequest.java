package com.campusvoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StatusUpdateRequest {

    @NotBlank(message = "New status is required")
    private String newStatus;

    @Size(max = 1500, message = "Remarks cannot exceed 1500 characters")
    private String remarks;

    public StatusUpdateRequest() {
    }

    public StatusUpdateRequest(String newStatus, String remarks) {
        this.newStatus = newStatus;
        this.remarks = remarks;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
