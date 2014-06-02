/*
 * Copyright (c) 2008-2014 Maxifier Ltd. All Rights Reserved.
 */
package com.maxifier.mxcache.impl.caches.def;

import com.maxifier.mxcache.storage.*;

/**
 * DoubleLongTroveStorage
 *
 * THIS IS GENERATED CLASS! DON'T EDIT IT MANUALLY!
 *
 * GENERATED FROM P2PTroveStorage.template
 *
 * @author Andrey Yakoushin (andrey.yakoushin@maxifier.com)
 * @author Alexander Kochurov (alexander.kochurov@maxifier.com)
 */
public class DoubleLongTroveStorage extends gnu.trove.map.hash.TDoubleLongHashMap implements DoubleLongStorage {
    @Override
    public boolean isCalculated(double o) {
        return super.contains(o);
    }

    @Override
    public long load(double o) {
        return super.get(o);
    }

    @Override
    public void save(double o, long t) {
        put(o, t);
    }
}