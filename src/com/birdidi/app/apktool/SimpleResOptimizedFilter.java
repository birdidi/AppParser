package com.birdidi.app.apktool;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResValuesFile;
import brut.androlib.res.data.value.ResStringValue;
import brut.androlib.res.data.value.ResValue;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleResOptimizedFilter implements ResTableFilter {

    @Override
    public void filter(ResTable resTable) throws AndrolibException {
        String appNameType = resTable.getSdkInfo().get("labelType"), appIconType = resTable.getSdkInfo().get("iconType");
        String labelResName = resTable.getSdkInfo().get("label"), iconResName = resTable.getSdkInfo().get("icon");
        Set<ResPackage> set = resTable.listMainPackages();
        Iterator<ResPackage> iterator = set.iterator();
        while (iterator.hasNext()) {
            //一个资源文件
            ResPackage resPackage = iterator.next();

            if (!StringUtils.isEmpty(appIconType) && !StringUtils.isEmpty(iconResName)) {
                Map<String, String> unvaluesBuckets = traverUnvalueFile(resPackage, appIconType, iconResName);
                List<Object> unvaluesPreffixList = Arrays.asList(unvaluesBuckets.keySet().toArray());
                Collections.sort(unvaluesPreffixList, RESOURCE_PRIORITY_COMPATOR);
                clear(resPackage, resPackage.listFiles().iterator(), unvaluesPreffixList/*.subList(0, 1)*/);
            }

            if (!StringUtils.isEmpty(appNameType) && !StringUtils.isEmpty(labelResName)) {
                Map<String, String> valuesBuckets = traverValuesFile(resPackage, appNameType, labelResName, true);
                List<Object> valuesPreffixList = Arrays.asList(valuesBuckets.keySet().toArray());
                Collections.sort(valuesPreffixList, RESOURCE_PRIORITY_COMPATOR);
                resTable.addSdkInfo("label", valuesBuckets.get(valuesPreffixList.get(0)));
            }
        }
    }

    private Map<String, String> traverUnvalueFile(ResPackage resPackage, String targetResType, String targetResName) throws AndrolibException {
        Map<String, String> resAvailablePaths = new HashMap<String, String>();
        //非value资源
        Set<ResResource> resResources = resPackage.listFiles();
        Iterator<ResResource> resResourceIterator = resResources.iterator();
        boolean hasOptimized = false;
        while (resResourceIterator.hasNext()) {
            ResResource resResource = resResourceIterator.next();
            String resPath = resResource.getFilePath();
            ResResSpec spec = resResource.getResSpec();

            //去除不需要的资源
            String resType = spec.getType().getName();
            String resName = spec.getName();
            //是否与图标资源一致
            if (!resType.equals(targetResType) || (!(!hasOptimized && resName.contains(targetResName)) && !resName.equals(targetResName)) ) {
                resPackage.removeResSpec(spec);
            } else {
                if (resName.equals(targetResName)) {
                    hasOptimized = true;
                }
                resAvailablePaths.put(resPath, targetResName);
            }
        }
        return resAvailablePaths;
    }

    private Map<String, String> traverValuesFile(ResPackage resPackage, String targetResType, String targetResName, boolean clear) throws AndrolibException {
        Map<String, String> resAvailablePaths = new HashMap<String, String>();
        //value资源
        Collection<ResValuesFile> resValuesFiles = resPackage.listValuesFiles();
        for (ResValuesFile resValuesFile : resValuesFiles) {
            //ResResource = 一个资源对应的字符值
            Iterator<ResResource> resValueIterator = resValuesFile.listResources().iterator();
            while (resValueIterator.hasNext()) {
                ResResource resResource = resValueIterator.next();
                String resName = resResource.getResSpec().getName();
                String resType = resResource.getResSpec().getType().getName();
                if ((!resName.equals(targetResName) || !resType.equals(targetResType))) {
                    if (clear) {
                        resPackage.removeResSpec(resResource.getResSpec());
                    }
                } else {
                    ResValue resValue = resResource.getValue();
                    if (resValue instanceof ResStringValue) {
                        ResStringValue stringValue = (ResStringValue) resValue;
                        resAvailablePaths.put(resValuesFile.getPath(), stringValue.encodeAsResXmlValue());
                    }
                    if (clear) {
                        resPackage.removeResSpec(resResource.getResSpec());
                    }
                    continue;
                }
            }
        }
        return resAvailablePaths;
    }

    private void clear(ResPackage resPackage, Iterator<ResResource> iterator, List<?> preffix) throws AndrolibException {
//        System.out.println("resPackage = [" + resPackage + "], iterator = [" + iterator + "], preffix = [" + preffix + "]");
        while (iterator.hasNext()) {
            ResResource resResource = iterator.next();
            String resPath = resResource.getFilePath();
//            System.out.println(resPath);
            boolean access = false;
            for (Object pattern : preffix) {
                if (resPath.equals(pattern.toString())) {
//                    System.out.println("access - " + resPath);
                    access = true;
                    break;
                }
            }
            if (!access) {
                resPackage.removeResSpec(resResource.getResSpec());
            }
        }
    }

    public static abstract class ResourceOptimizedCompared<T> implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            try {
                int priority1 = evalPriority(getToken(o1));
                int priority2 = evalPriority(getToken(o2));
                if (priority1 > priority2) {
                    return -1;
                }

                if (priority1 < priority2) {
                    return 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        private int evalPriority(String resName) {
//            System.out.println("resName = [" + resName + "]");
            String[] chunks = resName.split("-");
            int priority = 0;
            if (chunks != null) {
                if (chunks.length > 1) {
                    String resType = chunks[0];
                    for (int i = 1, len = chunks.length; i < len; i++) {
                        String chunk = chunks[i];
                        if ("zh".equals(chunk)) {
                            priority += 2;
                        }

                        Pattern rXX = Pattern.compile("^r[A-Z]{2,}$");
                        Matcher rMatcher = rXX.matcher(chunk);
                        if (rMatcher.find()) {
                            if ("rCN".equals(chunk)) {
                                priority += 1;
                            } else {
                                priority -= 2;
                            }
                        }

                        Pattern vXX = Pattern.compile("^v[1-9][0-9]*$");
                        Matcher vMatcher = vXX.matcher(chunk);
                        if (vMatcher.find()) {
                            String vs = chunk.substring(1);
                            try {
                                int vi = Integer.valueOf(vs);
                                if ("drawable".equals(resType) || "mipmap".equals(resType)) {
                                    if (vi < 21) {
                                        priority += vi;
                                    }
                                } else {
                                    priority += i;
                                }
                            } catch (NumberFormatException e) {
                            }
                        }


                        if (chunk.endsWith("dpi")) {
                            if (chunk.equals("xxxhdpi")) {
                                priority += 5;
                            } else if (chunk.equals("xxhdpi")) {
                                priority += 4;
                            } else if (chunk.equals("xhdpi")) {
                                priority += 3;
                            } else if (chunk.equals("hdpi")) {
                                priority += 2;
                            } else {
                                priority += 1;
                            }
                        }
                    }
                } else {
                    priority += 1;
                }
            }
            return priority;
        }

        public abstract String getToken(T t);
    }

    private static final Comparator<Object> RESOURCE_PRIORITY_COMPATOR = new ResourceOptimizedCompared<Object>() {
        @Override
        public String getToken(Object resResSpec) {
            String fullName = resResSpec.toString();
            return fullName.substring(0, fullName.indexOf("/"));
        }
    };
}
