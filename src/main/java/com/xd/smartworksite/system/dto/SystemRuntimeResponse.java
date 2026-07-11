package com.xd.smartworksite.system.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class SystemRuntimeResponse {
    private String applicationName;
    private List<String> activeProfiles;
    private String javaVersion;
    private String osName;
    private String osVersion;
    private int availableProcessors;
    private long maxMemoryBytes;
    private long totalMemoryBytes;
    private long freeMemoryBytes;
    private OffsetDateTime serverTime;

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    public List<String> getActiveProfiles() { return activeProfiles; }
    public void setActiveProfiles(List<String> activeProfiles) { this.activeProfiles = activeProfiles; }
    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public int getAvailableProcessors() { return availableProcessors; }
    public void setAvailableProcessors(int availableProcessors) { this.availableProcessors = availableProcessors; }
    public long getMaxMemoryBytes() { return maxMemoryBytes; }
    public void setMaxMemoryBytes(long maxMemoryBytes) { this.maxMemoryBytes = maxMemoryBytes; }
    public long getTotalMemoryBytes() { return totalMemoryBytes; }
    public void setTotalMemoryBytes(long totalMemoryBytes) { this.totalMemoryBytes = totalMemoryBytes; }
    public long getFreeMemoryBytes() { return freeMemoryBytes; }
    public void setFreeMemoryBytes(long freeMemoryBytes) { this.freeMemoryBytes = freeMemoryBytes; }
    public OffsetDateTime getServerTime() { return serverTime; }
    public void setServerTime(OffsetDateTime serverTime) { this.serverTime = serverTime; }
}
