package com.birdidi.app.apktool;

import com.android.apksig.ApkVerifier;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SignatureUtil {

    private static java.security.cert.Certificate[] loadCertificates(JarFile jarFile, JarEntry je, byte[] readBuffer) {
        try {
            InputStream is = jarFile.getInputStream(je);
            while (is.read(readBuffer, 0, readBuffer.length) != -1) {

            }
            is.close();
            return (java.security.cert.Certificate[]) (je != null ? je.getCertificates() : null);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Exception reading " + je.getName() + " in " + jarFile.getName() + ": " + e);
        }
        return null;
    }

    public static String getKeyCert(File apkFile, String digestType) {
        String finger = null;
        String javaVer = System.getProperty("java.version");
        try {
            if (!StringUtils.isEmpty(javaVer)) {
                if (javaVer.startsWith("1.8")) {
                    finger = SignatureUtil.fetchCerts(apkFile, digestType);
                } else {
                    finger = SignatureUtil.getSignaturePrintCertCmd(apkFile, digestType);
                }
            } else {
                finger = SignatureUtil.fetchCerts(apkFile, digestType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finger;
    }

    public static String getSignaturePrintCertCmd(File apkFile, String digestType) {
        String key = "SHA1";
        String printcert = null;
        if ("sha-1".equalsIgnoreCase(digestType)) {
            key = "SHA1";
        } else if ("sha-256".equalsIgnoreCase(digestType)) {
            key = "SHA256";
        } else {
            key = "MD5";
        }
        Process process = null;
        try {
            String os = System.getProperty("os.name").toLowerCase();
//            String[] cmd;
            if (os.contains("windows")) {
//                cmd = new String[]{"cmd", "/c", "start", "/b", "keytool -list -printcert -jarfile \"" + apkFile.getAbsolutePath() + "\" | findstr \"" + key + "\""};
                String cmd = "cmd /c start /b keytool -list -printcert -jarfile \"" + apkFile.getAbsolutePath() + "\" | findstr \"" + key + "\"";
                process = Runtime.getRuntime().exec(cmd);
            } else {
                String[] cmd = new String[]{"keytool", "-list", "-printcert", "-jarfile", apkFile.getAbsolutePath()};
                process = Runtime.getRuntime().exec(cmd);
            }
            System.out.println(apkFile.getAbsolutePath());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(key) || line.contains(key + ":")) {
                    line = line.replaceAll("\\s+|(" + key + ")", "");
                    printcert = line.replace(":", "").toLowerCase();
                    System.out.println(printcert);
                    break;
                }
            }

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.out.println(line);
            }
            int exitVal = process.waitFor();
            System.out.println("Exit : " + exitVal);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return printcert;
    }

    public static String fetchCerts(File apkFile, String digestType) throws Exception {
        com.android.apksig.ApkVerifier.Builder apkVerifierBuilder = new com.android.apksig.ApkVerifier.Builder(apkFile);
        ApkVerifier apkVerifier = apkVerifierBuilder.build();
        ApkVerifier.Result result = apkVerifier.verify();
        MessageDigest digest;
        if ("sha-1".equalsIgnoreCase(digestType)) {
            digest = MessageDigest.getInstance("SHA-1");
        } else if ("sha-256".equalsIgnoreCase(digestType)) {
            digest = MessageDigest.getInstance("SHA-256");
        } else {
            digest = MessageDigest.getInstance("MD5");
        }
        List<X509Certificate> signerCerts = result.getSignerCertificates();
        Iterator certsItr = signerCerts.iterator();
        while (certsItr.hasNext()) {
            X509Certificate signerCert = (X509Certificate) certsItr.next();
            byte[] encodedCert = signerCert.getEncoded();
            String certMD5 = hexDigest(encodedCert, digest);
            return certMD5;
        }
        return null;
    }

    /**
     * 有兼容性问题
     *
     * @param apkFilePath
     * @param digestType
     * @return
     */
    @Deprecated
    public static String getApkSign(String apkFilePath, String digestType) {
        byte[] readBuffer = new byte[8192];
        java.security.cert.Certificate[] certs = null;
        try {
            JarFile jarFile = new JarFile(apkFilePath);
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = (JarEntry) entries.nextElement();
                if (je.isDirectory()) {
                    continue;
                }
                if (je.getName().startsWith("META-INF/")) {
                    continue;
                }
                java.security.cert.Certificate[] localCerts = loadCertificates(jarFile, je, readBuffer);
                if (certs == null) {
                    certs = localCerts;
                } else {
                    for (int i = 0; i < certs.length; i++) {
                        boolean found = false;
                        for (int j = 0; j < localCerts.length; j++) {
                            if (certs[i] != null && certs[i].equals(localCerts[j])) {
                                found = true;
                                break;
                            }
                        }
                        if (!found || certs.length != localCerts.length) {
                            jarFile.close();
                            return null;
                        }
                    }
                }
            }
            jarFile.close();

            byte[] bs =/*sha1.digest(*/certs[0].getEncoded()/*)*/;
            String certSha1 = hexDigest(bs, digestType);
            return certSha1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String hexDigest(byte[] bytes, String digestType) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance(digestType);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        byte[] md5Bytes = md5.digest(bytes);
        return hexDigest(bytes, md5);
    }

    public static String hexDigest(byte[] bytes, MessageDigest digestType) {
        MessageDigest md5 = digestType;
        byte[] md5Bytes = md5.digest(bytes);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16)
                hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }
}
