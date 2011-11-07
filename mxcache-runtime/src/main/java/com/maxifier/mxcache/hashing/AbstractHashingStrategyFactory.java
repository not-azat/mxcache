package com.maxifier.mxcache.hashing;

import com.maxifier.mxcache.context.CacheContext;
import gnu.trove.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: dalex
 * Date: 01.07.2010
 * Time: 15:04:56
 * <p>
 * ���� ����� ���������� ���������, ���� �� ������ ������� ��������� ����������� ��������� ��� �������� ���������
 * �����������. ��� ����� ���������� ����� {@link AbstractHashingStrategyFactory#findStrategyClass(com.maxifier.mxcache.context.CacheContext, Class, java.lang.annotation.Annotation[])}.
 */
public abstract class AbstractHashingStrategyFactory implements HashingStrategyFactory {
    private static final Logger logger = LoggerFactory.getLogger(AbstractHashingStrategyFactory.class);

    private static final Map<Class, Class> STRATEGY_TYPE = new THashMap<Class, Class>();

    static {
        STRATEGY_TYPE.put(byte.class, TByteHashingStrategy.class);
        STRATEGY_TYPE.put(short.class, TShortHashingStrategy.class);
        STRATEGY_TYPE.put(int.class, TIntHashingStrategy.class);
        STRATEGY_TYPE.put(long.class, TLongHashingStrategy.class);
        STRATEGY_TYPE.put(float.class, TFloatHashingStrategy.class);
        STRATEGY_TYPE.put(double.class, TDoubleHashingStrategy.class);
    }

    @Override
    public Object createHashingStrategy(CacheContext context, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0) {
            return null;
        }
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        if (paramTypes.length > 1) {
            return createTupleHashingStrategy(context, paramTypes, paramAnnotations);
        } else {
            return createValueHashingStrategy(context, paramTypes[0], paramAnnotations[0]);
        }
    }

    private Object createValueHashingStrategy(CacheContext context, Class paramType, Annotation[] paramAnnotation) {
        Object strategy = findStrategyClass(context, paramType, paramAnnotation);
        if (strategy == null || !isSuitableStrategy(paramType, strategy)) {
            return null;
        }
        return  strategy;
    }

    private boolean isSuitableStrategy(Class paramType, Object strategy) {
        if (paramType.isPrimitive()) {
            Class strategyType = STRATEGY_TYPE.get(paramType);
            if (strategyType == null) {
                logger.error("Param of type " + paramType + " cannot have strategy");
                return false;
            }
            if (!strategyType.isInstance(strategy)) {
                logger.error("Param of type " + paramType + " cannot have strategy of type " + strategy);
                return false;
            }
        } else if (!TObjectHashingStrategy.class.isInstance(strategy)) {
            logger.error("Param of type " + paramType + " cannot have strategy of type " + strategy);
            return false;
        }
        return true;
    }

    private TObjectHashingStrategy createTupleHashingStrategy(CacheContext context, Class[] paramTypes, Annotation[][] paramAnnotations) {
        Object[] strategies = new Object[paramAnnotations.length];
        int n = 0;
        for (int i = 0; i < paramAnnotations.length; i++) {
            Object strategy = createValueHashingStrategy(context, paramTypes[i], paramAnnotations[i]);
            if (strategy != null) {
                n++;
            }
            strategies[i] = strategy;
        }
        return n > 0 ? new TupleHashingStrategy(strategies) : null;
    }

    /**
     * ���� ����� ������ ��������� ���������� ��������� �����������.
     * @param context context of instance
     * @param paramType ��� ���������, ������� ������ ������������
     * @param annotations ��������� ���������, ����� ������� ���������� ����� ���������
     * @return ��������� �����������, ��� null, ���� ��������� �� �������; �������������� ���������� null � �������
     * ��������� � ���, ���� ������� ������������ ��������� ��� ����� ���������.
     */
    protected abstract Object findStrategyClass(CacheContext context, Class paramType, Annotation[] annotations);
}