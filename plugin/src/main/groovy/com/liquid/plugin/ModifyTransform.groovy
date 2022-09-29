package com.liquid.plugin

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.io.FileUtils
import org.gradle.api.Project;

class ModifyTransform extends Transform {
    def mProject
    def pool = ClassPool.default

    ModifyTransform(Project project) {
        mProject = project
    }

    private void findTarget(File dir, String fileName) {
        if (dir.isDirectory()) {
            dir.listFiles().each {
                findTarget(it, fileName)
            }
        } else {
            def filePath = dir.absolutePath
            if (filePath.endsWith(".class")) {
                println("---------" + fileName)
                println("---------" + filePath)
                def className = filePath.replace(fileName, "")
                        .replace("\\", ".")
                        .replace("/", ".")
                def name = className
                        .replace(".class", "")
                        .substring(1)

                CtClass ctClass=  pool.get(name)
                addCode(ctClass, fileName)
            }
        }

    }

    private void addCode(CtClass ctClass ,String fileName) {
        ctClass.defrost()
        CtMethod[] methods = ctClass.getDeclaredMethods()
        for (method in methods) {
            println "method "+method.getName()+"参数个数  "+method.getParameterTypes().length
            method.insertAfter("if(true){}")

            if (method.getParameterTypes().length == 1) {
                method.insertBefore("System.out.println(\$1+\"1231321\");")
            }
            if (method.getParameterTypes().length == 2) {
                method.insertBefore("{ System.out.println(\$1); System.out.println(\$2);}")
            }
            if (method.getParameterTypes().length == 3) {
                method.insertBefore("{ System.out.println(\$1);System.out.println(\$2);System.out.println(\\\$3);}")
            }
        }

        ctClass.writeFile(fileName)
        ctClass.detach()
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        //TODO:必须加入！！
        mProject.android.bootClasspath.each {
            pool.appendClassPath(it.absolutePath)
        }

        transformInvocation.inputs.each {
            it.directoryInputs.each {
                //todo:modify class
                def preFileName = it.file.absolutePath
                pool.insertClassPath(preFileName)
                findTarget(it.file, preFileName)
                def dest = transformInvocation.outputProvider.getContentLocation(
                        it.name,
                        it.contentTypes,
                        it.scopes,
                        Format.DIRECTORY
                )

                FileUtils.copyDirectory(it.file, dest)
            }

            it.jarInputs.each {
                def dest = transformInvocation.outputProvider.getContentLocation(it.name
                        , it.contentTypes, it.scopes, Format.JAR)

                FileUtils.copyFile(it.file, dest)
            }
        }

    }


    @Override
    String getName() {
        return "liquid_tran"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }
}
