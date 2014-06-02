/*
 * Copyright (c) 2008-2014 Maxifier Ltd. All Rights Reserved.
 */
package com.maxifier.mxcache.impl.caches.def;

import com.maxifier.mxcache.tuple.Tuple;
import com.maxifier.mxcache.transform.SmartReference;

import java.util.*;

/**
 * TupleFloatWeakTroveStorage
 *
 * THIS IS GENERATED CLASS! DON'T EDIT IT MANUALLY!
 *
 * GENERATED FROM #SOURCE#
 *
 * @author Andrey Yakoushin (andrey.yakoushin@maxifier.com)
 * @author Alexander Kochurov (alexander.kochurov@maxifier.com)
 */
public class TupleFloatWeakTroveStorage extends ObjectFloatTroveStorage<Tuple> {
    private final List<Tuple> removed = Collections.synchronizedList(new ArrayList<Tuple>());

    private final int[] tupleIndices;

    public TupleFloatWeakTroveStorage() {
        throw new UnsupportedOperationException("TupleFloatWeakTroveStorage actually is not externalizable");
    }

    public TupleFloatWeakTroveStorage(int[] tupleIndices) {
        this.tupleIndices = tupleIndices;
    }

    public TupleFloatWeakTroveStorage(gnu.trove.strategy.HashingStrategy<Tuple> strategy, int[] tupleIndices) {
        super(strategy);
        this.tupleIndices = tupleIndices;
    }

    private void cleanup() {
        for(Tuple t: removed) {
            remove(t);
        }
        removed.clear();
    }

    @Override
    public float load(Tuple o) {
        cleanup();
        return super.load(o);
    }

    @Override
    public void save(Tuple o, float t) {
        cleanup();
        Callback callback = new Callback(o);
        for (int index: tupleIndices) {
            ((SmartReference)o.get(index)).setCallback(callback);
        }
        super.save(o, t);
    }

    private class Callback implements Runnable {
        private final Tuple o;

        public Callback(Tuple o) {
            this.o = o;
        }

        @Override
        public void run() {
            removed.add(o);
        }
    }
}
