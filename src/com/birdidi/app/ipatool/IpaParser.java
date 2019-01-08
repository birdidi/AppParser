package com.birdidi.app.ipatool;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * IPA解析器
 * <p>
 * CFBundleDisplayName - 应用名称
 * CFBundleIdentifier - 包名
 * CFBundleVersion - 版本号
 * CFBundleShortVersionString - 版本名称
 * CFBundleIcons - 应用图标列表
 * -CFBundlePrimaryIcon
 * -CFBundleIconFiles - 图标文件列表
 */
public class IpaParser {


    public static File extractPlist(File ipaFile, File plistDestDir) throws IOException {
        if (!plistDestDir.exists()) {
            plistDestDir.mkdirs();
        }
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(ipaFile);
            Enumeration<?> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry ele = (ZipEntry) enumeration.nextElement();
                final String entryName = ele.getName();
                if (entryName.endsWith(".app/Info.plist")) {
                    File plistDest = new File(plistDestDir, "Info.plist");
                    FileUtils.writeByteArrayToFile(plistDest, read(zipFile.getInputStream(ele)));
                    return plistDest;
                }
            }
        } finally {
            IOUtils.closeQuietly(zipFile);
        }
        return null;
    }

    public static IpaInfo decodeIpa(File ipaFile, File plistTempDir) throws Exception {
        IpaInfo ipaInfo = null;
        File plist = extractPlist(ipaFile, plistTempDir);
        if (plist != null) {
            ipaInfo = decodePlist(plist);
            return ipaInfo;
        }
        return ipaInfo;
    }

    public static IpaInfo decodePlist(File plistFile) throws Exception {
        IpaInfo info = new IpaInfo();
        NSDictionary plistDict = (NSDictionary) PropertyListParser.parse(plistFile);

        info.appName = plistDict.get("CFBundleDisplayName").toString();// - 应用名称
        info.bundleId = plistDict.get("CFBundleIdentifier").toString();//  - 包名
        info.bundleVersionCode = plistDict.get("CFBundleVersion").toString();//  - 版本号
        info.bundleVersionName = plistDict.get("CFBundleShortVersionString").toString();//  - 版本名称
        plistDict.get("CFBundleIcons");//  - 应用图标列表
        plistDict.get("CFBundlePrimaryIcon");//
        plistDict.get("CFBundleIconFiles");//  - 图标文件列表

        NSDictionary icons = (NSDictionary) plistDict.get("CFBundleIcons");
        NSDictionary iconPrimaryIcon = (NSDictionary) icons.get("CFBundlePrimaryIcon");
        NSArray iconFiles = (NSArray) iconPrimaryIcon.get("CFBundleIconFiles");
        NSObject iconName = iconPrimaryIcon.get("CFBundleIconName");
        info.appIcon = iconName.toString();
        if (iconFiles != null && iconFiles.count() > 0) {
            info.appIcon = iconFiles.getArray()[iconFiles.count() - 1].toString();
            info.appIcons = new ArrayList<String>();
            for (NSObject iconFile : iconFiles.getArray()) {
                info.appIcons.add(iconFile.toString());
            }
        }
        return info;
    }

    public static IpaInfo decodeIpaWithResources(File ipaFile, File exportedDir) throws Exception {
        IpaInfo info = decodeIpa(ipaFile, exportedDir);
        //解析图标文件
        if (!StringUtils.isEmpty(info.appIcon)) {
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(ipaFile);
                Enumeration<?> enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry ele = (ZipEntry) enumeration.nextElement();
                    File tmp = new File(exportedDir, ele.getName());
                    final String fileName = tmp.getName();
                    File dest = new File(exportedDir, fileName);
                    if (!ele.isDirectory() && fileName.startsWith(info.appIcon)) {//图标
                        System.out.println(dest);
                        FileUtils.writeByteArrayToFile(dest, read(zipFile.getInputStream(ele)));
                        try {
                            ImageIO.read(dest);
                        } catch (IIOException e) {
                            try {
                                File decoded = new File(exportedDir, fileName + "_decoded.png");
                                IPngConverter iPngConverter = new IPngConverter(dest, decoded);
                                iPngConverter.convert();
                                dest.delete();
                                dest = decoded;
                            } catch (Exception e1) {
                            }
                        }
                        info.appIcon = dest.getAbsolutePath();
//                        int index = -1;
//                        for (int i = 0, len = info.appIcons.size(); i < len; i++) {
//                            String icon = info.appIcons.get(i);
//                            if (fileName.startsWith(icon)) {
//                                index = i;
//                                break;
//                            }
//                        }
//                        if (index != -1) {
//                            info.appIcons.set(index, dest.getAbsolutePath());
//                        }
                        break;
                    }
                }
            } finally {
                IOUtils.closeQuietly(zipFile);
            }
        }
        return info;
    }

    private static byte[] read(InputStream inputStream) {
        ByteArrayOutputStream baos = null;
        byte[] buffer = new byte[1024];
        int len;

        try {
            baos = new ByteArrayOutputStream();
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(baos);
        }
        return null;
    }
}
