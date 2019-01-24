package com.asm.asmlib

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.api.transform.Transform
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import com.asm.asmlib.data.AutoMethod
import com.asm.asmlib.data.AutoConfig
import org.objectweb.asm.Opcodes

class AutoPlugin implements Plugin<Project> {

    void apply(Project project) {
        System.out.println("========================")
        System.out.println("=  start gradle plugin!=")
        System.out.println("========================")

        def autoConfig = project.extensions.create("AutoConfig", AutoConfig)
        // 解析RobotMethod
        String json = new JsonBuilder(autoConfig.methods).toString()
        HashSet<AutoMethod> methods = new JsonSlurper().parseText(json) as HashSet<AutoMethod>
        if (methods) {
            //把解析的结果存入 Constants.INJECT_METHOD
            Constants.INJECT_METHOD.addAll(methods)
        }

        def android = project.extensions.getByType(AppExtension.class)
        android.registerTransform(new AutoTransform(project))
    }


    class AutoTransform extends Transform {

        Project project

        AutoTransform(Project project) {
            this.project = project
        }

        @Override
        String getName() {
            return "AutoTransform"
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

        @Override
        void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
            super.transform(transformInvocation)

            Collection<TransformInput> inputs = transformInvocation.inputs
            TransformOutputProvider outputProvider = transformInvocation.outputProvider
            boolean isIncremental = transformInvocation.incremental

            if(!isIncremental){
                outputProvider.deleteAll()
            }

            inputs.each {
                TransformInput input ->
                    input.directoryInputs.each {
                        DirectoryInput directoryInput ->
                            if(directoryInput.file.isDirectory()){
                                directoryInput.file.eachFileRecurse{ File file ->
                                    def name = file.name
                                    //筛选.class类
                                    if (name.endsWith(".class") && !name.startsWith("R\$") &&
                                            !"R.class".equals(name) && !"BuildConfig.class".equals(name)) {
                                        //.class二进制文件解析
                                        ClassReader classReader = new ClassReader(file.bytes)
                                        //.class二进制文件 重新构建编译后的类
                                        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                                        ClassVisitor cv = new AutoClassVisitorPlugin(Opcodes.ASM4 , classWriter)
                                        //开始字节码处理
                                        classReader.accept(cv, ClassReader.EXPAND_FRAMES)
                                        //生成二进制文件并保存成新的文件
                                        byte[] code = classWriter.toByteArray()
                                        FileOutputStream fos = new FileOutputStream(
                                                file.parentFile.absolutePath + File.separator + name)
                                        fos.write(code)
                                        fos.close()
                                    }
                                }
                                def dest = outputProvider.getContentLocation(directoryInput.name,
                                        directoryInput.contentTypes, directoryInput.scopes,
                                        Format.DIRECTORY)

                                println("" + directoryInput.file + " transform" + dest)
                                // 将input的目录复制到output指定目录
                                FileUtils.copyDirectory(directoryInput.file, dest)
                            }
                    }
                    input.jarInputs.each {
                        JarInput jarInput ->
                            //jar文件一般是第三方依赖库jar文件

                            // 重命名输出文件（同目录copyFile会冲突）
                            def jarName = jarInput.name
                            def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                            if (jarName.endsWith(".jar")) {
                                jarName = jarName.substring(0, jarName.length() - 4)
                            }
                            //生成输出路径
                            def dest = outputProvider.getContentLocation(jarName + "_" + md5Name,
                                    jarInput.contentTypes, jarInput.scopes, Format.JAR)

                            println("jar " + jarInput.file.path + " transform " + dest)
                            //将输入内容复制到输出
                            FileUtils.copyFile(jarInput.file, dest)
                    }
            }

        }
    }

}