package com.maxifier.mxcache.impl.caches.abs;

import com.maxifier.mxcache.CacheFactory;
import com.maxifier.mxcache.caches.*;
import com.maxifier.mxcache.impl.MutableStatistics;
import com.maxifier.mxcache.impl.CacheId;
import com.maxifier.mxcache.impl.CalculatableHelper;
import com.maxifier.mxcache.impl.resource.*;
import com.maxifier.mxcache.provider.CacheDescriptor;
import com.maxifier.mxcache.storage.*;

/**
 * Project: Maxifier
 * Created by: Yakoushin Andrey
 * Date: 15.02.2010
 * Time: 13:29:47
 * <p/>
 * Copyright (c) 1999-2009 Magenta Corporation Ltd. All Rights Reserved.
 * Magenta Technology proprietary and confidential.
 * Use is subject to license terms.
 *
 * @author ELectronic ENgine
 */
public abstract class AbstractByteObjectCache<F> extends AbstractCache implements ByteObjectCache<F>, ByteObjectStorage<F> {
    private final ByteObjectCalculatable<F> calculatable;

    private final Object owner;

    public AbstractByteObjectCache(Object owner, ByteObjectCalculatable<F> calculatable, MutableStatistics statistics) {
        super(statistics);
        this.owner = owner;
        this.calculatable = calculatable;
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public F getOrCreate(byte o) {
        lock();
        try {
            Object v = load(o);
            if (v != UNDEFINED) {
                DependencyTracker.mark(getDependencyNode());
                hit();
                return (F)v;
            }
            DependencyNode callerNode = DependencyTracker.track(getDependencyNode());
            try {
                while(true) {
                    try {
                        return create(o);
                    } catch (ResourceOccupied e) {
                        if (callerNode != null) {
                            throw e;
                        } else {
                            unlock();
                            try {
                                e.getResource().waitForEndOfModification();
                            } finally {
                                lock();
                            }
                            v = load(o);
                            if (v != UNDEFINED) {
                                hit();
                                return (F)v;
                            }
                        }
                    }
                }
            } finally {
                DependencyTracker.exit(callerNode);
            }
        } finally {
            unlock();
        }
    }

    @SuppressWarnings({ "unchecked" })
    private F create(byte o) {
        long start = System.nanoTime();
        F t = calculatable.calculate(owner, o);
        long end = System.nanoTime();
        miss(end - start);
        save(o, t);
        return t;
    }

    @Override
    public CacheDescriptor getDescriptor() {
        CacheId id = CalculatableHelper.getId(calculatable.getClass());
        return CacheFactory.getProvider().getDescriptor(id);
    }

    @Override
    public String toString() {
        return getDescriptor() + ": " + owner;
    }
}
