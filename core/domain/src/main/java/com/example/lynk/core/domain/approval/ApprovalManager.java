package com.example.lynk.core.domain.approval;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the lifecycle of {@link ApprovalRequest}s.
 */
public class ApprovalManager {
    private final Map<String, ApprovalRequest> requests;

    /**
     * Constructs a new ApprovalManager.
     */
    public ApprovalManager() {
        this.requests = new HashMap<>();
    }

    /**
     * Submits a new approval request.
     *
     * @param request The {@link ApprovalRequest} to submit.
     */
    public void submitRequest(ApprovalRequest request) {
        if (request != null && !requests.containsKey(request.getId())) {
            requests.put(request.getId(), request);
        }
    }

    /**
     * Retrieves an approval request by its ID.
     *
     * @param requestId The ID of the request to retrieve.
     * @return The {@link ApprovalRequest}, or null if not found.
     */
    public ApprovalRequest getRequest(String requestId) {
        return requests.get(requestId);
    }

    /**
     * Approves a pending request.
     *
     * @param requestId The ID of the request to approve.
     * @return true if successfully approved, false if not found or not in PENDING state.
     */
    public boolean approveRequest(String requestId) {
        return transitionStatus(requestId, ApprovalStatus.APPROVED);
    }

    /**
     * Rejects a pending request.
     *
     * @param requestId The ID of the request to reject.
     * @return true if successfully rejected, false if not found or not in PENDING state.
     */
    public boolean rejectRequest(String requestId) {
        return transitionStatus(requestId, ApprovalStatus.REJECTED);
    }

    /**
     * Helper method to transition a request to a new status.
     *
     * @param requestId The ID of the request.
     * @param newStatus The new status to transition to.
     * @return true if successful, false otherwise.
     */
    private boolean transitionStatus(String requestId, ApprovalStatus newStatus) {
        ApprovalRequest request = requests.get(requestId);
        if (request != null && request.getStatus() == ApprovalStatus.PENDING) {
            request.setStatus(newStatus);
            return true;
        }
        return false;
    }
}
