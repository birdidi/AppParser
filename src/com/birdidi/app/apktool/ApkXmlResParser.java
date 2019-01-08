package com.birdidi.app.apktool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApkXmlResParser {

    public static final String REGEX_RESOURCE_VALUES = "^(values-zh)|^(values)$";
    public static final String REGEX_RESOURCE_DRAWABLE = "^(drawable-)|^(drawable)$";
    public static final String REGEX_RESOURCE_MIPMAP = "^(mipmap-)|^(mipmap)$";

    private File resDir;
    private static final Map<String, FileFilter> RESOURCE_FILTERS = new HashMap<String, FileFilter>();

    static {
        RESOURCE_FILTERS.put("drawable", new ResourceFileFilter(REGEX_RESOURCE_DRAWABLE).isDirectory(true));
        RESOURCE_FILTERS.put("mipmap", new ResourceFileFilter(REGEX_RESOURCE_MIPMAP).isDirectory(true));
    }

    public ApkXmlResParser(File resDir) {
        this.resDir = resDir;
    }

    public File extractResource(String resType, String name, File targetDir) {

        FileFilter fileFilter = RESOURCE_FILTERS.get(resType);
        File[] valuesDirs;
        if (fileFilter != null) {
            valuesDirs = resDir.listFiles(fileFilter);
        } else {
            valuesDirs = resDir.listFiles();
        }

        if (valuesDirs == null || valuesDirs.length < 1) {
            return null;
        }
        List<File> resBucketList = Arrays.asList(valuesDirs);
        Collections.sort(resBucketList, RESOURCE_PRIORITY_COMPATOR);
        File destFile = null;
        for (File valuesDir : resBucketList) {
            File[] resList = valuesDir.listFiles(DRAWABLE_SUFFIX_FILTER);
            if (resList != null && resList.length > 0) {
                for (File res : resList) {
                    final String fileName = res.getName();
                    String ext = FilenameUtils.getExtension(fileName);
                    if (fileName.equals(name + "." + ext)) {
                        try {
                            destFile = new File(targetDir, fileName);
                            FileUtils.copyFile(res, destFile);
                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (destFile != null) {
                    return destFile;
                }
                for (File res : resList) {
                    final String fileName = res.getName();
                    String ext = FilenameUtils.getExtension(fileName);
                    if ((fileName.contains(name) && fileName.endsWith("." + ext))) {
                        try {
                            destFile = new File(targetDir, fileName);
                            FileUtils.copyFile(res, destFile);
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return destFile;
    }

    public String findString(String name) {
        File[] valuesDirs = resDir.listFiles(RES_VALUES_FILTER);
        if (valuesDirs == null || valuesDirs.length < 1) {
            return null;
        }
        // TODO: 2018/11/09 取当前语言环境对应的文本
        List<File> valuesList = Arrays.asList(valuesDirs);
        Collections.sort(valuesList, RESOURCE_PRIORITY_COMPATOR);
        for (File valuesDir : valuesList) {
            File[] xmls = valuesDir.listFiles();
            if (xmls != null && xmls.length > 0) {
                for (File xml : xmls) {
                    try {
                        Element node = (Element) parseDOM(new FileInputStream(xml), "string", "name", name);
                        if (node != null) {
                            return node.getTextContent();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    private Node parseDOM(InputStream in, String tag, String key, String name) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        Document document = null;
        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            document = builder.parse(in);
            Element rootElement = document.getDocumentElement();
            NodeList nodes = rootElement.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);

                if (element.hasAttribute(key) && element.getAttribute(key).equals(name)) {
                    System.out.println(key + " : " + element.getAttribute(key));
                    return element;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final Comparator<File> RESOURCE_PRIORITY_COMPATOR = new SimpleResOptimizedFilter.ResourceOptimizedCompared<File>() {
        @Override
        public String getToken(File file) {
            return file.getName();
        }
    };

    private static final FileFilter DRAWABLE_SUFFIX_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String name = pathname.getName();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".9.png")) {
                return true;
            }
            return false;
        }
    };

    private static final FileFilter RES_VALUES_FILTER = new ResourceFileFilter(REGEX_RESOURCE_VALUES).isDirectory(true);

    private static final class ResourceFileFilter implements FileFilter {

        private String regex;
        private Boolean isDirectory = null;

        public ResourceFileFilter(String regex) {
            this.regex = regex;
        }

        public ResourceFileFilter isDirectory(boolean isDirectory) {
            this.isDirectory = isDirectory;
            return this;
        }

        @Override
        public boolean accept(File pathname) {
            final String fileName = pathname.getName();
            boolean consumed = false;
            if (isDirectory == null) {
                consumed = true;
            } else if (isDirectory && pathname.isDirectory()) {
                consumed = true;
            } else if (!isDirectory && pathname.isFile()) {
                consumed = true;
            }
            if (consumed) {
                Pattern pattern = Pattern.compile(this.regex);
                Matcher matcher = pattern.matcher(fileName);
                if (matcher.find()) {
                    return true;
                }
            }
            return false;
        }
    }
}
