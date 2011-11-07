package com.maxifier.mxcache.instrumentation.current;

import com.maxifier.mxcache.MxCache;
import com.maxifier.mxcache.asm.*;
import com.maxifier.mxcache.asm.commons.EmptyVisitor;
import com.maxifier.mxcache.instrumentation.*;
import com.maxifier.mxcache.util.SmartClassWriter;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: dalex
 * Date: 17.03.2010
 * Time: 9:14:33
 */
public abstract class InstrumentatorImpl implements com.maxifier.mxcache.instrumentation.Instrumentator {
    static final String CACHED_DESCRIPTOR = RuntimeTypes.CACHED_TYPE.getDescriptor();
    static final String RESOURCE_READER_DESCRIPTOR = RuntimeTypes.RESOURCE_READER_TYPE.getDescriptor();
    static final String RESOURCE_WRITER_DESCRIPTOR = RuntimeTypes.RESOURCE_WRITER_TYPE.getDescriptor();
    static final String USE_PROXY_DESCRIPTOR = RuntimeTypes.USE_PROXY_TYPE.getDescriptor();
    
    private static final BytecodeMatcher CACHED_DESCRIPTOR_MATCHER = new BytecodeMatcher(CACHED_DESCRIPTOR);
    private static final BytecodeMatcher RESOURCE_READER_DESCRIPTOR_MATCHER = new BytecodeMatcher(RESOURCE_READER_DESCRIPTOR);
    private static final BytecodeMatcher RESOURCE_WRITER_DESCRIPTOR_MATCHER = new BytecodeMatcher(RESOURCE_WRITER_DESCRIPTOR);
    private static final BytecodeMatcher USE_PROXY_DESCRIPTOR_MATCHER = new BytecodeMatcher(USE_PROXY_DESCRIPTOR);

    public static final InstrumentatorImpl INSTANCE_219 = new InstrumentatorImpl(false, "Instrumentator<2.1.9>") {
        @Override
        protected CachedInstrumentationStage createCachedStage(ClassVisitor visitor, ClassVisitor detector) {
            return new CachedInstrumentationStage219(this, visitor, detector);
        }

        @Override
        protected UseProxyInstrumentationStage createProxyStage(ClassVisitor visitor, ClassVisitor detector) {
            return new UseProxyInstrumentationStage219(this, visitor, detector);
        }
    };
    public static final InstrumentatorImpl INSTANCE_229 = new InstrumentatorImpl(true, "Instrumentator<2.2.9>") {
        @Override
        protected CachedInstrumentationStage createCachedStage(ClassVisitor visitor, ClassVisitor detector) {
            return new CachedInstrumentationStage229(this, visitor, detector);
        }

        @Override
        protected UseProxyInstrumentationStage createProxyStage(ClassVisitor visitor, ClassVisitor detector) {
            return new UseProxyInstrumentationStage229(this, visitor, detector);
        }
    };

    public static final InstrumentatorImpl CURRENT_INSTANCE = INSTANCE_229;
//    public static final InstrumentatorImpl CURRENT_INSTANCE = INSTANCE_219;

    private final String name;

    private final boolean addMarkerAnnotations;

    private InstrumentatorImpl(boolean addMarkerAnnotations, String name) {
        this.addMarkerAnnotations = addMarkerAnnotations;
        this.name = name;
    }

    void addMarkerAnnotation(ClassVisitor classVisitor, Type annotationType) {
        if (addMarkerAnnotations) {
            AnnotationVisitor visitor = classVisitor.visitAnnotation(annotationType.getDescriptor(), true);
            visitor.visit("compatibleVersion", MxCache.getCompatibleVersion());
            visitor.visit("version", MxCache.getVersion());
            visitor.visitEnd();
        }
    }

