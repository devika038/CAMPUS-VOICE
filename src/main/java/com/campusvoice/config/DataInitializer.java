package com.campusvoice.config;

import com.campusvoice.entity.*;
import com.campusvoice.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(DepartmentRepository departmentRepository,
                           CategoryRepository categoryRepository,
                           UserRepository userRepository,
                           ComplaintRepository complaintRepository,
                           ComplaintHistoryRepository historyRepository,
                           PasswordEncoder passwordEncoder) {
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedDepartments();
        seedCategories();
        seedDemoUsersAndComplaints();
    }

    private void seedDepartments() {
        List<String> depts = List.of(
                "Computer Science",
                "Electrical",
                "Mechanical",
                "Civil",
                "Hostel",
                "Maintenance",
                "Library",
                "Administration"
        );

        for (String name : depts) {
            if (!departmentRepository.existsByNameIgnoreCase(name)) {
                departmentRepository.save(new Department(name, name + " Department"));
            }
        }
    }

    private void seedCategories() {
        List<String> cats = List.of(
                "Infrastructure",
                "Academics",
                "Hostel",
                "Harassment",
                "Lab Equipment",
                "Library",
                "Examination",
                "Other"
        );

        for (String name : cats) {
            if (!categoryRepository.existsByNameIgnoreCase(name)) {
                categoryRepository.save(new Category(name));
            }
        }
    }

    private void seedDemoUsersAndComplaints() {
        if (userRepository.count() > 0) {
            return; // Already seeded
        }

        Department csDept = departmentRepository.findByNameIgnoreCase("Computer Science").orElseThrow();
        Department maintDept = departmentRepository.findByNameIgnoreCase("Maintenance").orElseThrow();
        Department hostelDept = departmentRepository.findByNameIgnoreCase("Hostel").orElseThrow();
        Department libDept = departmentRepository.findByNameIgnoreCase("Library").orElseThrow();

        Category labCat = categoryRepository.findByNameIgnoreCase("Lab Equipment").orElseThrow();
        Category infraCat = categoryRepository.findByNameIgnoreCase("Infrastructure").orElseThrow();
        Category acadCat = categoryRepository.findByNameIgnoreCase("Academics").orElseThrow();
        Category hostelCat = categoryRepository.findByNameIgnoreCase("Hostel").orElseThrow();

        String commonPasswordHash = passwordEncoder.encode("Password123!");

        // 1. Student User
        User student = new User("Aarav Sharma", "student@campus.edu", commonPasswordHash, Role.STUDENT, null);
        userRepository.save(student);

        // 2. Faculty User
        User faculty = new User("Dr. Priya Raman", "faculty@campus.edu", commonPasswordHash, Role.FACULTY, null);
        userRepository.save(faculty);

        // 3. Admin - Computer Science
        User adminCs = new User("Prof. Vikram Mehta", "admin.cs@campus.edu", commonPasswordHash, Role.DEPARTMENT_ADMIN, csDept);
        userRepository.save(adminCs);

        // 4. Admin - Maintenance
        User adminMaint = new User("Rajesh Kumar", "admin.maint@campus.edu", commonPasswordHash, Role.DEPARTMENT_ADMIN, maintDept);
        userRepository.save(adminMaint);

        // 5. Admin - Hostel
        User adminHostel = new User("Sunita Rao", "admin.hostel@campus.edu", commonPasswordHash, Role.DEPARTMENT_ADMIN, hostelDept);
        userRepository.save(adminHostel);

        // 6. Admin - Library
        User adminLib = new User("Anand Verma", "admin.library@campus.edu", commonPasswordHash, Role.DEPARTMENT_ADMIN, libDept);
        userRepository.save(adminLib);

        // Demo Complaints
        // Complaint 1: CS Dept - ASSIGNED
        LocalDateTime t1 = LocalDateTime.now().minusDays(3);
        Complaint c1 = new Complaint("CV-0001", student, labCat, csDept,
                "Broken projector in CS Lab 3",
                "The ceiling HDMI projector flickers red and shuts off after 5 minutes during lab sessions.",
                Priority.HIGH, ComplaintStatus.ASSIGNED);
        c1.setCreatedAt(t1);
        c1.setUpdatedAt(t1);
        c1.addHistory(new ComplaintHistory(c1, student, student.getName(), null, ComplaintStatus.PENDING, "Complaint submitted."));
        c1.addHistory(new ComplaintHistory(c1, null, "Campus Voice", ComplaintStatus.PENDING, ComplaintStatus.ASSIGNED, "Assigned to Computer Science department."));
        complaintRepository.save(c1);

        // Complaint 2: CS Dept - IN_PROGRESS (Critical: contains 'fire' & 'danger')
        LocalDateTime t2 = LocalDateTime.now().minusDays(2);
        LocalDateTime t2_update = LocalDateTime.now().minusHours(18);
        Complaint c2 = new Complaint("CV-0002", student, infraCat, csDept,
                "Server room AC leaking water near rack",
                "The AC unit in Lab 2 server closet is dripping water right above the main switch. Urgent danger of short circuit and fire.",
                Priority.CRITICAL, ComplaintStatus.IN_PROGRESS);
        c2.setCreatedAt(t2);
        c2.setUpdatedAt(t2_update);
        c2.addHistory(new ComplaintHistory(c2, student, student.getName(), null, ComplaintStatus.PENDING, "Complaint submitted."));
        c2.addHistory(new ComplaintHistory(c2, null, "Campus Voice", ComplaintStatus.PENDING, ComplaintStatus.ASSIGNED, "Assigned to Computer Science department."));
        c2.addHistory(new ComplaintHistory(c2, adminCs, adminCs.getName(), ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS, "Technician dispatched to inspect AC drainage and isolate electrical lines."));
        complaintRepository.save(c2);

        // Complaint 3: CS Dept - RESOLVED
        LocalDateTime t3 = LocalDateTime.now().minusDays(5);
        LocalDateTime t3_inprog = LocalDateTime.now().minusDays(4);
        LocalDateTime t3_res = LocalDateTime.now().minusDays(1);
        Complaint c3 = new Complaint("CV-0003", student, acadCat, csDept,
                "Outdated Java runtime on CS Lab terminals",
                "The IDE installations on Lab 1 workstations were failing compilation due to missing Java 17+ tooling.",
                Priority.MEDIUM, ComplaintStatus.RESOLVED);
        c3.setCreatedAt(t3);
        c3.setUpdatedAt(t3_res);
        c3.addHistory(new ComplaintHistory(c3, student, student.getName(), null, ComplaintStatus.PENDING, "Complaint submitted."));
        c3.addHistory(new ComplaintHistory(c3, null, "Campus Voice", ComplaintStatus.PENDING, ComplaintStatus.ASSIGNED, "Assigned to Computer Science department."));
        c3.addHistory(new ComplaintHistory(c3, adminCs, adminCs.getName(), ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS, "Scheduled batch update across all Lab 1 workstations."));
        c3.addHistory(new ComplaintHistory(c3, adminCs, adminCs.getName(), ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED, "Updated all Lab 1 terminals to JDK 17 & 21 and verified student build environments."));
        complaintRepository.save(c3);

        // Complaint 4: Maintenance - IN_PROGRESS
        LocalDateTime t4 = LocalDateTime.now().minusDays(1);
        Complaint c4 = new Complaint("CV-0004", faculty, infraCat, maintDept,
                "Restroom water tap leakage in Academic Block B",
                "Continuous water overflow in 2nd floor restroom causing floor slippery conditions.",
                Priority.HIGH, ComplaintStatus.IN_PROGRESS);
        c4.setCreatedAt(t4);
        c4.setUpdatedAt(LocalDateTime.now().minusHours(4));
        c4.addHistory(new ComplaintHistory(c4, faculty, faculty.getName(), null, ComplaintStatus.PENDING, "Complaint submitted."));
        c4.addHistory(new ComplaintHistory(c4, null, "Campus Voice", ComplaintStatus.PENDING, ComplaintStatus.ASSIGNED, "Assigned to Maintenance department."));
        c4.addHistory(new ComplaintHistory(c4, adminMaint, adminMaint.getName(), ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS, "Plumbing team assigned valve replacement."));
        complaintRepository.save(c4);

        // Complaint 5: Hostel - ASSIGNED
        LocalDateTime t5 = LocalDateTime.now().minusHours(12);
        Complaint c5 = new Complaint("CV-0005", student, hostelCat, hostelDept,
                "Water heater malfunction in North Hostel Wing B",
                "Geyser on 3rd floor wing B is not heating water since yesterday morning.",
                Priority.MEDIUM, ComplaintStatus.ASSIGNED);
        c5.setCreatedAt(t5);
        c5.setUpdatedAt(t5);
        c5.addHistory(new ComplaintHistory(c5, student, student.getName(), null, ComplaintStatus.PENDING, "Complaint submitted."));
        c5.addHistory(new ComplaintHistory(c5, null, "Campus Voice", ComplaintStatus.PENDING, ComplaintStatus.ASSIGNED, "Assigned to Hostel department."));
        complaintRepository.save(c5);
    }
}
