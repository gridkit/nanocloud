/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.lab.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * Instance of this class is capable of parsing Java byte code (class file), identify call sites and optionally inject call interception if mandated by {@link HookManager}.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@SuppressWarnings("deprecation")
public class ClassRewriter {

    private static final String HOOK_PREFIX = "$$hook_";
    private static final String STUB_PREFIX = "$$stub_";

//	@SuppressWarnings("deprecation")
    private static final String CALL_SITE_HOOK_IMPL = ReflectionMethodCallSiteHookContext.class.getName().replace('.', '/');
    private static final String HOOK_CONTEXT_TYPE = Interception.class.getName().replace('.', '/');

    private HookManager hookManager;
    private ClassWriter writer;

    private String className;
    private int hookCount;
    private List<HookInfo> pendingHooks = new ArrayList<HookInfo>();

    public ClassRewriter(HookManager manager) {
        this.hookManager = manager;
    }

    public byte[] rewrite(byte[] classData) {
        try {
            className = null;
            hookCount = 0;
            ClassReader reader = new ClassReader(classData);
            // TODO somehow avoid frame calculation for non modified methdos
            writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            reader.accept(new ClassInstrumenter(writer), ClassReader.SKIP_FRAMES);

            return hookCount == 0 ? classData : writer.toByteArray();
        }
        finally {
            writer = null;
        }
    }

