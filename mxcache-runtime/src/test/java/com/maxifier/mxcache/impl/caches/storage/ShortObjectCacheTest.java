package com.maxifier.mxcache.impl.caches.storage;

import com.maxifier.mxcache.caches.*;
import com.maxifier.mxcache.storage.ShortObjectStorage;
import com.maxifier.mxcache.storage.Storage;
import com.maxifier.mxcache.storage.elementlocked.ShortObjectElementLockedStorage;
import com.maxifier.mxcache.provider.Signature;
import com.maxifier.mxcache.resource.MxResource;
import com.maxifier.mxcache.impl.MutableStatisticsImpl;
import com.maxifier.mxcache.impl.resource.DependencyTracker;
import com.maxifier.mxcache.impl.resource.ResourceOccupied;
import com.maxifier.mxcache.interfaces.StatisticsHolder;
import org.testng.annotations.*;

import java.util.concurrent.locks.*;

import static org.mockito.Mockito.*;

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
@Test
public class ShortObjectCacheTest {
    private static final Signature SINGATURE = new Signature(short.class, Object.class);

    private static final ShortObjectCalculatable CALCULATABLE = new ShortObjectCalculatable() {
        @Override
        public Object calculate(Object owner, short o) {
            assert o == (short)42;
            return "123";
        }
    };

    @DataProvider(name = "both")
    public Object[][] v200v210v219() {
        return new Object[][] {{false}, {true}};
    }

    @Test(dataProvider = "both")
    public void testMiss(boolean elementLocked) {
        ShortObjectStorage storage = createStorage(elementLocked);

        when(storage.load((short)42)).thenReturn(Storage.UNDEFINED);
        when(storage.size()).thenReturn(0);

        ShortObjectCache cache = (ShortObjectCache) Wrapping.getFactory(SINGATURE, SINGATURE, elementLocked).
                    wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        assert cache.size() == 0;
        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate((short)42).equals("123");

        verify(storage).size();
        verify(storage, atLeast(1)).load((short)42);
        verify(storage).save((short)42, "123");
        if (elementLocked) {
            
                ((ShortObjectElementLockedStorage)verify(storage, atLeast(1))).lock((short)42);
                ((ShortObjectElementLockedStorage)verify(storage, atLeast(1))).unlock((short)42);
            
        }
        verifyNoMoreInteractions(storage);

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 1;
    }

    @Test(dataProvider = "both")
    public void testHit(boolean elementLocked) {
        ShortObjectStorage storage = createStorage(elementLocked);

        ShortObjectCache cache = (ShortObjectCache) Wrapping.getFactory(SINGATURE, SINGATURE, elementLocked).
                    wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        when(storage.load((short)42)).thenReturn("123");
        when(storage.size()).thenReturn(1);

        assert cache.size() == 1;
        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate((short)42).equals("123");

        verify(storage).size();
        verify(storage, atLeast(1)).load((short)42);
        if (elementLocked) {
            
                ((ShortObjectElementLockedStorage)verify(storage, atLeast(1))).lock((short)42);
                ((ShortObjectElementLockedStorage)verify(storage, atLeast(1))).unlock((short)42);
            
        }
        verifyNoMoreInteractions(storage);

        assert cache.getStatistics().getHits() == 1;
        assert cache.getStatistics().getMisses() == 0;
    }

    @Test(dataProvider = "both")
    public void testClear(boolean elementLocked) {
        ShortObjectStorage storage = createStorage(elementLocked);

        ShortObjectCache cache = (ShortObjectCache) Wrapping.getFactory(SINGATURE, SINGATURE, elementLocked).
                    wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        cache.clear();

        verify(storage).clear();
        verifyNoMoreInteractions(storage);
    }

    @Test(dataProvider = "both")
    public void testSetDuringDependencyNodeOperations(boolean elementLocked) {
        ShortObjectStorage storage = createStorage(elementLocked);

        when(storage.load((short)42)).thenReturn(Storage.UNDEFINED, "123");

        ShortObjectCalculatable calculatable = mock(ShortObjectCalculatable.class);
        MxResource r = mock(MxResource.class);
        when(calculatable.calculate("123", (short)42)).thenThrow(new ResourceOccupied(r));

        ShortObjectCache cache = (ShortObjectCache) Wrapping.getFactory(SINGATURE, SINGATURE, elementLocked).
                    wrap("123", calculatable, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate((short)42).equals("123");

        assert cache.getStatistics().getHits() == 1;
        assert cache.getStatistics().getMisses() == 0;

        verify(storage, times(2)).load((short)42);
        if (elementLocked) {
            
                ((ShortObjectElementLockedStorage)verify(storage, atLeast(1))).lock((short)42);
                ((ShortObjectElementLockedStorage)verify(storage, atLeast(1))).unlock((short)42);
            
        }
        verifyNoMoreInteractions(storage);
        verify(calculatable).calculate("123", (short)42);
        verifyNoMoreInteractions(calculatable);
    }

    @Test(dataProvider = "both")
    public void testResetStat(boolean elementLocked) {
        ShortObjectStorage storage = createStorage(elementLocked);

        when(storage.load((short)42)).thenReturn(Storage.UNDEFINED);

        ShortObjectCache cache = (ShortObjectCache) Wrapping.getFactory(SINGATURE, SINGATURE, elementLocked).
                    wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate((short)42).equals("123");

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 1;

        cache.getStatistics().reset();

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        verify(storage, atLeast(1)).load((short)42);
        verify(storage).save((short)42, "123");
        if (elementLocked) {
            
                ((ShortObjectElementLockedStorage)verify(storage, atLeast(1))).lock((short)42);
                ((ShortObjectElementLockedStorage)verify(storage, atLeast(1))).unlock((short)42);
               
        }
        verifyNoMoreInteractions(storage);
    }

    private ShortObjectStorage createStorage(boolean elementLocked) {
        ShortObjectStorage storage = mock(elementLocked ? ShortObjectElementLockedStorage.class : ShortObjectStorage.class);
        if (elementLocked) {
            when(((ShortObjectElementLockedStorage)storage).getLock()).thenReturn(new ReentrantLock());
        }
        return storage;
    }

    @Test(dataProvider = "both")
    public void testTransparentStat(boolean elementLocked) {
        ShortObjectStorage storage = mock(elementLocked ? ShortObjectElementLockedStorage.class : ShortObjectStorage.class, withSettings().extraInterfaces(StatisticsHolder.class));

        ShortObjectCache cache = (ShortObjectCache) Wrapping.getFactory(SINGATURE, SINGATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        cache.getStatistics();

        verify((StatisticsHolder)storage).getStatistics();
        verifyNoMoreInteractions(storage);
    }
}