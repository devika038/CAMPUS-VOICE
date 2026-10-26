package com.campusvoice.dto;

import com.campusvoice.entity.ComplaintHistory;
import java.time.format.DateTimeFormatter;

public class ComplaintHistoryDto {
    private Long id;
    private String oldStatus;
    private String newStatus;
    private String remarks;
    private String time;
    private String who;

    public ComplaintHistoryDto() {
    }

    public ComplaintHistoryDto(Long id, String oldStatus, String newStatus, String remarks, String time, String who) {
        this.id = id;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.remarks = remarks;
        this.time = time;
        this.who = who;
    }

    public static ComplaintHistoryDto fromEntity(ComplaintHistory entity) {
        if (entity == null) return null;
        return new ComplaintHistoryDto(
                entity.getId(),
                entity.getOldStatus() != null ? entity.getOldStatus().name() : "",
                entity.getNewStatus() != null ? entity.getNewStatus().name() : "",
                entity.getRemarks() != null ? entity.getRemarks() : "",
                entity.getCreatedAt() != null ? entity.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : "",
                entity.getActingUserName()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getWho() {
        return who;
    }

    public void setWho(String who) {
        this.who = who;
    }
}
