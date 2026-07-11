package com.xd.smartworksite.system.dto;

import java.time.OffsetDateTime;

public class SystemVersionResponse {
    private String applicationName;
    private String artifactVersion;
    private String springBootVersion;
    private String javaVersion;
    private OffsetDateTime serverTime;

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    public String getArtifactVersion() { return artifactVersion; }
    public void setArtifactVersion(String artifactVersion) { this.artifactVersion = artifactVersion; }
    public String getSpringBootVersion() { return springBootVersion; }
    public void setSpringBootVersion(String springBootVersion) { this.springBootVersion = springBootVersion; }
    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
    public OffsetDateTime getServerTime() { return serverTime; }
    public void setServerTime(OffsetDateTime serverTime) { this.serverTime = serverTime; }
}
