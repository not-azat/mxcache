/*
 * Copyright (c) 2008-2014 Maxifier Ltd. All Rights Reserved.
 */
package com.maxifier.mxcache.impl.caches.def;

import com.maxifier.mxcache.storage.*;

/**
 * THIS IS GENERATED CLASS! DON'T EDIT IT MANUALLY!
 *
 * GENERATED FROM P2PTroveStorage.template
 *
 * @author Andrey Yakoushin (andrey.yakoushin@maxifier.com)
 * @author Alexander Kochurov (alexander.kochurov@maxifier.com)
 */
public class IntCharacterTroveStorage extends gnu.trove.map.hash.TIntShortHashMap implements IntCharacterStorage {
    @Override
    public boolean isCalculated(int o) {
        return super.contains(o);
    }

    @Override
    public char load(int o) {
        return (char)super.get(o);
    }

    @Override
    public void save(int o, char t) {
        put(o, (short)t);
    }
}