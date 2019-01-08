package com.birdidi.app.apktool;

import brut.androlib.AndrolibException;
import brut.androlib.err.CantFind9PatchChunk;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.value.ResBoolValue;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.decoder.ResFileDecoder;
import brut.androlib.res.decoder.ResStreamDecoderContainer;
import brut.directory.DirUtil;
import brut.directory.Directory;
import brut.directory.DirectoryException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApkResFileDecoder extends ResFileDecoder {
    public final ResStreamDecoderContainer mDecoders;
    private static final Logger LOGGER = Logger.getLogger(ResFileDecoder.class.getName());
    private static final String[] RAW_IMAGE_EXTENSIONS = new String[]{"m4a"};
    private static final String[] RAW_9PATCH_IMAGE_EXTENSIONS = new String[]{"qmg", "spi"};

    public ApkResFileDecoder(ResStreamDecoderContainer decoders) {
        super(decoders);
        this.mDecoders = decoders;
    }

    public void decode(ResResource res, Directory inDir, Directory outDir) throws AndrolibException {
        ResFileValue fileValue = (ResFileValue)res.getValue();
        String inFileName = fileValue.getStrippedPath();
        String outResName = res.getFilePath();
        String typeName = res.getResSpec().getType().getName();
        String ext = null;
        int extPos = inFileName.lastIndexOf(".");
        String outFileName;
        if (extPos == -1) {
            outFileName = outResName;
        } else {
            ext = inFileName.substring(extPos).toLowerCase();
            outFileName = outResName + ext;
        }

        try {
            if (typeName.equals("raw")) {
                this.decode(inDir, inFileName, outDir, outFileName, "raw");
                return;
            }

            if (typeName.equals("font") && !".xml".equals(ext)) {
                this.decode(inDir, inFileName, outDir, outFileName, "raw");
                return;
            }

            if (typeName.equals("drawable") || typeName.equals("mipmap")) {
                String[] var11;
                int var12;
                int var13;
                String extension;
                if (inFileName.toLowerCase().endsWith(".9" + ext)) {
                    outFileName = outResName + ".9" + ext;
                    if (inFileName.toLowerCase().endsWith(".r.9" + ext)) {
                        outFileName = outResName + ".r.9" + ext;
                    }

                    var11 = RAW_9PATCH_IMAGE_EXTENSIONS;
                    var12 = var11.length;

                    for(var13 = 0; var13 < var12; ++var13) {
                        extension = var11[var13];
                        if (inFileName.toLowerCase().endsWith("." + extension)) {
                            this.copyRaw(inDir, outDir, outFileName);
                            return;
                        }
                    }

                    if (inFileName.toLowerCase().endsWith(".xml")) {
                        this.decode(inDir, inFileName, outDir, outFileName, "xml");
                        return;
                    }

                    try {
                        this.decode(inDir, inFileName, outDir, outFileName, "9patch");
                        return;
                    } catch (CantFind9PatchChunk var15) {
                        LOGGER.log(Level.WARNING, String.format("Cant find 9patch chunk in file: \"%s\". Renaming it to *.png.", inFileName), var15);
                        outDir.removeFile(outFileName);
                        outFileName = outResName + ext;
                    }
                }

                var11 = RAW_IMAGE_EXTENSIONS;
                var12 = var11.length;

                for(var13 = 0; var13 < var12; ++var13) {
                    extension = var11[var13];
                    if (inFileName.toLowerCase().endsWith("." + extension)) {
                        this.copyRaw(inDir, outDir, outFileName);
                        return;
                    }
                }

                if (!".xml".equals(ext)) {
                    this.decode(inDir, inFileName, outDir, outFileName, "raw");
                    return;
                }
            }

            this.decode(inDir, inFileName, outDir, outFileName, "xml");
        } catch (AndrolibException var16) {
            LOGGER.log(Level.SEVERE, String.format("Could not decode file, replacing by FALSE value: %s", inFileName), var16);
            res.replace(new ResBoolValue(false, 0, (String)null));
        }

    }

    public void decode(Directory inDir, String inFileName, Directory outDir, String outFileName, String decoder) throws AndrolibException {
        try {
            InputStream in = inDir.getFileInput(inFileName);
            Throwable var7 = null;

            try {
                OutputStream out = outDir.getFileOutput(outFileName);
                Throwable var9 = null;

                try {
                    this.mDecoders.decode(in, out, decoder);
                } catch (Throwable var34) {
                    var9 = var34;
                    throw var34;
                } finally {
                    if (out != null) {
                        if (var9 != null) {
                            try {
                                out.close();
                            } catch (Throwable var33) {
                                var9.addSuppressed(var33);
                            }
                        } else {
                            out.close();
                        }
                    }

                }
            } catch (Throwable var36) {
                var7 = var36;
            } finally {
                if (in != null) {
                    if (var7 != null) {
                        try {
                            in.close();
                        } catch (Throwable var32) {
                            var7.addSuppressed(var32);
                        }
                    } else {
                        in.close();
                    }
                }

            }

        } catch (IOException var38) {
            throw new AndrolibException(var38);
        } catch (DirectoryException var38){
            throw new AndrolibException(var38);
        }
    }

    public void copyRaw(Directory inDir, Directory outDir, String filename) throws AndrolibException {
        try {
            DirUtil.copyToDir(inDir, outDir, filename);
        } catch (DirectoryException var5) {
            throw new AndrolibException(var5);
        }
    }

    public void decodeManifest(Directory inDir, String inFileName, Directory outDir, String outFileName) throws AndrolibException {
        try {
            InputStream in = inDir.getFileInput(inFileName);
            Throwable var6 = null;

            try {
                OutputStream out = outDir.getFileOutput(outFileName);
                Throwable var8 = null;

                try {
                    ((ApkXmlPullStreamDecoder)this.mDecoders.getDecoder("xml")).decodeManifest(in, out);
                } catch (Throwable var33) {
                    var8 = var33;
                    throw var33;
                } finally {
                    if (out != null) {
                        if (var8 != null) {
                            try {
                                out.close();
                            } catch (Throwable var32) {
                                var8.addSuppressed(var32);
                            }
                        } else {
                            out.close();
                        }
                    }

                }
            } catch (Throwable var35) {
                var6 = var35;
            } finally {
                if (in != null) {
                    if (var6 != null) {
                        try {
                            in.close();
                        } catch (Throwable var31) {
                            var6.addSuppressed(var31);
                        }
                    } else {
                        in.close();
                    }
                }

            }

        } catch (IOException var37) {
            throw new AndrolibException(var37);
        } catch (DirectoryException var37){
            throw new AndrolibException(var37);
        }
    }
}
