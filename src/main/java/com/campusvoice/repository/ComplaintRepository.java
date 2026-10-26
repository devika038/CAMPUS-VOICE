package com.campusvoice.repository;

import com.campusvoice.entity.Complaint;
import com.campusvoice.entity.Department;
import com.campusvoice.entity.Priority;
import com.campusvoice.entity.ComplaintStatus;
import com.campusvoice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, String> {

    List<Complaint> findByUserOrderByCreatedAtDesc(User user);

    List<Complaint> findByDepartmentOrderByCreatedAtDesc(Department department);

    @Query("SELECT c FROM Complaint c " +
           "WHERE c.department = :department " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:priority IS NULL OR c.priority = :priority) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(c.id) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(c.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(c.user.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY " +
           "  CASE c.priority " +
           "    WHEN com.campusvoice.entity.Priority.CRITICAL THEN 1 " +
           "    WHEN com.campusvoice.entity.Priority.HIGH THEN 2 " +
           "    WHEN com.campusvoice.entity.Priority.MEDIUM THEN 3 " +
           "    WHEN com.campusvoice.entity.Priority.LOW THEN 4 " +
           "    ELSE 5 END ASC, " +
           "  c.createdAt ASC")
    List<Complaint> findAdminComplaintsFiltered(
            @Param("department") Department department,
            @Param("status") ComplaintStatus status,
            @Param("priority") Priority priority,
            @Param("keyword") String keyword);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 4) AS UNSIGNED)), 0) FROM complaints", nativeQuery = true)
    Long findMaxComplaintSequenceNumber();
}
