package com.example.lynk.core.domain.approval;

/**
 * Represents the status of an {@link ApprovalRequest}.
 */
public enum ApprovalStatus {
    /**
     * Request is waiting for approval.
     */
    PENDING,

    /**
     * Request has been approved.
     */
    APPROVED,

    /**
     * Request has been rejected.
     */
    REJECTED
}
