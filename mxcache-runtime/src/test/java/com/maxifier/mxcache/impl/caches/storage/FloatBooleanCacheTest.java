package com.maxifier.mxcache.impl.caches.storage;

import com.maxifier.mxcache.caches.*;
import com.maxifier.mxcache.storage.FloatBooleanStorage;
import com.maxifier.mxcache.storage.elementlocked.FloatBooleanElementLockedStorage;
import com.maxifier.mxcache.provider.Signature;
import com.maxifier.mxcache.resource.MxResource;
import com.maxifier.mxcache.impl.MutableStatisticsImpl;
import com.maxifier.mxcache.impl.resource.DependencyTracker;
import com.maxifier.mxcache.impl.resource.ResourceOccupied;
import com.maxifier.mxcache.interfaces.StatisticsHolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.*;

import java.util.concurrent.locks.*;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

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
@SuppressWarnings({ "unchecked" })
@Test
public class FloatBooleanCacheTest {
    private static final Signature SIGNATURE = new Signature(float.class, boolean.class);

    private static final FloatBooleanCalculatable CALCULATABLE = new FloatBooleanCalculatable() {
        @Override
        public boolean calculate(Object owner, float o) {
            assert o == 42f;
            return true;
        }
    };

    @DataProvider(name = "both")
    public Object[][] v200v210v219() {
        return new Object[][] {{false}, {true}};
    }

    private static class Occupied implements FloatBooleanCalculatable {
        private boolean occupied;

        private int occupiedRequests;

        public Occupied() {

        }

        public synchronized void setOccupied(boolean occupied) {
            this.occupied = occupied;
            notifyAll();
        }

