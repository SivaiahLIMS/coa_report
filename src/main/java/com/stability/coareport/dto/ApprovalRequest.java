package com.stability.coareport.dto;

public class ApprovalRequest {
    private Long reportId;
    private String action; // "approve" or "reject"
    private String reason; // Reason for rejection (optional for approval)

    public ApprovalRequest() {
    }

    public ApprovalRequest(Long reportId, String action, String reason) {
        this.reportId = reportId;
        this.action = action;
        this.reason = reason;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
