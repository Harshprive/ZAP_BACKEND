package com.ZAP_Backend.ZapServices.DataTransferObject;

public class ProviderResponse {
    private Long providerId;
    private boolean accepted;

    public ProviderResponse(Long providerId, boolean accepted) {
        this.providerId = providerId;
        this.accepted = accepted;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
