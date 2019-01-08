package com.birdidi.app.apktool;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.decoder.AXmlResourceParser;
import brut.androlib.res.decoder.ResStreamDecoder;
import brut.androlib.res.util.ExtXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.wrapper.XmlPullParserWrapper;
import org.xmlpull.v1.wrapper.XmlPullWrapperFactory;
import org.xmlpull.v1.wrapper.XmlSerializerWrapper;
import org.xmlpull.v1.wrapper.classic.StaticXmlSerializerWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class ApkXmlPullStreamDecoder implements ResStreamDecoder {
    private final XmlPullParser mParser;
    private final ExtXmlSerializer mSerial;
    private static final Logger LOGGER = Logger.getLogger(brut.androlib.res.decoder.XmlPullStreamDecoder.class.getName());

    public ApkXmlPullStreamDecoder(XmlPullParser parser, ExtXmlSerializer serializer) {
        this.mParser = parser;
        this.mSerial = serializer;
    }

    public void decode(InputStream in, OutputStream out) throws AndrolibException {
        try {
            XmlPullWrapperFactory factory = XmlPullWrapperFactory.newInstance();
            XmlPullParserWrapper par = factory.newPullParserWrapper(this.mParser);
            final ResTable resTable = ((AXmlResourceParser) this.mParser).getAttrDecoder().getCurrentPackage().getResTable();
            XmlSerializerWrapper ser = new StaticXmlSerializerWrapper(this.mSerial, factory) {
                boolean hideSdkInfo = false;
                boolean hidePackageInfo = false;

                public void event(XmlPullParser pp) throws XmlPullParserException, IOException {
                    int type = pp.getEventType();
                    if (type == 2) {
                        if ("manifest".equalsIgnoreCase(pp.getName())) {
                            try {
                                this.hidePackageInfo = this.parseManifest(pp);
                            } catch (AndrolibException var5) {
                                ;
                            }
                        } else if ("uses-sdk".equalsIgnoreCase(pp.getName())) {
                            try {
                                this.hideSdkInfo = this.parseAttr(pp);
                                if (this.hideSdkInfo) {
                                    return;
                                }
                            } catch (AndrolibException var4) {
                                ;
                            }
                        } else if ("application".equalsIgnoreCase(pp.getName())) {
                            try {
                                this.parseAttr(pp);
                            } catch (AndrolibException e) {
                                ;
                            }
                        }
                    } else {
                        if (this.hideSdkInfo && type == 3 && "uses-sdk".equalsIgnoreCase(pp.getName())) {
                            return;
                        }

                        if (this.hidePackageInfo && type == 3 && "manifest".equalsIgnoreCase(pp.getName())) {
                            super.event(pp);
                            return;
                        }
                    }

                    super.event(pp);
                }

                private boolean parseManifest(XmlPullParser pp) throws AndrolibException {
                    for (int i = 0; i < pp.getAttributeCount(); ++i) {
                        String attr_name = pp.getAttributeName(i);
                        if (attr_name.equalsIgnoreCase("package")) {
                            resTable.setPackageRenamed(pp.getAttributeValue(i));
                        } else if (attr_name.equalsIgnoreCase("versionCode")) {
                            resTable.setVersionCode(pp.getAttributeValue(i));
                        } else if (attr_name.equalsIgnoreCase("versionName")) {
                            resTable.setVersionName(pp.getAttributeValue(i));
                        }
                    }

                    return true;
                }

                private boolean parseAttr(XmlPullParser pp) throws AndrolibException {
                    for (int i = 0; i < pp.getAttributeCount(); ++i) {
                        String a_ns = "http://schemas.android.com/apk/res/android";
                        String ns = pp.getAttributeNamespace(i);
                        if ("http://schemas.android.com/apk/res/android".equalsIgnoreCase(ns)) {
                            String name = pp.getAttributeName(i);
                            String value = pp.getAttributeValue(i);
                            if (name != null && value != null) {
                                if (name.equalsIgnoreCase("minSdkVersion") || name.equalsIgnoreCase("targetSdkVersion") || name.equalsIgnoreCase("maxSdkVersion")) {
                                    resTable.addSdkInfo(name, value);
                                } else if ("application".equalsIgnoreCase(pp.getName()) && (name.equalsIgnoreCase("icon") || name.equalsIgnoreCase("label"))) {
                                    resTable.addSdkInfo(name, value);
                                }
                            }
                        } else {
                            resTable.clearSdkInfo();
                            if (i >= pp.getAttributeCount()) {
                                return false;
                            }
                        }
                    }

                    return !resTable.getAnalysisMode();
                }
            };
            par.setInput(in, (String) null);
            ser.setOutput(out, (String) null);

            while (par.nextToken() != 1) {
                ser.event(par);
            }

            ser.flush();
        } catch (XmlPullParserException var7) {
            throw new AndrolibException("Could not decode XML", var7);
        } catch (IOException var8) {
            throw new AndrolibException("Could not decode XML", var8);
        }
    }

    public void decodeManifest(InputStream in, OutputStream out) throws AndrolibException {
        this.decode(in, out);
    }
}

