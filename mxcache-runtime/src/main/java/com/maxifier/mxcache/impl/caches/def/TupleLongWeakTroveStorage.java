package com.maxifier.mxcache.impl.caches.def;

import com.maxifier.mxcache.tuple.Tuple;
import com.maxifier.mxcache.transform.SmartReference;
import gnu.trove.*;

import java.util.*;

/**
 * Project: Maxifier
 * Created by: Yakoushin Andrey
 * Date: 15.02.2010
 * Time: 13:40:10
 * <p/>
 * Copyright (c) 1999-2009 Magenta Corporation Ltd. All Rights Reserved.
 * Magenta Technology proprietary and confidential.
 * Use is subject to license terms.
 *
 * @author ELectronic ENgine
 */

public class TupleLongWeakTroveStorage extends ObjectLongTroveStorage<Tuple> {
    private final List<Tuple> removed = Collections.synchronizedList(new ArrayList<Tuple>());

    private final int[] tupleIndices;

    @Deprecated
    public TupleLongWeakTroveStorage() {
        throw new UnsupportedOperationException("TupleLongWeakTroveStorage actually is not externalizable");
    }

    public TupleLongWeakTroveStorage(int[] tupleIndices) {
        this.tupleIndices = tupleIndices;
    }

    public TupleLongWeakTroveStorage(TObjectHashingStrategy<Tuple> strategy, int[] tupleIndices) {
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
    public long load(Tuple o) {
        cleanup();
        return super.load(o);
    }

    @Override
    public void save(Tuple o, long t) {
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