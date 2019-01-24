package com.asm.asmlib

import com.asm.asmlib.data.AutoMethod
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AutoClassVisitorPlugin extends ClassVisitor{

    AutoClassVisitorPlugin(int i, ClassVisitor classVisitor) {
        super(i, classVisitor)
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        AutoMethod method = Constants.INJECT_METHOD.find {
            name.equals(it.oName) && desc.equals(it.oDesc)
        }
        if(method){
            return new AutoMethodVisitor(Opcodes.ASM4 , super.visitMethod(access, name, desc, signature, exceptions))
        }
        return super.visitMethod(access, name, desc, signature, exceptions)
    }
}