package com.maxifier.mxcache.impl.caches.storage;

import com.maxifier.mxcache.caches.*;
import com.maxifier.mxcache.storage.CharacterShortStorage;
import com.maxifier.mxcache.storage.elementlocked.CharacterShortElementLockedStorage;
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
public class CharacterShortCacheTest {
    private static final Signature SIGNATURE = new Signature(char.class, short.class);

    private static final CharacterShortCalculatable CALCULATABLE = new CharacterShortCalculatable() {
        @Override
        public short calculate(Object owner, char o) {
            assert o == '*';
            return (short)42;
        }
    };

    @DataProvider(name = "both")
    public Object[][] v200v210v219() {
        return new Object[][] {{false}, {true}};
    }

    private static class Occupied implements CharacterShortCalculatable {
        private boolean occupied;

        private int occupiedRequests;

        public Occupied() {

        }

        public synchronized void setOccupied(boolean occupied) {
            this.occupied = occupied;
            notifyAll();
        }

        @Override
        public synchronized short calculate(Object owner, char o) {
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
            return (short)42;
        }
    }

    @Test(dataProvider = "both")
    public void testOccupied(boolean elementLocked) throws Throwable {
        CharacterShortStorage storage = createStorage(elementLocked);
        Occupied occupied = new Occupied();

        when(storage.isCalculated('*')).thenReturn(false);
        when(storage.size()).thenReturn(0);

        final CharacterShortCache cache = (CharacterShortCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
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

                    assert cache.getOrCreate('*') == (short)42;

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
        verify(storage, atLeast(1)).isCalculated('*');
        verify(storage).save('*', (short)42);
        if (elementLocked) {
            
                ((CharacterShortElementLockedStorage)verify(storage, atLeast(1))).lock('*');
                ((CharacterShortElementLockedStorage)verify(storage, atLeast(1))).unlock('*');
            
        }
        verifyNoMoreInteractions(storage);
    }

    @Test(dataProvider = "both")
    public void testMiss(boolean elementLocked) {
        CharacterShortStorage storage = createStorage(elementLocked);

        when(storage.isCalculated('*')).thenReturn(false);
        when(storage.size()).thenReturn(0);

        CharacterShortCache cache = (CharacterShortCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        assert cache.size() == 0;
        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate('*') == (short)42;

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 1;

        verify(storage).size();
        verify(storage, atLeast(1)).isCalculated('*');
        verify(storage).save('*', (short)42);
        if (elementLocked) {
            
                ((CharacterShortElementLockedStorage)verify(storage, atLeast(1))).lock('*');
                ((CharacterShortElementLockedStorage)verify(storage, atLeast(1))).unlock('*');
            
        }
        verifyNoMoreInteractions(storage);
    }

    @Test(dataProvider = "both")
    public void testHit(boolean elementLocked) {
        CharacterShortStorage storage = createStorage(elementLocked);

        CharacterShortCache cache = (CharacterShortCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        when(storage.isCalculated('*')).thenReturn(true);
        when(storage.load('*')).thenReturn((short)42);
        when(storage.size()).thenReturn(1);

        assert cache.size() == 1;
        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate('*') == (short)42;

        assert cache.getStatistics().getHits() == 1;
        assert cache.getStatistics().getMisses() == 0;

        verify(storage).size();
        verify(storage, atLeast(1)).isCalculated('*');
        verify(storage).load('*');
        if (elementLocked) {
            
                ((CharacterShortElementLockedStorage)verify(storage, atLeast(1))).lock('*');
                ((CharacterShortElementLockedStorage)verify(storage, atLeast(1))).unlock('*');
            
        }
        verifyNoMoreInteractions(storage);
    }

    @Test(dataProvider = "both")
    public void testClear(boolean elementLocked) {
        CharacterShortStorage storage = createStorage(elementLocked);

        CharacterShortCache cache = (CharacterShortCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        cache.clear();

        verify(storage).clear();
        verifyNoMoreInteractions(storage);
    }

    @Test(dataProvider = "both")
    public void testSetDuringDependencyNodeOperations(boolean elementLocked) {
        CharacterShortStorage storage = createStorage(elementLocked);

        when(storage.isCalculated('*')).thenReturn(false, true);
        when(storage.load('*')).thenReturn((short)42);

        CharacterShortCalculatable calculatable = mock(CharacterShortCalculatable.class);
        MxResource r = mock(MxResource.class);
        when(calculatable.calculate("123", '*')).thenThrow(new ResourceOccupied(r));

        CharacterShortCache cache = (CharacterShortCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", calculatable, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate('*') == (short)42;

        assert cache.getStatistics().getHits() == 1;
        assert cache.getStatistics().getMisses() == 0;

        verify(storage, times(2)).isCalculated('*');
        verify(storage).load('*');
        if (elementLocked) {
            
                ((CharacterShortElementLockedStorage)verify(storage, atLeast(1))).lock('*');
                ((CharacterShortElementLockedStorage)verify(storage, atLeast(1))).unlock('*');
            
        }
        verifyNoMoreInteractions(storage);
        verify(calculatable).calculate("123", '*');
        verifyNoMoreInteractions(calculatable);
    }

    @Test(dataProvider = "both")
    public void testResetStat(boolean elementLocked) {
        CharacterShortStorage storage = createStorage(elementLocked);

        when(storage.isCalculated('*')).thenReturn(false);

        CharacterShortCache cache = (CharacterShortCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        assert cache.getOrCreate('*') == (short)42;

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 1;

        cache.getStatistics().reset();

        assert cache.getStatistics().getHits() == 0;
        assert cache.getStatistics().getMisses() == 0;

        verify(storage, atLeast(1)).isCalculated('*');
        verify(storage).save('*', (short)42);
        if (elementLocked) {
            
                ((CharacterShortElementLockedStorage)verify(storage, atLeast(1))).lock('*');
                ((CharacterShortElementLockedStorage)verify(storage, atLeast(1))).unlock('*');
            
        }
        verifyNoMoreInteractions(storage);
    }

    private CharacterShortStorage createStorage(boolean elementLocked) {
        CharacterShortStorage storage = mock(elementLocked ? CharacterShortElementLockedStorage.class : CharacterShortStorage.class);
        if (elementLocked) {
            when(((CharacterShortElementLockedStorage)storage).getLock()).thenReturn(new ReentrantLock());
        }
        return storage;
    }    

    @Test(dataProvider = "both")
    public void testTransparentStat(boolean elementLocked) {
        CharacterShortStorage storage = mock(elementLocked ? CharacterShortElementLockedStorage.class : CharacterShortStorage.class, withSettings().extraInterfaces(StatisticsHolder.class));

        CharacterShortCache cache = (CharacterShortCache) Wrapping.getFactory(SIGNATURE, SIGNATURE, elementLocked).
                wrap("123", CALCULATABLE, DependencyTracker.DUMMY_NODE, storage, new MutableStatisticsImpl());

        cache.getStatistics();

        verify((StatisticsHolder)storage).getStatistics();
        verifyNoMoreInteractions(storage);
    }
}