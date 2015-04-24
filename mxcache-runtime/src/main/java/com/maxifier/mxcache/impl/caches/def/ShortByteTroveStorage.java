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
public class ShortByteTroveStorage extends gnu.trove.map.hash.TShortByteHashMap implements ShortByteStorage {
    @Override
    public boolean isCalculated(short o) {
        return super.contains(o);
    }

    @Override
    public byte load(short o) {
        return super.get(o);
    }

    @Override
    public void save(short o, byte t) {
        put(o, t);
    }
}