package com.maxifier.mxcache.ideaplugin.inspections;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.maxifier.mxcache.ideaplugin.MxCacheInspection;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
* User: dalex
* Date: 17.03.2010
* Time: 11:39:36
*/
public class ResourceCompatibilityInspection extends MxCacheInspection {
    private static final String INSPECTION_ID = "MxCacheResourceCompatibility";

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "MxCache resource compatibility problems";
    }

    @NotNull
    @Override
    public String getShortName() {
        return INSPECTION_ID;
    }

    @Override
    protected void checkMethod(PsiMethod method, InspectionManager inspectionManager, List<ProblemDescriptor> res) {
        PsiModifierList modifiers = method.getModifierList();
        boolean hasResourceAccessor = false;
        PsiElement cached = null;
        PsiElement explicitDependency = null;
        for (PsiAnnotation annotation : modifiers.getAnnotations()) {
            PsiJavaCodeReferenceElement referenceElement = annotation.getNameReferenceElement();
            if (referenceElement != null) {
                String qualifiedName = referenceElement.getQualifiedName();
                hasResourceAccessor |= isResourceAccessor(qualifiedName);
                if (isExplicitDependency(qualifiedName)) {
                    explicitDependency = annotation;
                }
                if (isCached(qualifiedName)) {
                    cached = annotation;
                }
            }
        }
        if (hasResourceAccessor && cached != null) {
            reportError(inspectionManager, res, cached, "@Cached method should not have @ResourceReader and @ResourceWriter annotations");
        }
        if (explicitDependency != null && cached == null) {
            reportError(inspectionManager, res, explicitDependency, "@ResourceDependency annotation should be applied only to @Cached methods");
        }
    }

    @NotNull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}