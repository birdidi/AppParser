package com.birdidi.app.apktool;

import android.util.TypedValue;
import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResID;
import brut.androlib.res.decoder.AXmlResourceParser;
import brut.androlib.res.decoder.ResAttrDecoder;
import brut.androlib.res.decoder.StringBlock;
import brut.util.ExtDataInput;
import com.google.common.io.LittleEndianDataInputStream;
import org.xmlpull.v1.XmlPullParserException;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class ApkAXmlResourceParser extends AXmlResourceParser {
    private ExtDataInput m_reader;
    private ResAttrDecoder mAttrDecoder;
    private AndrolibException mFirstError;
    private boolean m_operational = false;
    private StringBlock m_strings;
    private int[] m_resourceIDs;
    private ApkAXmlResourceParser.NamespaceStack m_namespaces = new ApkAXmlResourceParser.NamespaceStack();
    private String android_ns = "http://schemas.android.com/apk/res/android";
    private boolean m_decreaseDepth;
    private int m_event;
    private int m_lineNumber;
    private int m_name;
    private int m_namespaceUri;
    private int[] m_attributes;
    private int m_idAttribute;
    private int m_classAttribute;
    private int m_styleAttribute;
    private static final Logger LOGGER = Logger.getLogger(AXmlResourceParser.class.getName());

    public ApkAXmlResourceParser() {
        this.resetEventInfo();
    }

    public AndrolibException getFirstError() {
        return this.mFirstError;
    }

    public ResAttrDecoder getAttrDecoder() {
        return this.mAttrDecoder;
    }

    public void setAttrDecoder(ResAttrDecoder attrDecoder) {
        this.mAttrDecoder = attrDecoder;
    }

    public void open(InputStream stream) {
        this.close();
        if (stream != null) {
            this.m_reader = new ExtDataInput((DataInput) new LittleEndianDataInputStream(stream));
        }

    }

    public void close() {
        if (this.m_operational) {
            this.m_operational = false;
            this.m_reader = null;
            this.m_strings = null;
            this.m_resourceIDs = null;
            this.m_namespaces.reset();
            this.resetEventInfo();
        }
    }

    public int next() throws XmlPullParserException, IOException {
        if (this.m_reader == null) {
            throw new XmlPullParserException("Parser is not opened.", this, (Throwable) null);
        } else {
            try {
                this.doNext();
                return this.m_event;
            } catch (IOException var2) {
                this.close();
                throw var2;
            }
        }
    }

    public int nextToken() throws XmlPullParserException, IOException {
        return this.next();
    }

    public int getDepth() {
        return this.m_namespaces.getDepth() - 1;
    }

    public int getEventType() throws XmlPullParserException {
        return this.m_event;
    }

    public int getLineNumber() {
        return this.m_lineNumber;
    }

    public String getName() {
        String name = this.m_name != -1 && (this.m_event == 2 || this.m_event == 3) ? this.m_strings.getString(this.m_name) : null;
        return name;
    }

    public String getText() {
        return this.m_name != -1 && this.m_event == 4 ? this.m_strings.getString(this.m_name) : null;
    }

    public String getNamespace() {
        return this.m_strings.getString(this.m_namespaceUri);
    }

    public String getPositionDescription() {
        return "XML line #" + this.getLineNumber();
    }

    public int getNamespaceCount(int depth) throws XmlPullParserException {
        return this.m_namespaces.getAccumulatedCount(depth);
    }

    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        int prefix = this.m_namespaces.getPrefix(pos);
        return this.m_strings.getString(prefix);
    }

    public String getNamespaceUri(int pos) throws XmlPullParserException {
        int uri = this.m_namespaces.getUri(pos);
        return this.m_strings.getString(uri);
    }

    public int getAttributeCount() {
        return this.m_event != 2 ? -1 : this.m_attributes.length / 5;
    }

    public String getAttributeNamespace(int index) {
        int offset = this.getAttributeOffset(index);
        int namespace = this.m_attributes[offset + 0];
        if (namespace == -1) {
            return "";
        } else {
            String value = this.m_strings.getString(namespace);
            if (value.length() == 0) {
                ResID resourceId = new ResID(this.getAttributeNameResource(index));
                if (resourceId.package_ == 127) {
                    value = this.getNonDefaultNamespaceUri();
                } else {
                    value = this.android_ns;
                }
            }

            return value;
        }
    }

    private String getNonDefaultNamespaceUri() {
        int offset = this.m_namespaces.getCurrentCount() + 1;
        String prefix = this.m_strings.getString(this.m_namespaces.get(offset, true));
        return !prefix.equalsIgnoreCase("android") ? this.m_strings.getString(this.m_namespaces.get(offset, false)) : this.android_ns;
    }

    public String getAttributePrefix(int index) {
        int offset = this.getAttributeOffset(index);
        int uri = this.m_attributes[offset + 0];
        int prefix = this.m_namespaces.findPrefix(uri);
        return prefix == -1 ? "" : this.m_strings.getString(prefix);
    }

    public String getAttributeName(int index) {
        int offset = this.getAttributeOffset(index);
        int name = this.m_attributes[offset + 1];
        if (name == -1) {
            return "";
        } else {
            String value = this.m_strings.getString(name);
//            System.out.println("attribute name = " + value);
            if (value.length() != 0) {
                return value;
            } else {
                try {
                    value = this.mAttrDecoder.decodeManifestAttr(this.getAttributeNameResource(index));
                } catch (AndrolibException var6) {
                    value = "";
                }

                return value;
            }
        }
    }

    public int getAttributeNameResource(int index) {
        int offset = this.getAttributeOffset(index);
        int name = this.m_attributes[offset + 1];
        return this.m_resourceIDs != null && name >= 0 && name < this.m_resourceIDs.length ? this.m_resourceIDs[name] : 0;
    }

    public String getAttributeValue(int index) {
        int offset = this.getAttributeOffset(index);
        int valueType = this.m_attributes[offset + 3];
        int valueData = this.m_attributes[offset + 4];
        int valueRaw = this.m_attributes[offset + 2];
//        if (this.mAttrDecoder != null) {
//            try {
//                String value = this.mAttrDecoder.decode(valueType, valueData, valueRaw == -1 ? null : ResXmlEncoders.escapeXmlChars(this.m_strings.getString(valueRaw)), this.getAttributeNameResource(index));
//                System.out.println(valueType + ", " + valueData + ", " + valueRaw + ", " + this.m_strings.getString(valueRaw) + " = " + value);
//                return value;
//            } catch (AndrolibException var7) {
//                this.setFirstError(var7);
//                LOGGER.log(Level.WARNING, String.format("Could not decode attr value, using undecoded value instead: ns=%s, name=%s, value=0x%08x", this.getAttributePrefix(index), this.getAttributeName(index), valueData), var7);
//            }
//        }

        String value = null;
        if (valueRaw != -1) {
            value = this.m_strings.getString(valueRaw);
        } else {
            value = TypedValue.coerceToString(valueType, valueData);
        }
//        System.out.println("attribute value : " + value);
        return value;
    }

    public void setInput(InputStream stream, String inputEncoding) throws XmlPullParserException {
        this.open(stream);
    }

    public String getInputEncoding() {
        return null;
    }

    public int getColumnNumber() {
        return -1;
    }

    public Object getProperty(String name) {
        return null;
    }

    public boolean getFeature(String feature) {
        return false;
    }

    private final int getAttributeOffset(int index) {
        if (this.m_event != 2) {
            throw new IndexOutOfBoundsException("Current event is not START_TAG.");
        } else {
            int offset = index * 5;
            if (offset >= this.m_attributes.length) {
                throw new IndexOutOfBoundsException("Invalid attribute index (" + index + ").");
            } else {
                return offset;
            }
        }
    }

    private final void resetEventInfo() {
        this.m_event = -1;
        this.m_lineNumber = -1;
        this.m_name = -1;
        this.m_namespaceUri = -1;
        this.m_attributes = null;
        this.m_idAttribute = -1;
        this.m_classAttribute = -1;
        this.m_styleAttribute = -1;
    }

    private final void doNext() throws IOException {
        if (this.m_strings == null) {
            this.m_reader.skipCheckInt(524291);
            this.m_reader.skipInt();
            this.m_strings = StringBlock.read(this.m_reader);
            this.m_namespaces.increaseDepth();
            this.m_operational = true;
        }

        if (this.m_event != 1) {
            int event = this.m_event;
            this.resetEventInfo();

            while (true) {
                if (this.m_decreaseDepth) {
                    this.m_decreaseDepth = false;
                    this.m_namespaces.decreaseDepth();
                }

                if (event == 3 && this.m_namespaces.getDepth() == 1 && this.m_namespaces.getCurrentCount() == 0) {
                    this.m_event = 1;
                } else {
                    int chunkType;
                    if (event == 0) {
                        chunkType = 1048834;
                    } else {
                        chunkType = this.m_reader.readInt();
                    }

                    int lineNumber;
                    if (chunkType == 524672) {
                        lineNumber = this.m_reader.readInt();
                        if (lineNumber >= 8 && lineNumber % 4 == 0) {
                            this.m_resourceIDs = this.m_reader.readIntArray(lineNumber / 4 - 2);
                            continue;
                        }

                        throw new IOException("Invalid resource ids size (" + lineNumber + ").");
                    }

                    if (chunkType < 1048832 || chunkType > 1048836) {
                        throw new IOException("Invalid chunk type (" + chunkType + ").");
                    }

                    if (chunkType == 1048834 && event == -1) {
                        this.m_event = 0;
                    } else {
                        this.m_reader.skipInt();
                        lineNumber = this.m_reader.readInt();
                        this.m_reader.skipInt();
                        int attributeCount;
                        int i;
                        if (chunkType == 1048832 || chunkType == 1048833) {
                            if (chunkType == 1048832) {
                                attributeCount = this.m_reader.readInt();
                                i = this.m_reader.readInt();
                                this.m_namespaces.push(attributeCount, i);
                            } else {
                                this.m_reader.skipInt();
                                this.m_reader.skipInt();
                                this.m_namespaces.pop();
                            }
                            continue;
                        }

                        this.m_lineNumber = lineNumber;
                        if (chunkType == 1048834) {
                            this.m_namespaceUri = this.m_reader.readInt();
                            this.m_name = this.m_reader.readInt();
                            this.m_reader.skipInt();
                            attributeCount = this.m_reader.readInt();
                            this.m_idAttribute = (attributeCount >>> 16) - 1;
                            attributeCount &= 65535;
                            this.m_classAttribute = this.m_reader.readInt();
                            this.m_styleAttribute = (this.m_classAttribute >>> 16) - 1;
                            this.m_classAttribute = (this.m_classAttribute & '\uffff') - 1;
                            this.m_attributes = this.m_reader.readIntArray(attributeCount * 5);

                            for (i = 3; i < this.m_attributes.length; i += 5) {
                                this.m_attributes[i] >>>= 24;
                            }

                            this.m_namespaces.increaseDepth();
                            this.m_event = 2;
                        } else if (chunkType == 1048835) {
                            this.m_namespaceUri = this.m_reader.readInt();
                            this.m_name = this.m_reader.readInt();
                            this.m_event = 3;
                            this.m_decreaseDepth = true;
                        } else {
                            if (chunkType != 1048836) {
                                continue;
                            }

                            this.m_name = this.m_reader.readInt();
                            this.m_reader.skipInt();
                            this.m_reader.skipInt();
                            this.m_event = 4;
                        }
                    }
                }

                return;
            }
        }
    }

    private void setFirstError(AndrolibException error) {
        if (this.mFirstError == null) {
            this.mFirstError = error;
        }

    }

    private static final class NamespaceStack {
        private int[] m_data = new int[32];
        private int m_dataLength;
        private int m_count;
        private int m_depth;

        public NamespaceStack() {
        }

        public final void reset() {
            this.m_dataLength = 0;
            this.m_count = 0;
            this.m_depth = 0;
        }

        public final int getCurrentCount() {
            if (this.m_dataLength == 0) {
                return 0;
            } else {
                int offset = this.m_dataLength - 1;
                return this.m_data[offset];
            }
        }

        public final int getAccumulatedCount(int depth) {
            if (this.m_dataLength != 0 && depth >= 0) {
                if (depth > this.m_depth) {
                    depth = this.m_depth;
                }

                int accumulatedCount = 0;

                for (int offset = 0; depth != 0; --depth) {
                    int count = this.m_data[offset];
                    accumulatedCount += count;
                    offset += 2 + count * 2;
                }

                return accumulatedCount;
            } else {
                return 0;
            }
        }

        public final void push(int prefix, int uri) {
            if (this.m_depth == 0) {
                this.increaseDepth();
            }

            this.ensureDataCapacity(2);
            int offset = this.m_dataLength - 1;
            int count = this.m_data[offset];
            this.m_data[offset - 1 - count * 2] = count + 1;
            this.m_data[offset] = prefix;
            this.m_data[offset + 1] = uri;
            this.m_data[offset + 2] = count + 1;
            this.m_dataLength += 2;
            ++this.m_count;
        }

        public final boolean pop() {
            if (this.m_dataLength == 0) {
                return false;
            } else {
                int offset = this.m_dataLength - 1;
                int count = this.m_data[offset];
                if (count == 0) {
                    return false;
                } else {
                    --count;
                    offset -= 2;
                    this.m_data[offset] = count;
                    offset -= 1 + count * 2;
                    this.m_data[offset] = count;
                    this.m_dataLength -= 2;
                    --this.m_count;
                    return true;
                }
            }
        }

        public final int getPrefix(int index) {
            return this.get(index, true);
        }

        public final int getUri(int index) {
            return this.get(index, false);
        }

        public final int findPrefix(int uri) {
            return this.find(uri, false);
        }

        public final int getDepth() {
            return this.m_depth;
        }

        public final void increaseDepth() {
            this.ensureDataCapacity(2);
            int offset = this.m_dataLength;
            this.m_data[offset] = 0;
            this.m_data[offset + 1] = 0;
            this.m_dataLength += 2;
            ++this.m_depth;
        }

        public final void decreaseDepth() {
            if (this.m_dataLength != 0) {
                int offset = this.m_dataLength - 1;
                int count = this.m_data[offset];
                if (offset - 1 - count * 2 != 0) {
                    this.m_dataLength -= 2 + count * 2;
                    this.m_count -= count;
                    --this.m_depth;
                }
            }
        }

        private void ensureDataCapacity(int capacity) {
            int available = this.m_data.length - this.m_dataLength;
            if (available <= capacity) {
                int newLength = (this.m_data.length + available) * 2;
                int[] newData = new int[newLength];
                System.arraycopy(this.m_data, 0, newData, 0, this.m_dataLength);
                this.m_data = newData;
            }
        }

        private final int find(int prefixOrUri, boolean prefix) {
            if (this.m_dataLength == 0) {
                return -1;
            } else {
                int offset = this.m_dataLength - 1;

                for (int i = this.m_depth; i != 0; --i) {
                    int count = this.m_data[offset];

                    for (offset -= 2; count != 0; --count) {
                        if (prefix) {
                            if (this.m_data[offset] == prefixOrUri) {
                                return this.m_data[offset + 1];
                            }
                        } else if (this.m_data[offset + 1] == prefixOrUri) {
                            return this.m_data[offset];
                        }

                        offset -= 2;
                    }
                }

                return -1;
            }
        }

        private final int get(int index, boolean prefix) {
            if (this.m_dataLength != 0 && index >= 0) {
                int offset = 0;

                for (int i = this.m_depth; i != 0; --i) {
                    int count = this.m_data[offset];
                    if (index < count) {
                        offset += 1 + index * 2;
                        if (!prefix) {
                            ++offset;
                        }

                        return this.m_data[offset];
                    }

                    index -= count;
                    offset += 2 + count * 2;
                }

                return -1;
            } else {
                return -1;
            }
        }
    }
}
