package com.maxifier.mxcache.tuple;

import gnu.trove.TIntHashingStrategy;
import gnu.trove.TObjectHashingStrategy;
import gnu.trove.TObjectIdentityHashingStrategy;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: dalex
 * Date: 17.02.2010
 * Time: 9:40:52
 */
@Test
public class TupleGeneratorUTest {
    private static Class[] array(Class... r) {
        return r;
    }

    @Test
    public void testPrimitives() throws Exception {
        check(array(boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, float.class, double.class, double.class),
              true, (byte) 3, 'a', (short) 11, 441, 71L, 31f, Float.NaN, 44d, Double.NaN);
    }

    public void testObject() throws Exception {
        check(array(String.class, Comparable.class, Object.class), "Test", 4, null);
    }

    public void testMixed() throws Exception {
        check(array(String.class, int.class), "Test", 3);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidParam() throws Exception {
        createTuple(array(String.class), "Test").get(2);
    }

    public void testCustomStrategy() throws Exception {
        TObjectIdentityHashingStrategy<String> strategy = new TObjectIdentityHashingStrategy<String>();
        Tuple tuple = createTuple(array(String.class, int.class), "Test", 3);
        // � int hashCode ����� ��� ������
        int sample = Arrays.hashCode(new int[] { System.identityHashCode("Test"), 3});
        assert tuple.hashCode(strategy, null) == sample;

        Tuple tuple2 = createTuple(array(String.class, int.class), "tEST", 3);
        assert tuple.equals(tuple2, new TObjectHashingStrategy<String>() {
            @Override
            public int computeHashCode(String object) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean equals(String o1, String o2) {
                return o1.equalsIgnoreCase(o2);
            }
        }, null);
    }

    public void testCustomStrategyForPrivitive() throws Exception {
        TIntHashingStrategy constIntHashing = new TIntHashingStrategy() {
            @Override
            public int computeHashCode(int val) {
                return -1;
            }
        };

        Tuple tuple = createTuple(array(int.class, boolean.class), 3, true);
        // � int hashCode ����� ��� ������
        int sample = Arrays.hashCode(new int[] { -1, Boolean.TRUE.hashCode()});
        assert tuple.hashCode(constIntHashing, null) == sample;

        Tuple tuple2 = createTuple(array(int.class, boolean.class), 3, true);
        assert tuple.equals(tuple2, constIntHashing, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIncompatibleStrategy() throws Exception {
        Tuple tuple = createTuple(array(int.class), 3);
        Tuple tuple2 = createTuple(array(int.class), 4);
        assert tuple.equals(tuple2, new TObjectIdentityHashingStrategy());
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void testIncompatibleStrategyForBoolean() throws Exception {
        Tuple tuple = createTuple(array(boolean.class), true);
        Tuple tuple2 = createTuple(array(boolean.class), false);
        assert tuple.equals(tuple2, new TObjectIdentityHashingStrategy());
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void testInvalidTypeInt() throws Exception {
        createTuple(array(String.class), "Test").getInt(0);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void testInvalidTypeLong() throws Exception {
        createTuple(array(String.class), "Test").getLong(0);
    }

    public void testEquals() throws Exception {
        Class[] t = array(boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, float.class, double.class, double.class, String.class, String.class);
        Object[] v1 = { true, (byte) 3, 'a', (short) 11, 441, 71L, 31f, Float.NaN, 44d, Double.NaN, "123", null};
        Object[] v2 = { false, (byte) 4, '7', (short) 12, 421, 33L, 4.5f, 0f, 0d, 3.1d, "321", "124365"};

        Tuple t0 = createTuple(t, v1);
        Tuple t0c = createTuple(t, v1);

        assertTrue(t0.equals(t0));
        assertTrue(t0.equals(t0c));
        assertTrue(t0c.equals(t0));
        assertTrue(t0c.equals(t0c));

        int n = t.length;
        int N = 1 << n;
        Object[] s = new Object[n];
        for (int i = 1; i < N; i++) {
            for (int b = 0, m = i; b < n; b++, m >>= 1) {
                s[b] = (m & 1) == 0 ? v1[b] : v2[b];
            }
            Tuple t1 = createTuple(t, s);
            assertFalse(t0.equals(t1));
            assertFalse(t1.equals(t0));
            assertTrue(t1.equals(t1));
        }
    }

    private static Tuple check(Class[] types, Object... values) throws Exception {
        assert types.length == values.length;
        Tuple tuple = createTuple(types, values);
        assertEquals(tuple.size(), values.length);
        for (int i = 0; i < values.length; i++) {
            assertEquals(tuple.get(i), values[i]);
            Class type = types[i];
            if (type == boolean.class) {
                assertEquals(tuple.getBoolean(i), values[i]);
            } else if (type == byte.class) {
                assertEquals(tuple.getByte(i), values[i]);
            } else if (type == char.class) {
                assertEquals(tuple.getChar(i), values[i]);
            } else if (type == short.class) {
                assertEquals(tuple.getShort(i), values[i]);
            } else if (type == int.class) {
                assertEquals(tuple.getInt(i), values[i]);
            } else if (type == long.class) {
                assertEquals(tuple.getLong(i), values[i]);
            } else if (type == float.class) {
                assertEquals(tuple.getFloat(i), values[i]);
            } else if (type == double.class) {
                assertEquals(tuple.getDouble(i), values[i]);
            }
        }
        assertTrue(Arrays.equals(tuple.toArray(), values));
        assertEquals(tuple.hashCode(), Arrays.hashCode(values));
        assertEquals(tuple, tuple);
        assertNotNull(tuple.toString());
        return tuple;
    }

    private static Tuple createTuple(Class[] types, Object... values) throws Exception {
        TupleFactory factory = TupleGenerator.getTupleFactory(types);
        Class<? extends Tuple> type = factory.getTupleClass();
        assertEquals(type.getClassLoader(), ClassLoader.getSystemClassLoader());
        assertEquals(Class.forName(type.getName()), type);
        return factory.create(values);
    }
}