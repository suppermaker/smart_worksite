package com.xd.smartworksite.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.login-security")
public class LoginSecurityProperties {

    private int maxFailureCount = 5;
    private long failureWindowSeconds = 900;
    private long lockSeconds = 900;

    public int getMaxFailureCount() {
        return maxFailureCount;
    }

    public void setMaxFailureCount(int maxFailureCount) {
        this.maxFailureCount = maxFailureCount;
    }

    public long getFailureWindowSeconds() {
        return failureWindowSeconds;
    }

    public void setFailureWindowSeconds(long failureWindowSeconds) {
        this.failureWindowSeconds = failureWindowSeconds;
    }

    public long getLockSeconds() {
        return lockSeconds;
    }

    public void setLockSeconds(long lockSeconds) {
        this.lockSeconds = lockSeconds;
    }
}
