package com.campusvoice.controller;

import com.campusvoice.dto.ComplaintResponse;
import com.campusvoice.dto.StatusUpdateRequest;
import com.campusvoice.entity.User;
import com.campusvoice.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/complaints")
@PreAuthorize("hasRole('DEPARTMENT_ADMIN')")
public class AdminComplaintController {

    private final ComplaintService complaintService;

    public AdminComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getDepartmentComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User adminUser) {
        List<ComplaintResponse> complaints = complaintService.getDepartmentComplaints(adminUser, status, priority, search);
        return ResponseEntity.ok(complaints);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateComplaintStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal User adminUser) {
        ComplaintResponse updated = complaintService.updateStatus(id, request, adminUser);
        return ResponseEntity.ok(updated);
    }
}
