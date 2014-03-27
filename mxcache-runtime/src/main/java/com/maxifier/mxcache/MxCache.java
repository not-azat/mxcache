/*
 * Copyright (c) 2008-2014 Maxifier Ltd. All Rights Reserved.
 */
package com.maxifier.mxcache;

import com.maxifier.mxcache.clean.CacheCleaner;
import com.maxifier.mxcache.impl.resource.DependencyNode;
import com.maxifier.mxcache.impl.resource.DependencyTracker;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * This class contains some basic utilities for MxCache.
 *
 * @author Alexander Kochurov (alexander.kochurov@maxifier.com)
 */
public final class MxCache {
    private MxCache() {}

    private static final String VERSION = "2.2.9";

    private static final Version PROJECT_VERSION = loadVersion();

    private static Version loadVersion() {
        String packageVersion = MxCache.class.getPackage().getImplementationVersion();
        if (packageVersion != null) {
            return new Version(packageVersion);
        }
        return loadVersionFromFile();
    }

    private static Version loadVersionFromFile() {
        try {
            InputStream in = MxCache.class.getResourceAsStream("mxcache-version");
            try {
                return new Version(IOUtils.toString(in));
            } finally {
                IOUtils.closeQuietly(in);
            }
        } catch (IOException e) {
            return Version.UNKNOWN;
        }
    }

    /**
     * @return minimal runtime version compatible with current instrumentation
     */
    public static String getCompatibleVersion() {
        return VERSION;
    }

    /**
     * @return Current version of runtime
     */
    public static Version getVersionObject() {
        return PROJECT_VERSION;
    }

    /**
     * @return Current version of runtime
     */
    public static String getVersion() {
        return PROJECT_VERSION.toString();
    }

    /**
     * Executes a piece of code in probe mode.
     * 'Probe mode' disables calculation of caches. If your code queries a cache and there's no value for it yet,
     * the execution will fail and this method will return false.
     * <br />
     * All exceptions of the original code are rethrown. Checked exceptions are wrapped with RuntimeExceptions.
     * <br />
     * <b>Probe mode is broken in current implementation as it will wait for lock if the value is calculated from other
     * thread.</b>
     * @param task the piece of code to execute
     * @return true if execution succeeded, false if it failed due to some caches were not initialized.
     */
    @PublicAPI
    public static boolean probe(Runnable task) {
        DependencyNode prevNode = DependencyTracker.track(DependencyTracker.PROBE_NODE);
        try {
            task.run();
            return true;
        } catch (InternalProbeFailedError e) {
            return false;
        } finally {
            DependencyTracker.exit(prevNode);
        }
    }

    /**
     * Executes a piece of code with bypassing caches.
     * A new value will be calculated for every cache. These values are completely ignored: they are never put to cache.
     * @param task a piece of code to execute
     * @return true always
     */
    @PublicAPI
    public static boolean withoutCache(Runnable task) {
        DependencyNode prevNode = DependencyTracker.track(DependencyTracker.NOCACHE_NODE);
        try {
            task.run();
            return true;
        } finally {
            DependencyTracker.exit(prevNode);
        }
    }

    /**
     * Executes a piece of code in probe mode.
     * 'Probe mode' disables calculation of caches. If your code queries a cache and there's no value for it yet,
     * the execution will fail and this method will throw {@link com.maxifier.mxcache.ProbeFailedException}.
     * <br />
     * All exceptions of the original code are rethrown. Checked exceptions are wrapped with RuntimeExceptions.
     * <br />
     * <b>Probe mode is broken in current implementation as it will wait for lock if the value is calculated from other
     * thread.</b>
     * @param task the piece of code to execute
     * @return the value returned by a piece of code.
     * @throws ProbeFailedException if there was uninitialized cache.
     */
    @PublicAPI
    public static <T> T probe(Callable<T> task) throws ProbeFailedException {
        DependencyNode prevNode = DependencyTracker.track(DependencyTracker.PROBE_NODE);
        try {
            return task.call();
        } catch (InternalProbeFailedError e) {
            throw new ProbeFailedException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            DependencyTracker.exit(prevNode);
        }
    }

    /**
     * Use this method if you want to hide dependencies from some resource or cache without breaking dependency
     * tracking completely. Just wrap your code with Callable and pass it to this method.
     * <br />
     * The callable will be invoked immediately in the same thread, the result will be returned to you.
     */
    @PublicAPI
    public static <T> T hideCallerDependencies(CallableWithoutExceptions<T> callable) {
        DependencyNode node = DependencyTracker.track(DependencyTracker.HIDDEN_CALLER_NODE);
        try {
            return callable.call();
        } finally {
            DependencyTracker.exit(node);
        }
    }

    /**
     * Use this method if you want to hide dependencies from some resource or cache without breaking dependency
     * tracking completely. Just wrap your code with Callable and pass it to this method.
     * <br />
     * The callable will be invoked immediately in the same thread, the result will be returned to you.
     */
    @PublicAPI
    public static <T> T hideCallerDependencies(Callable<T> callable) throws Exception {
        DependencyNode node = DependencyTracker.track(DependencyTracker.HIDDEN_CALLER_NODE);
        try {
            return callable.call();
        } finally {
            DependencyTracker.exit(node);
        }
    }

    /**
     * Use this method if you want to hide dependencies from some resource or cache without breaking dependency
     * tracking completely. Just wrap your code with Callable and pass it to this method.
     * <br />
     * The runnable will be invoked immediately in the same thread.
     */
    @PublicAPI
    public static void hideCallerDependencies(Runnable runnable) {
        DependencyNode node = DependencyTracker.track(DependencyTracker.HIDDEN_CALLER_NODE);
        try {
            runnable.run();
        } finally {
            DependencyTracker.exit(node);
        }
    }

    /**
     * Use this method to obtain cache cleaning service.
     * @return cache cleaner
     */
    @PublicAPI
    public static CacheCleaner getCleaner() {
        return CacheFactory.getCleaner();
    }
}
