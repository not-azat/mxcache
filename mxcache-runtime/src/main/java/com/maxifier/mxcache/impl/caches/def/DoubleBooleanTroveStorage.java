package com.maxifier.mxcache.impl.caches.def;

import gnu.trove.*;

import com.maxifier.mxcache.storage.*;

/**
 * Project: Maxifier
 * Created by: Yakoushin Andrey
 * Date: 15.02.2010
 * Time: 13:54:51
 * <p/>
 * Copyright (c) 1999-2009 Magenta Corporation Ltd. All Rights Reserved.
 * Magenta Technology proprietary and confidential.
 * Use is subject to license terms.
 *
 * @author ELectronic ENgine
 */

public class DoubleBooleanTroveStorage extends TDoubleByteHashMap implements DoubleBooleanStorage {
    public DoubleBooleanTroveStorage() {
    }

    public DoubleBooleanTroveStorage(TDoubleHashingStrategy strategy) {
        super(strategy);
    }

    @Override
    public boolean isCalculated(double o) {
        return super.contains(o);
    }

    @Override
    public boolean load(double o) {
        return super.get(o) != 0;
    }

    @Override
    public void save(double o, boolean t) {
        put(o, (byte)(t? 1 : 0));
    }
}