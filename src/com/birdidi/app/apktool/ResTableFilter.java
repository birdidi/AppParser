package com.birdidi.app.apktool;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResTable;

public interface ResTableFilter {

    void filter(ResTable resTable) throws AndrolibException;
}
