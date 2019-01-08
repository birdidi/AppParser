package com.birdidi.app.apktool;

import brut.androlib.Androlib;
import brut.androlib.AndrolibException;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResValuesFile;
import brut.androlib.res.decoder.AXmlResourceParser;
import brut.androlib.res.decoder.Res9patchStreamDecoder;
import brut.androlib.res.decoder.ResAttrDecoder;
import brut.androlib.res.decoder.ResFileDecoder;
import brut.androlib.res.decoder.ResRawStreamDecoder;
import brut.androlib.res.decoder.ResStreamDecoderContainer;
import brut.androlib.res.util.ExtMXSerializer;
import brut.androlib.res.util.ExtXmlSerializer;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.directory.FileDirectory;
import brut.util.Duo;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;

public class ApkParser {

    private ResTable mResTable;
    private Androlib mAndrolib;
    private static final Logger LOGGER = Logger.getLogger(AndrolibResources.class.getName());
    private String mPackageRenamed = null;
    private String mPackageId = null;
    private static final String[] IGNORED_PACKAGES = new String[]{"android", "com.htc", "miui", "com.lge", "com.lge.internal", "yi", "com.miui.core", "flyme", "air.com.adobe.appentry", "FFFFFFFFFFFFFFFFFFFFFF"};
    private static final String[] ALLOWED_PACKAGES = new String[]{"com.miui"};

    public ApkParser() {
        this(null);
    }

    public ApkParser(File frmageworkFolder) {
        mAndrolib = new Androlib();
        if (frmageworkFolder != null) {
            if (!frmageworkFolder.exists()) {
                frmageworkFolder.mkdirs();
            }
            mAndrolib.apkOptions.frameworkFolderLocation = frmageworkFolder.getAbsolutePath();
        }
    }

    public void decodeResourcesFull(ExtFile apkFile, File outDir, ResTable resTable) throws AndrolibException {
        this.mAndrolib.decodeResourcesFull(apkFile, outDir, resTable);
    }

