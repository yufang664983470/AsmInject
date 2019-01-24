package com.asm.asmlib

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AutoMethodVisitor extends MethodVisitor{

    AutoMethodVisitor(int i, MethodVisitor methodVisitor) {
        super(i, methodVisitor)
    }

    @Override
    void visitCode() {
        super.visitCode()
        //相当于ClickFun.doClick(view);
        mv.visitVarInsn(Opcodes.ILOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/asm/asminjectdemo/ClickFun", "doClick", "(Landroid/view/View;)V", false)
    }
}