package com.stability.coareport.dto;

import java.time.LocalDateTime;

public class CommentDto {
    private Long id;
    private Long reportId;
    private Long userId;
    private String username;
    private String commentText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CommentDto() {
    }

    public CommentDto(Long id, Long reportId, Long userId, String username, String commentText,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reportId = reportId;
        this.userId = userId;
        this.username = username;
        this.commentText = commentText;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