    public ResTable decodeManifestLite(ExtFile apkFile, ResTableFilter filter) throws Exception {
        ExtFile extFile = new ExtFile(apkFile);
        ResTable resTable = getResTable(extFile);

        ResTable pureTabel = getResTable(extFile);
        decodeManifest(pureTabel, apkFile, true);

//        Map<String, String> manifestDict = extract.getManifestDict();
//        String appNameId = manifestDict.get("application#label");
//        String appIconId = manifestDict.get("application#icon");
//        String packageName = manifestDict.get("manifest#package");
//        String versionCode = manifestDict.get("manifest#versionCode");
//        String versionName = manifestDict.get("manifest#versionName");
//        String minSdkVersion = manifestDict.get("uses-sdk#minSdkVersion");
//        String targetSdkVersion = manifestDict.get("uses-sdk#targetSdkVersion");
//        ResResSpec appIconResSpec = resTable.getResSpec(Integer.valueOf(appIconId));

        String appNameIdRaw = pureTabel.getSdkInfo().get("label");
        String appIconIdRaw = pureTabel.getSdkInfo().get("icon");
        String appNameId = appNameIdRaw.replace("@", "");
        String appIconId = appIconIdRaw.replace("@", "");

        String packageName = pureTabel.getPackageRenamed();
        String versionCode = pureTabel.getVersionInfo().versionCode;
        String versionName = pureTabel.getVersionInfo().versionName;
        String minSdkVersion = pureTabel.getSdkInfo().get("minSdkVersion");
        String targetSdkVersion = pureTabel.getSdkInfo().get("targetSdkVersion");
        ResResSpec appIconResSpec = resTable.getResSpec(Integer.valueOf(appIconId));

        resTable.setPackageOriginal(packageName);
        resTable.setPackageRenamed(packageName);
        resTable.setVersionCode(versionCode);
        resTable.setVersionName(versionName);
        resTable.addSdkInfo("minSdkVersion", minSdkVersion);
        resTable.addSdkInfo("targetSdkVersion", targetSdkVersion);
        ResResSpec appNameResSpec = null;
        if (isIntegerNum(appNameId)) {
            try {
                appNameResSpec = resTable.getResSpec(Integer.valueOf(appNameId));
                resTable.addSdkInfo("label", appNameResSpec.getName());
                resTable.addSdkInfo("labelType", appNameResSpec.getType().getName());
            } catch (AndrolibException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        resTable.addSdkInfo("icon", appIconResSpec.getName());
        resTable.addSdkInfo("iconType", appIconResSpec.getType().getName());

        if (filter != null) {
            filter.filter(resTable);
        }

        if (appNameResSpec == null) {
            resTable.addSdkInfo("label", appNameIdRaw);
            resTable.addSdkInfo("labelType", "raw");
        }
        return resTable;
    }

    public void decodeManifest(ResTable resTable, ExtFile apkFile, boolean isPure) throws AndrolibException {
        Duo<ResFileDecoder, AXmlResourceParser> duo = this.getManifestFileDecoder(isPure);
        ApkResFileDecoder fileDecoder = (ApkResFileDecoder) duo.m1;
        ((AXmlResourceParser) duo.m2).setAttrDecoder(new ResAttrDecoder());
        ResAttrDecoder attrDecoder = ((AXmlResourceParser) duo.m2).getAttrDecoder();
        attrDecoder.setCurrentPackage(new ResPackage(resTable, 0, (String) null));

        try {
            InputStream is = apkFile.getDirectory().getFileInput("AndroidManifest.xml");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            fileDecoder.mDecoders.getDecoder("xml").decode(is, os);
        } catch (DirectoryException var10) {
            throw new AndrolibException(var10);
        }
    }

    public void decodeManifestWithResources(ResTable resTable, ExtFile apkFile, File outDir) throws AndrolibException {
        Duo<ResFileDecoder, AXmlResourceParser> duo = this.getResFileDecoder();
        ApkResFileDecoder fileDecoder = (ApkResFileDecoder) duo.m1;
        ResAttrDecoder attrDecoder = ((AXmlResourceParser) duo.m2).getAttrDecoder();
        attrDecoder.setCurrentPackage((ResPackage) resTable.listMainPackages().iterator().next());
        Object var8 = null;

        try {
            Directory inApk = apkFile.getDirectory();
            Directory out = new FileDirectory(outDir);
            LOGGER.info("Decoding AndroidManifest.xml with resources...");
            fileDecoder.decodeManifest(inApk, "AndroidManifest.xml", out, "AndroidManifest.xml");
            if (!resTable.getAnalysisMode()) {
                this.adjustPackageManifest(resTable, outDir.getAbsolutePath() + File.separator + "AndroidManifest.xml");
                ResXmlPatcher.removeManifestVersions(new File(outDir.getAbsolutePath() + File.separator + "AndroidManifest.xml"));
                this.mPackageId = String.valueOf(resTable.getPackageId());
            }

        } catch (DirectoryException var11) {
            throw new AndrolibException(var11);
        }
    }

    public void decode(ResTable resTable, ExtFile apkFile, File outDir) throws AndrolibException, DirectoryException {
        Duo<ResFileDecoder, AXmlResourceParser> duo = this.getResFileDecoder();
        ApkResFileDecoder fileDecoder = (ApkResFileDecoder) duo.m1;
        ResAttrDecoder attrDecoder = ((AXmlResourceParser) duo.m2).getAttrDecoder();
        attrDecoder.setCurrentPackage((ResPackage) resTable.listMainPackages().iterator().next());
        Directory in = null;
        outDir.mkdirs();
//        fileDecoder.decodeManifest(apkFile.getDirectory(), "AndroidManifest.xml", new ExtFile(outDir).getDirectory(), "AndroidManifest.xml");
        Directory out;
        try {
            out = new FileDirectory(outDir);
            Directory inApk = apkFile.getDirectory();
            out = out.createDir("res");
            if (inApk.containsDir("res")) {
                in = inApk.getDir("res");
            }

            if (in == null && inApk.containsDir("r")) {
                in = inApk.getDir("r");
            }

            if (in == null && inApk.containsDir("R")) {
                in = inApk.getDir("R");
            }
        } catch (DirectoryException var15) {
            throw new AndrolibException(var15);
        }

        ExtMXSerializer xmlSerializer = this.getResXmlSerializer();
        Iterator var11 = resTable.listMainPackages().iterator();

        while (var11.hasNext()) {
            ResPackage pkg = (ResPackage) var11.next();
            attrDecoder.setCurrentPackage(pkg);
            LOGGER.info("Decoding file-resources...");
            Iterator var13 = pkg.listFiles().iterator();

            while (var13.hasNext()) {
                ResResource res = (ResResource) var13.next();
                fileDecoder.decode(res, in, out);
            }

            LOGGER.info("Decoding values */* XMLs...");
            var13 = pkg.listValuesFiles().iterator();

            while (var13.hasNext()) {
                ResValuesFile valuesFile = (ResValuesFile) var13.next();
                this.generateValuesFile(valuesFile, out, xmlSerializer);
            }

            this.generatePublicXml(pkg, out, xmlSerializer);
        }

        AndrolibException decodeError = ((AXmlResourceParser) duo.m2).getFirstError();
        if (decodeError != null) {
            throw decodeError;
        }
    }

    private void generatePublicXml(ResPackage pkg, Directory out, XmlSerializer serial) throws AndrolibException {
        try {
            OutputStream outStream = out.getFileOutput("values/public.xml");
            serial.setOutput(outStream, (String) null);
            serial.startDocument((String) null, (Boolean) null);
            serial.startTag((String) null, "resources");
            Iterator var5 = pkg.listResSpecs().iterator();

            while (var5.hasNext()) {
                ResResSpec spec = (ResResSpec) var5.next();
                serial.startTag((String) null, "public");
                serial.attribute((String) null, "type", spec.getType().getName());
                serial.attribute((String) null, "name", spec.getName());
                serial.attribute((String) null, "appId", String.format("0x%08x", spec.getId().id));
                serial.endTag((String) null, "public");
            }

            serial.endTag((String) null, "resources");
            serial.endDocument();
            serial.flush();
            outStream.close();
        } catch (DirectoryException var7) {
            throw new AndrolibException("Could not generate public.xml file", var7);
        } catch (IOException var7) {
            throw new AndrolibException("Could not generate public.xml file", var7);
        }
    }

    private void generateValuesFile(ResValuesFile valuesFile, Directory out, ExtXmlSerializer serial) throws AndrolibException {
        try {
            OutputStream outStream = out.getFileOutput(valuesFile.getPath());
            serial.setOutput(outStream, (String) null);
            serial.startDocument((String) null, (Boolean) null);
            serial.startTag((String) null, "resources");
            Iterator var5 = valuesFile.listResources().iterator();

            while (var5.hasNext()) {
                ResResource res = (ResResource) var5.next();
                if (!valuesFile.isSynthesized(res)) {
                    ((ResValuesXmlSerializable) res.getValue()).serializeToResValuesXml(serial, res);
                }
            }

            serial.endTag((String) null, "resources");
            serial.newLine();
            serial.endDocument();
            serial.flush();
            outStream.close();
        } catch (DirectoryException var7) {
            throw new AndrolibException("Could not generate: " + valuesFile.getPath(), var7);
        } catch (IOException var7) {
            throw new AndrolibException("Could not generate: " + valuesFile.getPath(), var7);
        }
    }

    public void adjustPackageManifest(ResTable resTable, String filePath) throws AndrolibException {
        ResPackage resPackage = resTable.getCurrentResPackage();
        String packageOriginal = resPackage.getName();
        this.mPackageRenamed = resTable.getPackageRenamed();
        resTable.setPackageId(resPackage.getId());
        resTable.setPackageOriginal(packageOriginal);
        if (!packageOriginal.equalsIgnoreCase(this.mPackageRenamed) && (!Arrays.asList(IGNORED_PACKAGES).contains(packageOriginal) || Arrays.asList(ALLOWED_PACKAGES).contains(this.mPackageRenamed))) {
            LOGGER.info("Renamed manifest package found! Replacing " + this.mPackageRenamed + " with " + packageOriginal);
            ResXmlPatcher.renameManifestPackage(new File(filePath), packageOriginal);
        } else {
            LOGGER.info("Regular manifest package...");
        }

    }

    public Duo<ResFileDecoder, AXmlResourceParser> getResFileDecoder() {
        ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();
        decoders.setDecoder("raw", new ResRawStreamDecoder());
        decoders.setDecoder("9patch", new Res9patchStreamDecoder());
        AXmlResourceParser axmlParser = new AXmlResourceParser();
        axmlParser.setAttrDecoder(new ResAttrDecoder());
        decoders.setDecoder("xml", new ApkXmlPullStreamDecoder(axmlParser, this.getResXmlSerializer()));
        return new Duo(new ApkResFileDecoder(decoders), axmlParser);
    }

    public Duo<ResFileDecoder, AXmlResourceParser> getManifestFileDecoder(boolean useSelf) {
        ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();
        AXmlResourceParser axmlParser;
        if (useSelf) {
            axmlParser = new ApkAXmlResourceParser();
        } else {
            axmlParser = new AXmlResourceParser();
        }
        decoders.setDecoder("xml", new ApkXmlPullStreamDecoder(axmlParser, this.getResXmlSerializer()));
        return new Duo(new ApkResFileDecoder(decoders), axmlParser);
    }

    public ExtMXSerializer getResXmlSerializer() {
        ExtMXSerializer serial = new ExtMXSerializer();
        serial.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "    ");
        serial.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", System.getProperty("line.separator"));
        serial.setProperty("DEFAULT_ENCODING", "utf-8");
        serial.setDisabledAttrEscape(true);
        return serial;
    }

    public ResTable getResTable(ExtFile apkFile) throws AndrolibException {
        if (mResTable == null) {
//            if (!this.hasResources()) {
//                throw new AndrolibException("Apk doesn't containt resources.arsc file");
//            }

            AndrolibResources.sKeepBroken = false;
            mResTable = mAndrolib.getResTable(apkFile);
        }

        return mResTable;
    }

    private static boolean isIntegerNum(String str) {
        return !StringUtils.isEmpty(str) && (str.matches("^-?\\d+$") || str.matches("^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$"));
    }
}
