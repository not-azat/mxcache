package com.maxifier.mxcache.ideaplugin;

import com.intellij.openapi.compiler.FileProcessingCompiler;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.vfs.VirtualFile;
import com.maxifier.mxcache.instrumentation.Instrumentator;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA.
 * User: dalex
 * Date: 29.03.2010
 * Time: 12:31:40
 */
class CachedProcessingItem implements FileProcessingCompiler.ProcessingItem {
    private final Instrumentator instrumentator;
    private final VirtualFile outputDirectory;
    private final VirtualFile file;

    public CachedProcessingItem(VirtualFile outputDirectory, VirtualFile file, Instrumentator instrumentator) {
        this.file = file;
        this.outputDirectory = outputDirectory;
        this.instrumentator = instrumentator;
    }

    public VirtualFile getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    @NotNull
    public VirtualFile getFile() {
        return file;
    }

    @Override
    public ValidityState getValidityState() {
        return new CachedValidityState(file.getModificationStamp());
    }

    @Override
    public String toString() {
        return file.toString();
    }

    public Instrumentator getInstrumentator() {
        return instrumentator;
    }
}