        @Override
        public synchronized boolean calculate(Object owner, float o) {
            if (occupied) {
                occupiedRequests++;
                notifyAll();

                MxResource r = mock(MxResource.class);
                doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        synchronized(Occupied.this) {
                            while(occupied) {
                                Occupied.this.wait();
                            }
                        }
                        return null;
                    }
                }).when(r).waitForEndOfModification();
                throw new ResourceOccupied(r);
            }
            return true;
        }
    }

    @Test(dataProvider = "both")
    public void testOccupied(boolean elementLocked) throws Throwable {
        FloatBooleanStorage storage = createStorage(elementLocked);
        Occupied occupied = new Occupied();

        when(storage.isCalculated(42f)).thenReturn(false);
        when(storage.size()).thenReturn(0);

        final FloatBooleanCache cache = (FloatBooleanCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", occupied, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        occupied.setOccupied(true);

        class TestThread extends Thread {
            public Throwable t;

            @Override
            public void run() {
                try {
                    assert cache.size() == 0;
                    assert cache.getStatistics().getHits() == 0;
                    assert cache.getStatistics().getMisses() == 0;

                    assert cache.getOrCreate(42f) == true;

                    assertEquals(cache.getStatistics().getHits(), 0);
                    assertEquals(cache.getStatistics().getMisses(), 1);
                } catch (Throwable t) {
                    this.t = t;
                }
            }
        }
        TestThread t = new TestThread();
        t.start();

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized(occupied) {
            while(occupied.occupiedRequests == 0) {
                occupied.wait();
            }
        }

        occupied.setOccupied(false);

        t.join();

        if (t.t != null) {
            throw t.t;
        }

        verify(storage).size();
        verify(storage, atLeast(1)).isCalculated(42f);
        verify(storage).save(42f, true);
        if (elementLocked) {
            
                ((FloatBooleanElementLockedStorage)verify(storage, atLeast(1))).lock(42f);
                ((FloatBooleanElementLockedStorage)verify(storage, atLeast(1))).unlock(42f);
            
        }
        verifyNoMoreInteractions(storage);
    }

    @Test(dataProvider = "both")
    public void testMiss(boolean elementLocked) {
        FloatBooleanStorage storage = createStorage(elementLocked);

        when(storage.isCalculated(42f)).thenReturn(false);
        when(storage.size()).thenReturn(0);

        FloatBooleanCache cache = (FloatBooleanCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        assert cache.size() == 0;
        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate(42f) == true;

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 1;

        verify(storage).size();
        verify(storage, atLeast(1)).isCalculated(42f);
        verify(storage).save(42f, true);
        if (elementLocked) {
            
                ((FloatBooleanElementLockedStorage)verify(storage, atLeast(1))).lock(42f);
                ((FloatBooleanElementLockedStorage)verify(storage, atLeast(1))).unlock(42f);
            
        }
        verifyNoMoreInteractions(storage);
    }

    @Test(dataProvider = "both")
    public void testHit(boolean elementLocked) {
        FloatBooleanStorage storage = createStorage(elementLocked);

        FloatBooleanCache cache = (FloatBooleanCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        when(storage.isCalculated(42f)).thenReturn(true);
        when(storage.load(42f)).thenReturn(true);
        when(storage.size()).thenReturn(1);

        assert cache.size() == 1;
        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate(42f) == true;

        assert cache.getStatistics().getHits() == 1;
        assert cache.getStatistics().getMisses() == 0;

        verify(storage).size();
        verify(storage, atLeast(1)).isCalculated(42f);
        verify(storage).load(42f);
        if (elementLocked) {
            
                ((FloatBooleanElementLockedStorage)verify(storage, atLeast(1))).lock(42f);
                ((FloatBooleanElementLockedStorage)verify(storage, atLeast(1))).unlock(42f);
            
        }
        verifyNoMoreInteractions(storage);
    }

    @Test(dataProvider = "both")
    public void testClear(boolean elementLocked) {
        FloatBooleanStorage storage = createStorage(elementLocked);

        FloatBooleanCache cache = (FloatBooleanCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        cache.clear();

        verify(storage).clear();
        verifyNoMoreInteractions(storage);
    }

    @Test(dataProvider = "both")
    public void testSetDuringDependencyNodeOperations(boolean elementLocked) {
        FloatBooleanStorage storage = createStorage(elementLocked);

        when(storage.isCalculated(42f)).thenReturn(false, true);
        when(storage.load(42f)).thenReturn(true);

        FloatBooleanCalculatable calculatable = mock(FloatBooleanCalculatable.class);
        MxResource r = mock(MxResource.class);
        when(calculatable.calculate("123", 42f)).thenThrow(new ResourceOccupied(r));

        FloatBooleanCache cache = (FloatBooleanCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", calculatable, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate(42f) == true;

        assert cache.getStatistics().getHits() == 1;
        assert cache.getStatistics().getMisses() == 0;

        verify(storage, times(2)).isCalculated(42f);
        verify(storage).load(42f);
        if (elementLocked) {
            
                ((FloatBooleanElementLockedStorage)verify(storage, atLeast(1))).lock(42f);
                ((FloatBooleanElementLockedStorage)verify(storage, atLeast(1))).unlock(42f);
            
        }
        verifyNoMoreInteractions(storage);
        verify(calculatable).calculate("123", 42f);
        verifyNoMoreInteractions(calculatable);
    }

    @Test(dataProvider = "both")
    public void testResetStat(boolean elementLocked) {
        FloatBooleanStorage storage = createStorage(elementLocked);

        when(storage.isCalculated(42f)).thenReturn(false);

        FloatBooleanCache cache = (FloatBooleanCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate(42f) == true;

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 1;

        cache.getStatistics().reset();

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        verify(storage, atLeast(1)).isCalculated(42f);
        verify(storage).save(42f, true);
        if (elementLocked) {
            
                ((FloatBooleanElementLockedStorage)verify(storage, atLeast(1))).lock(42f);
                ((FloatBooleanElementLockedStorage)verify(storage, atLeast(1))).unlock(42f);
            
        }
        verifyNoMoreInteractions(storage);
    }

    private FloatBooleanStorage createStorage(boolean elementLocked) {
        FloatBooleanStorage storage = mock(elementLocked ? FloatBooleanElementLockedStorage.class : FloatBooleanStorage.class);
        if (elementLocked) {
            when(((FloatBooleanElementLockedStorage)storage).getLock()).thenReturn(new ReentrantLock());
        }
        return storage;
    }    

    @Test(dataProvider = "both")
    public void testTransparentStat(boolean elementLocked) {
        FloatBooleanStorage storage = mock(elementLocked ? FloatBooleanElementLockedStorage.class : FloatBooleanStorage.class, withSettings().extraInterfaces(StatisticsHolder.class));

        FloatBooleanCache cache = (FloatBooleanCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        cache.getStatistics();

        verify((StatisticsHolder)storage).getStatistics();
        verifyNoMoreInteractions(storage);
    }
}