    private static String[] getParamTypes(String signature) {
        List<String> result = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        int c = signature.lastIndexOf(')');
        String types = signature.substring(1, c);
        boolean longName = false;
        for(int i = 0; i != types.length(); ++i) {
            char x  = types.charAt(i);
            if ('[' == x) {
                sb.append(x);
            }
            else if (';' == x) {
                sb.append(x);
                result.add(sb.toString());
                sb.setLength(0);
                longName = false;
            }
            else if ('L' == x) {
                sb.append(x);
                longName = true;
            }
            else if (longName){
                sb.append(x);
            }
            else {
                sb.append(x);
                result.add(sb.toString());
                sb.setLength(0);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private static String getReturnType(String signature) {
        int c = signature.lastIndexOf(')');
        return signature.substring(c + 1);
    }

    class ClassInstrumenter extends ClassVisitor implements Opcodes {

        public ClassInstrumenter(ClassWriter writer) {
            super(Opcodes.ASM9, writer);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            className = name;
            @SuppressWarnings("unused")
            int minVersion = version >> 16;
            int majVersion = version & 0xFF;
            if (majVersion < 49) {
                // minimum supported version
                version = 49;
            }
            if ("true".equalsIgnoreCase(System.getProperty("gridkit.interceptor.trace"))) {
                System.out.println("Instrumentation [" + className + "] scan ...");
            }
            writer.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,	String signature, String[] exceptions) {
            MethodVisitor visitor = new MethodInstrumenter(name, signature, writer.visitMethod(access, name, desc, signature, exceptions));
            visitor = new JSRInlinerAdapter(visitor, access, name, desc, signature, exceptions);
            return visitor;
        }

        @Override
        public void visitEnd() {
            generatePendingHookMethods();
            super.visitEnd();
        }

        public void generatePendingHookMethods() {
            for(HookInfo hookInfo: pendingHooks) {
                generateHook(hookInfo);
            }
            pendingHooks.clear();
        }

        private void generateHook(HookInfo hookInfo) {
            new HookMethodGenerator(hookInfo).generate();
        }
    }

    class HookMethodGenerator implements Opcodes {

        private HookInfo hookInfo;
        private MethodVisitor mv;
        private boolean isStatic;
        private int lvBase;
        private String returnType;
        private String[] paramTypes;
        private int[] paramOffs;
        private String hookName;
        private String stubName;

        public HookMethodGenerator(HookInfo hookInfo) {
            this.hookInfo = hookInfo;
        }

        public void generate() {

            int hookAccess = 0;
            hookAccess |= ACC_FINAL;
            hookAccess |= ACC_PRIVATE;
            hookAccess |= ACC_STATIC;
            hookAccess |= ACC_SYNTHETIC;

            int stubAccess = 0;
            stubAccess |= ACC_FINAL;
            stubAccess |= ACC_PUBLIC;
            stubAccess |= ACC_STATIC;
            stubAccess |= ACC_SYNTHETIC;

            isStatic = hookInfo.opcode == INVOKESTATIC;

            hookName = HOOK_PREFIX + hookInfo.hookId;
            stubName = STUB_PREFIX + hookInfo.hookId;


            paramTypes = getParamTypes(hookInfo.hookSignature);
            returnType = getReturnType(hookInfo.hookSignature);

            paramOffs = new int[paramTypes.length];
            int n = 0;
            for(int i = 0; i != paramTypes.length; ++i) {
                paramOffs[i] = n;
                if ("J".equals(paramTypes[i]) || "D".equals(paramTypes[i])) {
                    // wide primitive types in Java
                    n += 2;
                }
                else {
                    n += 1;
                }
            }
            lvBase = n;

            // hook method
            mv = writer.visitMethod(hookAccess, hookName, hookInfo.hookSignature, null, null);
            mv.visitCode();
            initParamsArray();
            initInvocationContext();
            callHook();
            handleResult();

            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // stub method
            mv = writer.visitMethod(stubAccess, stubName, hookInfo.hookSignature, null, null);
            mv.visitCode();
            for(int i = 0; i != paramTypes.length; ++i) {
                load_param_for_call(i);
            }
            callTarget();

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        private void initParamsArray() {
            // int param array
            int thisShift = isStatic ? 0 : 1;
            intPush(paramTypes.length - thisShift);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            astore_paramsArray();

            for(int i = thisShift; i != paramTypes.length; ++i) {
                aload_paramsArray();
                intPush(i - thisShift);
                load_param_as_object(i);
                aastore();
            }
        }

        private void load_param_as_object(int n) {
            String type = paramTypes[n];
            if ("Z".equals(type)) {
                mv.visitVarInsn(ILOAD, paramOffs[n]);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
            }
            else if ("B".equals(type)) {
                mv.visitVarInsn(ILOAD, paramOffs[n]);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
            }
            else if ("S".equals(type)) {
                mv.visitVarInsn(ILOAD, paramOffs[n]);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
            }
            else if ("C".equals(type)) {
                mv.visitVarInsn(ILOAD, paramOffs[n]);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
            }
            else if ("I".equals(type)) {
                mv.visitVarInsn(ILOAD, paramOffs[n]);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            }
            else if ("J".equals(type)){
                mv.visitVarInsn(LLOAD, paramOffs[n]);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
            }
            else if ("F".equals(type)) {
                mv.visitVarInsn(FLOAD, paramOffs[n]);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
            }
            else if ("D".equals(type)) {
                mv.visitVarInsn(DLOAD, paramOffs[n]);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
            }
            else {
                mv.visitVarInsn(ALOAD, paramOffs[n]);
            }
        }

        private void load_param_for_call(int n) {
            String type = paramTypes[n];
            if ("Z".equals(type)) {
                mv.visitVarInsn(ILOAD, paramOffs[n]);
            }
            else if ("B".equals(type)) {
                mv.visitVarInsn(ILOAD, paramOffs[n]);
            }
            else if ("S".equals(type)) {
                mv.visitVarInsn(ILOAD, paramOffs[n]);
            }
            else if ("C".equals(type)) {
                mv.visitVarInsn(ILOAD, paramOffs[n]);
            }
            else if ("I".equals(type)) {
                mv.visitVarInsn(ILOAD, paramOffs[n]);
            }
            else if ("J".equals(type)){
                mv.visitVarInsn(LLOAD, paramOffs[n]);
            }
            else if ("F".equals(type)) {
                mv.visitVarInsn(FLOAD, paramOffs[n]);
            }
            else if ("D".equals(type)) {
                mv.visitVarInsn(DLOAD, paramOffs[n]);
            }
            else {
                mv.visitVarInsn(ALOAD, paramOffs[n]);
            }
        }

        private void initInvocationContext() {
            mv.visitTypeInsn(NEW, CALL_SITE_HOOK_IMPL);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, CALL_SITE_HOOK_IMPL, "<init>", "()V");
            astore_hookContext();

            aload_hookContext();
            mv.visitLdcInsn(Type.getType("L" + className + ";"));
            invoke_ctx("setHostClass", "(Ljava/lang/Class;)V");

            aload_hookContext();
            mv.visitLdcInsn(Type.getType("L" + hookInfo.targetClass + ";"));
            invoke_ctx("setTargetClass", "(Ljava/lang/Class;)V");

            aload_hookContext();
            mv.visitLdcInsn(stubName);
            mv.visitLdcInsn(hookInfo.hookSignature);
            invoke_ctx("setStubMethod", "(Ljava/lang/String;Ljava/lang/String;)V");

            aload_hookContext();
            mv.visitLdcInsn(hookInfo.targetMethod);
            mv.visitLdcInsn(hookInfo.targetSignature);
            invoke_ctx("setTargetMethod", "(Ljava/lang/String;Ljava/lang/String;)V");

            if (!isStatic) {
                aload_hookContext();
                load_param_as_object(0);
                invoke_ctx("setThis", "(Ljava/lang/Object;)V");
            }

            aload_hookContext();
            aload_paramsArray();
            invoke_ctx("setParameters", "([Ljava/lang/Object;)V");
        }

        private void callHook() {
            intPush(hookInfo.hookId);
            aload_hookContext();
            String hookClass = hookManager.getInvocationTargetClass();
            String hookMethod = hookManager.getInvocationTargetMethod();
            mv.visitMethodInsn(INVOKESTATIC, hookClass, hookMethod, "(IL" + HOOK_CONTEXT_TYPE + ";)V");
        }

        private void handleResult() {
            aload_hookContext();
            invoke_ctx("isResultReady", "()Z");

            Label handleContextResult = new Label();

            // pass-through to original calling convention
            mv.visitJumpInsn(IFNE, handleContextResult);
            callTarget();

            mv.visitLabel(handleContextResult);
            refreshFrame();

            // checking in exception is pending
            aload_hookContext();
            invoke_ctx("getError", "()Ljava/lang/Throwable;");
            mv.visitInsn(DUP);

            Label returnValue = new Label();

            mv.visitJumpInsn(IFNULL, returnValue);
            mv.visitInsn(ATHROW);

            mv.visitLabel(returnValue);
            refreshFrame();
            if ("V".equals(returnType)) {
                mv.visitInsn(RETURN);
            }
            else {
                aload_hookContext();
                invoke_ctx("getResult", "()Ljava/lang/Object;");
                convert_and_return();
            }
        }

        private void refreshFrame() {
            mv.visitFrame(F_APPEND, 0, new Object[0], 0, null);
        }

        private void callTarget() {
            for(int i = 0; i != paramTypes.length; ++i) {
                load_param_for_call(i);
            }
            mv.visitMethodInsn(hookInfo.opcode, hookInfo.targetClass, hookInfo.targetMethod, hookInfo.targetSignature);
            return_verbatim();
        }

        private void return_verbatim() {
            if ("V".equals(returnType)) {
                mv.visitInsn(RETURN);
            }
            else if ("ZBSCI".contains(returnType)) {
                mv.visitInsn(IRETURN);
            }
            else if ("J".contains(returnType)) {
                mv.visitInsn(LRETURN);
            }
            else if ("F".contains(returnType)) {
                mv.visitInsn(FRETURN);
            }
            else if ("D".contains(returnType)) {
                mv.visitInsn(DRETURN);
            }
            else {
                mv.visitInsn(ARETURN);
            }
        }

        private void convert_and_return() {
            if ("Z".equals(returnType)) {
                mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
                mv.visitInsn(IRETURN);
            }
            else if ("B".equals(returnType)) {
                mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
                mv.visitInsn(IRETURN);
            }
            else if ("S".equals(returnType)) {
                mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
                mv.visitInsn(IRETURN);
            }
            else if ("C".equals(returnType)) {
                mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
                mv.visitInsn(IRETURN);
            }
            else if ("I".equals(returnType)) {
                mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
                mv.visitInsn(IRETURN);
            }
            else if ("J".equals(returnType)){
                mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
                mv.visitInsn(LRETURN);
            }
            else if ("F".equals(returnType)) {
                mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
                mv.visitInsn(FRETURN);
            }
            else if ("D".equals(returnType)) {
                mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
                mv.visitInsn(DRETURN);
            }
            else {
                if (!"java/lang/Object".equals(returnType)) {
                    mv.visitTypeInsn(CHECKCAST, toClassName(returnType));
                }
                mv.visitInsn(ARETURN);
            }
        }

        private String toClassName(String type) {
            if (type.startsWith("L")) {
                return type.substring(1, type.length() - 1);
            }
            else {
                return type;
            }
        }

        private void invoke_ctx(String method, String signature) {
            mv.visitMethodInsn(INVOKEVIRTUAL, CALL_SITE_HOOK_IMPL, method, signature);
        }

        private void aastore() {
            mv.visitInsn(AASTORE);
        }

        private void astore_paramsArray() {
            mv.visitVarInsn(ASTORE, lvBase);
        }

        private void aload_paramsArray() {
            mv.visitVarInsn(ALOAD, lvBase);
        }

        private void astore_hookContext() {
            mv.visitVarInsn(ASTORE, lvBase + 1);
        }

        private void aload_hookContext() {
            mv.visitVarInsn(ALOAD, lvBase +1);
        }

        private void intPush(int ic) {
            if (ic == 0) {
                mv.visitInsn(ICONST_0);
            }
            else if (ic == 1) {
                mv.visitInsn(ICONST_1);
            }
            else if (ic == 2) {
                mv.visitInsn(ICONST_2);
            }
            else if (ic == 3) {
                mv.visitInsn(ICONST_3);
            }
            else if (ic == 4) {
                mv.visitInsn(ICONST_4);
            }
            else if (ic == 5) {
                mv.visitInsn(ICONST_5);
            }
            else if (ic < Byte.MAX_VALUE && ic > Byte.MIN_VALUE) {
                mv.visitIntInsn(BIPUSH, ic);
            }
            else if (ic < Short.MAX_VALUE && ic > Short.MAX_VALUE) {
                mv.visitIntInsn(SIPUSH, ic);
            }
            else {
                mv.visitLdcInsn(Integer.valueOf(ic));
            }
        }
    }

    void addHookMethod(int hookId, int opcode, String hookSignature, String targetClass, String targetMethod, String targetSignature) {

        HookInfo hookInfo = new HookInfo();
        hookInfo.hookId = hookId;
        hookInfo.opcode = opcode;
        hookInfo.hookSignature = hookSignature;
        hookInfo.targetClass = targetClass;
        hookInfo.targetMethod = targetMethod;
        hookInfo.targetSignature = targetSignature;

        hookCount++;
        pendingHooks.add(hookInfo);
    }

    static class HookInfo {
        int hookId;
        int opcode;
        String hookSignature;
        String targetClass;
        String targetMethod;
        String targetSignature;
    }

    class MethodInstrumenter extends MethodVisitor implements Opcodes {

        private String methdoName;
        private String signature;

        public MethodInstrumenter(String name, String signature, MethodVisitor writer) {
            super(Opcodes.ASM9, writer);
            this.methdoName = name;
            this.signature = signature;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
            if ("<init>".equals(name)) {
                // TODO constructor handling
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            }
            else {
                String hostClass = className;
                String hostMethod = methdoName;
                String hostMethodSignature = signature;
                String targetClass = owner;
                String targetMethod = name;
                String targetSignature = desc;

                int hookId = hookManager.checkCallsite(hostClass, hostMethod, hostMethodSignature, targetClass, targetMethod, targetSignature);

                if (hookId < 0) {
                    super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                }
                else {
                    String ndesc = desc;
                    if (opcode != INVOKESTATIC) {
                        ndesc = "(L" + targetClass + ";" + desc.substring(1);
                    }
                    addHookMethod(hookId, opcode, ndesc, targetClass, targetMethod, targetSignature);
                    if ("true".equalsIgnoreCase(System.getProperty("gridkit.interceptor.trace"))) {
                        System.out.println("Instrumentation [" + className + "] instrumenting @" + hostMethod + " call site + " + name + desc);
                    }
                    super.visitMethodInsn(INVOKESTATIC, className, HOOK_PREFIX + hookId, ndesc, isInterface);
                }
            }
        }
    }
}
