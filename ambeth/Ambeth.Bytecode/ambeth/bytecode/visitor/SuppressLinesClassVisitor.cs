//package de.osthus.ambeth.bytecode.visitor;

//import de.osthus.ambeth.repackaged.org.objectweb.asm.ClassVisitor;
//import de.osthus.ambeth.repackaged.org.objectweb.asm.MethodVisitor;
//import de.osthus.ambeth.repackaged.org.objectweb.asm.Opcodes;

//public class SuppressLinesClassVisitor extends ClassVisitor
//{
//    public SuppressLinesClassVisitor(ClassVisitor cv)
//    {
//        super(Opcodes.ASM4, cv);
//    }

//    @Override
//    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
//    {
//        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//        return new SuppressLinesMethodVisitor(mv);
//    }
//}