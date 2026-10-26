package com.campusvoice.controller;

import com.campusvoice.dto.ComplaintHistoryDto;
import com.campusvoice.dto.ComplaintRequest;
import com.campusvoice.dto.ComplaintResponse;
import com.campusvoice.entity.User;
import com.campusvoice.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody ComplaintRequest request,
            @AuthenticationPrincipal User currentUser) {
        ComplaintResponse created = complaintService.createComplaint(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(
            @AuthenticationPrincipal User currentUser) {
        List<ComplaintResponse> complaints = complaintService.getMyComplaints(currentUser);
        return ResponseEntity.ok(complaints);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        ComplaintResponse complaint = complaintService.getComplaintById(id, currentUser);
        return ResponseEntity.ok(complaint);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ComplaintHistoryDto>> getComplaintHistory(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        List<ComplaintHistoryDto> history = complaintService.getComplaintHistory(id, currentUser);
        return ResponseEntity.ok(history);
    }
}
