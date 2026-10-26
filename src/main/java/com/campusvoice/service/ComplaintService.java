package com.campusvoice.service;

import com.campusvoice.dto.ComplaintHistoryDto;
import com.campusvoice.dto.ComplaintRequest;
import com.campusvoice.dto.ComplaintResponse;
import com.campusvoice.dto.StatusUpdateRequest;
import com.campusvoice.entity.*;
import com.campusvoice.exception.BadRequestException;
import com.campusvoice.exception.ForbiddenException;
import com.campusvoice.exception.InvalidStatusTransitionException;
import com.campusvoice.exception.ResourceNotFoundException;
import com.campusvoice.repository.CategoryRepository;
import com.campusvoice.repository.ComplaintHistoryRepository;
import com.campusvoice.repository.ComplaintRepository;
import com.campusvoice.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final PriorityService priorityService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            ComplaintHistoryRepository historyRepository,
                            CategoryRepository categoryRepository,
                            DepartmentRepository departmentRepository,
                            PriorityService priorityService) {
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.priorityService = priorityService;
    }

    private synchronized String generateNextComplaintId() {
        Long maxSeq = complaintRepository.findMaxComplaintSequenceNumber();
        long nextSeq = (maxSeq != null ? maxSeq : 0L) + 1L;
        return String.format("CV-%04d", nextSeq);
    }

    public ComplaintResponse createComplaint(ComplaintRequest request, User currentUser) {
        if (currentUser.getRole() == Role.DEPARTMENT_ADMIN) {
            throw new ForbiddenException("Department Administrators cannot submit student complaints.");
        }

        Category category = categoryRepository.findByNameIgnoreCase(request.getCategory().trim())
                .orElseThrow(() -> new BadRequestException("Invalid category: " + request.getCategory()));

        Department department = departmentRepository.findByNameIgnoreCase(request.getDepartment().trim())
                .orElseThrow(() -> new BadRequestException("Invalid department: " + request.getDepartment()));

        String complaintId = generateNextComplaintId();
        Priority priority = priorityService.calculatePriority(category.getName(), request.getDescription());

        // Complaint is created and routed directly to the selected department in ASSIGNED status
        Complaint complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setUser(currentUser);
        complaint.setCategory(category);
        complaint.setDepartment(department);
        complaint.setTitle(request.getTitle().trim());
        complaint.setDescription(request.getDescription().trim());
        complaint.setPriority(priority);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());

        // History entry 1: Submission (PENDING)
        ComplaintHistory submissionEntry = new ComplaintHistory(
                complaint,
                currentUser,
                currentUser.getName(),
                null,
                ComplaintStatus.PENDING,
                "Complaint submitted."
        );
        complaint.addHistory(submissionEntry);

        // History entry 2: Automatic Routing to department (ASSIGNED)
        ComplaintHistory routedEntry = new ComplaintHistory(
                complaint,
                null,
                "Campus Voice",
                ComplaintStatus.PENDING,
                ComplaintStatus.ASSIGNED,
                "Assigned to " + department.getName() + " department."
        );
        complaint.addHistory(routedEntry);

        Complaint savedComplaint = complaintRepository.save(complaint);
        return ComplaintResponse.fromEntity(savedComplaint);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getMyComplaints(User currentUser) {
        return complaintRepository.findByUserOrderByCreatedAtDesc(currentUser).stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(String id, User currentUser) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));

        // Authorization checks (Rule 3)
        if (currentUser.getRole() == Role.DEPARTMENT_ADMIN) {
            if (currentUser.getDepartment() == null || 
                !complaint.getDepartment().getId().equals(currentUser.getDepartment().getId())) {
                throw new ForbiddenException("You are only authorized to view complaints for your assigned department.");
            }
        } else {
            if (!complaint.getUser().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("You are only authorized to view your own complaints.");
            }
        }

        return ComplaintResponse.fromEntity(complaint);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getDepartmentComplaints(User adminUser, String statusStr, String priorityStr, String keyword) {
        if (adminUser.getRole() != Role.DEPARTMENT_ADMIN) {
            throw new ForbiddenException("Only Department Administrators can view department complaints.");
        }
        if (adminUser.getDepartment() == null) {
            throw new BadRequestException("Admin user has no department assigned.");
        }

        ComplaintStatus status = (statusStr != null && !statusStr.isBlank()) ? ComplaintStatus.fromString(statusStr) : null;
        Priority priority = (priorityStr != null && !priorityStr.isBlank()) ? Priority.fromString(priorityStr) : null;
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        return complaintRepository.findAdminComplaintsFiltered(adminUser.getDepartment(), status, priority, cleanKeyword).stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ComplaintResponse updateStatus(String complaintId, StatusUpdateRequest request, User adminUser) {
        if (adminUser.getRole() != Role.DEPARTMENT_ADMIN) {
            throw new ForbiddenException("Only Department Administrators can update complaint status.");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        if (adminUser.getDepartment() == null || 
            !complaint.getDepartment().getId().equals(adminUser.getDepartment().getId())) {
            throw new ForbiddenException("You can only update complaints assigned to your department.");
        }

        ComplaintStatus targetStatus = ComplaintStatus.fromString(request.getNewStatus());
        ComplaintStatus currentStatus = complaint.getStatus();

        if (currentStatus.isTerminal()) {
            throw new InvalidStatusTransitionException("This complaint is already closed (" + currentStatus + ") and cannot be modified.");
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransitionException("Invalid status transition from " + currentStatus + " to " + targetStatus + ". Allowed: " + currentStatus.getAllowedNextStatuses());
        }

        // Rule 9: Require remarks when resolving or rejecting a complaint
        String remarks = request.getRemarks() != null ? request.getRemarks().trim() : "";
        if (targetStatus.isTerminal() && remarks.isEmpty()) {
            throw new BadRequestException("Remarks are required to resolve or reject a complaint.");
        }

        // Apply update
        complaint.setStatus(targetStatus);
        complaint.setUpdatedAt(LocalDateTime.now());

        // Append to history (Rule 8 & 10)
        ComplaintHistory historyEntry = new ComplaintHistory(
                complaint,
                adminUser,
                adminUser.getName(),
                currentStatus,
                targetStatus,
                remarks.isEmpty() ? null : remarks
        );
        complaint.addHistory(historyEntry);

        Complaint updatedComplaint = complaintRepository.save(complaint);
        return ComplaintResponse.fromEntity(updatedComplaint);
    }

    @Transactional(readOnly = true)
    public List<ComplaintHistoryDto> getComplaintHistory(String id, User currentUser) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + id));

        // Authorization check
        if (currentUser.getRole() == Role.DEPARTMENT_ADMIN) {
            if (currentUser.getDepartment() == null || 
                !complaint.getDepartment().getId().equals(currentUser.getDepartment().getId())) {
                throw new ForbiddenException("You cannot view history for another department's complaint.");
            }
        } else {
            if (!complaint.getUser().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("You cannot view history for another user's complaint.");
            }
        }

        return historyRepository.findByComplaintOrderByCreatedAtAsc(complaint).stream()
                .map(ComplaintHistoryDto::fromEntity)
                .collect(Collectors.toList());
    }
}