    /**
     * ��������������� �������
     * @param bytecode ������� ������
     * @return ������������������� ������� � ������ �������������� �������,
     * ��� null, ���� �������������� �� ���������
     * @throws com.maxifier.mxcache.IllegalCachedClass ���� ����� �������� ������������ �������������� ������
     */
    @Override
    public ClassInstrumentationResult instrument(byte[] bytecode) {
        boolean containsCached = containsCached(bytecode);
        boolean containsResources = containsResources(bytecode);
        boolean containsUseProxy = containsUseProxy(bytecode);
        if (!containsCached && !containsResources && !containsUseProxy) {
            return null;
        }
        ClassReader classReader = new ClassReader(bytecode);
        ClassWriter classWriter = new SmartClassWriter(classReader);

        List<InstrumentationStage> stages = new ArrayList<InstrumentationStage>();
        ClassVisitor visitor = classWriter;
        ClassVisitor detector = new EmptyVisitor();
        // instrumentation stages are stacked, last added is first passed to class reader
        if (containsResources) {
            InstrumentationStage stage = createResourceStage(visitor, detector);
            stages.add(stage);
            visitor = stage;
            detector = stage.getDetector();
        }
        if (containsCached) {
            InstrumentationStage stage = createCachedStage(visitor, detector);
            stages.add(stage);
            visitor = stage;
            detector = stage.getDetector();
        }
        if (containsUseProxy) {
            // CachedInstrumentationStage removes @Cached annotations so it's impossible to differentiate
            // @UseProxy w/ @Cached and @UseProxy w/o @Cached after it, so we place proxy instrumentation stage
            // before it
            InstrumentationStage stage = createProxyStage(visitor, detector);
            stages.add(stage);
            visitor = stage;
            detector = stage.getDetector();
        }
        classReader.accept(detector, ClassReader.SKIP_FRAMES);

        classReader.accept(visitor, ClassReader.EXPAND_FRAMES);

        if (isClassChanged(stages)) {
            byte[] bytes = classWriter.toByteArray();
            List<ClassDefinition> classes = getAdditionalClasses(stages);

//            ClassLoader cl = new ClassLoader() {};
//            for (ClassDefinition c : classes) {
//                CodegenHelper.verify(c.getBytecode());
//                CodegenHelper.loadClass(cl, c.getBytecode());
//            }
//            CodegenHelper.dumpClass(bytes);
//            CodegenHelper.verify(bytes, cl);
            
            return new ClassInstrumentationResult(bytes, classes);
        }
        return null;
    }

    private ResourceInstrumentationStage createResourceStage(ClassVisitor visitor, ClassVisitor detector) {
        return new ResourceInstrumentationStage(this, visitor, detector);
    }

    protected abstract UseProxyInstrumentationStage createProxyStage(ClassVisitor visitor, ClassVisitor detector);

    protected abstract CachedInstrumentationStage createCachedStage(ClassVisitor visitor, ClassVisitor detector);

    private static boolean isClassChanged(List<InstrumentationStage> stages) {
        for (InstrumentationStage stage : stages) {
            if (stage.isClassChanged()) {
                return true;
            }
        }
        return false;
    }

    private static List<ClassDefinition> getAdditionalClasses(List<InstrumentationStage> stages) {
        List<ClassDefinition> res = new ArrayList<ClassDefinition>();
        for (InstrumentationStage stage : stages) {
            res.addAll(stage.getAdditionalClasses());
        }
        return res;
    }

    private static boolean containsCached(byte[] bytecode) {
        return CACHED_DESCRIPTOR_MATCHER.isContainedIn(bytecode);
    }

    private static boolean containsUseProxy(byte[] bytecode) {
        return USE_PROXY_DESCRIPTOR_MATCHER.isContainedIn(bytecode);
    }

    private static boolean containsResources(byte[] bytecode) {
        return RESOURCE_READER_DESCRIPTOR_MATCHER.isContainedIn(bytecode) ||
               RESOURCE_WRITER_DESCRIPTOR_MATCHER.isContainedIn(bytecode);
    }

    @Override
    public String toString() {
        return name;
    }
}