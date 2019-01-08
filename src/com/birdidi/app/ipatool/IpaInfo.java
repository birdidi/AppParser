package com.birdidi.app.ipatool;

import java.util.List;

public class IpaInfo {

    public String bundleId;
    public String bundleVersionCode;
    public String bundleVersionName;
    public String appName;
    public String appIcon;
    List<String> appIcons;

    @Override
    public String toString() {
        return "IpaInfo{" +
                "bundleId='" + bundleId + '\'' +
                ", bundleVersionCode='" + bundleVersionCode + '\'' +
                ", bundleVersionName='" + bundleVersionName + '\'' +
                ", appName='" + appName + '\'' +
                ", appIcon='" + appIcon + '\'' +
                '}';
    }
}
