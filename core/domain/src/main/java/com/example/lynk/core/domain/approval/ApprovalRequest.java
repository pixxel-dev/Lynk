package com.example.lynk.core.domain.approval;

/**
 * Represents a request for approval in the system.
 */
public class ApprovalRequest {
    private final String id;
    private final String requesterId;
    private final String details;
    private ApprovalStatus status;

    /**
     * Constructs a new ApprovalRequest.
     *
     * @param id The unique identifier for this request.
     * @param requesterId The ID of the user or agent requesting approval.
     * @param details Additional details or context for the request.
     */
    public ApprovalRequest(String id, String requesterId, String details) {
        this.id = id;
        this.requesterId = requesterId;
        this.details = details;
        this.status = ApprovalStatus.PENDING;
    }

    /**
     * Retrieves the request's unique identifier.
     *
     * @return The request ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the ID of the requester.
     *
     * @return The requester's ID.
     */
    public String getRequesterId() {
        return requesterId;
    }

    /**
     * Retrieves the details of the request.
     *
     * @return The request details.
     */
    public String getDetails() {
        return details;
    }

    /**
     * Retrieves the current status of the approval request.
     *
     * @return The current {@link ApprovalStatus}.
     */
    public ApprovalStatus getStatus() {
        return status;
    }

    /**
     * Updates the status of this request.
     *
     * @param status The new {@link ApprovalStatus}.
     */
    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }
}
