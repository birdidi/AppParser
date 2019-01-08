package com.birdidi.app.apktool;

import brut.androlib.res.data.ResTable;

public class ApkInfo {

    private String bundleId;
    private String versionName;
    private String versionCode;
    private String minSdkVersion;
    private String targetSdkVersion;
    private String appName;
    private String appIcon;

    private String appNameType;
    private String appIconType;

    public ApkInfo() {

    }

    public ApkInfo(ResTable resTable) {
        setBundleId(resTable.getPackageRenamed());
        setVersionCode(resTable.getVersionInfo().versionCode);
        setVersionName(resTable.getVersionInfo().versionName);
        setMinSdkVersion(resTable.getSdkInfo().get("minSdkVersion"));
        setTargetSdkVersion(resTable.getSdkInfo().get("targetSdkVersion"));
        setAppName(resTable.getSdkInfo().get("label"));
        setAppNameType(resTable.getSdkInfo().get("labelType"));
        setAppIcon(resTable.getSdkInfo().get("icon"));
        setAppIconType(resTable.getSdkInfo().get("iconType"));
    }

    public String getBundleId() {
        return bundleId;
    }

    public void setBundleId(String bundleId) {
        this.bundleId = bundleId;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getMinSdkVersion() {
        return minSdkVersion;
    }

    public void setMinSdkVersion(String minSdkVersion) {
        this.minSdkVersion = minSdkVersion;
    }

    public String getTargetSdkVersion() {
        return targetSdkVersion;
    }

    public void setTargetSdkVersion(String targetSdkVersion) {
        this.targetSdkVersion = targetSdkVersion;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(String appIcon) {
        this.appIcon = appIcon;
    }

    public String getAppNameType() {
        return appNameType;
    }

    public void setAppNameType(String appNameType) {
        this.appNameType = appNameType;
    }

    public String getAppIconType() {
        return appIconType;
    }

    public void setAppIconType(String appIconType) {
        this.appIconType = appIconType;
    }

    @Override
    public String toString() {
        return "ApkInfo{" +
                "bundleId='" + bundleId + '\'' +
                ", versionName='" + versionName + '\'' +
                ", versionCode='" + versionCode + '\'' +
                ", minSdkVersion='" + minSdkVersion + '\'' +
                ", targetSdkVersion='" + targetSdkVersion + '\'' +
                ", appName='" + appName + '\'' +
                ", appIcon='" + appIcon + '\'' +
                ", appNameType='" + appNameType + '\'' +
                ", appIconType='" + appIconType + '\'' +
                '}';
    }
}
