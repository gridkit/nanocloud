package org.gridkit.vicluster.isolate.btrace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import net.java.btrace.agent.Session.State;
import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.extensions.ExtensionsRepository;
import net.java.btrace.api.extensions.ExtensionsRepository.Location;
import net.java.btrace.instr.ClassFilter;
import net.java.btrace.instr.ClassRenamer;
import net.java.btrace.instr.ClinitInjector;
import net.java.btrace.instr.InstrumentUtils;
import net.java.btrace.instr.Instrumentor;
import net.java.btrace.instr.MethodRemover;
import net.java.btrace.instr.OnMethod;
import net.java.btrace.instr.Preprocessor;
import net.java.btrace.instr.Verifier;
import net.java.btrace.org.objectweb.asm.ClassReader;
import net.java.btrace.org.objectweb.asm.ClassVisitor;
import net.java.btrace.org.objectweb.asm.ClassWriter;
import net.java.btrace.org.objectweb.asm.Opcodes;

import org.gridkit.vicluster.isolate.IsolateClassTransformer;

public class BTraceClassTransformer implements IsolateClassTransformer {

    private AtomicReference<State> state = new AtomicReference<State>(State.DISCONNECTED);
    
    private String className;
    private volatile List<OnMethod> onMethods;
    private volatile boolean hasSubclassChecks;
    private volatile ClassFilter filter;
    private volatile boolean skipRetransforms;
    private volatile byte[] btraceCode;
    final private Set<String> instrumentedClasses = new HashSet<String>();
    
    private ExtensionsRepository extRepo;
    {
		extRepo = new ExtensionsRepository(this.getClass().getClassLoader(), Location.SERVER) {
			@Override
			public String getExtensionsPath() {
				return "";
			}
		};    	
    }
    
    public BTraceClassTransformer(Class<?> probe) throws IOException {
    	className = probe.getName();
    	loadTraceClass(probe);    	
    }
    
    @Override
	public byte[] transform(ClassLoader cl, String name, byte[] originalClass) {
    	if (name.equals(className)) {
    		return btraceCode;
    	}
    	try {
			byte[] st1 = transformClInit(name, null, originalClass);
			if (st1 == null) {
				st1 = originalClass;
			}
			byte[] st2 = transformTrace(name, null, st1);
			if (st2 == null) {
				st2 = originalClass;
			}
			return st2;
		} catch (IllegalClassFormatException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] transformClInit(final String cname, Class<?> classBeingRedefined, byte[] classfileBuffer) {
    	if (!hasSubclassChecks || classBeingRedefined != null || isBTraceClass(cname) || isSensitiveClass(cname)) {
    		return null;
    	}
    	
    	if (!skipRetransforms) {
    		BTraceLogger.debugPrint("injecting <clinit> for " + cname); // NOI18N
    		ClassReader cr = new ClassReader(classfileBuffer);
    		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
    		ClinitInjector injector = new ClinitInjector(cw, className, cname);
    		InstrumentUtils.accept(cr, injector);
    		if (injector.isTransformed()) {
    			byte[] instrumentedCode = cw.toByteArray();
    			BTraceLogger.dumpClass(cname + "_clinit", instrumentedCode); // NOI18N
    			return instrumentedCode;
    		}
    	} else {
    		BTraceLogger.debugPrint("client " + className + ": skipping transform for " + cname); // NOI18N
    	}
    	return null;
    }
    
    private byte[] transformTrace(final String cname,
    		Class<?> classBeingRedefined, byte[] classfileBuffer)
    				throws IllegalClassFormatException {
    	try {
    		if (isBTraceClass(cname) || isSensitiveClass(cname)) {
    			BTraceLogger.debugPrint("skipping transform for BTrace class " + cname); // NOI18N
    			return null;
    		}
    		
    		if (classBeingRedefined != null) {
    			// class already defined; retransforming
    			if (!skipRetransforms && filter.isCandidate(classBeingRedefined)) {
    				return doTransform(classBeingRedefined, cname, classfileBuffer);
    			} else {
    				BTraceLogger.debugPrint("client " + className + ": skipping transform for " + cname); // NOi18N
    			}
    		} else {
    			// class not yet defined
    			if (!hasSubclassChecks) {
    				if (filter.isCandidate(classfileBuffer)) {
    					return doTransform(classBeingRedefined, cname, classfileBuffer);
    				} else {
    					BTraceLogger.debugPrint("client " + className + ": skipping transform for " + cname); // NOI18N
    				}
    			}
    		}
    		
    		return null; // ignore
    	} catch (Exception e) {
    		e.printStackTrace();
    		if (e instanceof IllegalClassFormatException) {
    			throw (IllegalClassFormatException) e;
    		}
    		return null;
    	} finally {
    	}
    }
    
    public BTraceClassTransformer() throws IOException {
    }
    
    State getState() {
        return state.get();
    }

    public boolean loadTraceClass(Class<?> probe) throws IOException {
    	InputStream is = probe.getClassLoader().getResourceAsStream(probe.getName().replace('.', '/') + ".class");
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	byte[] buf = new byte[4 << 10];
    	while(true) {
    		int n = is.read(buf);
    		if (n < 0) {
    			break;
    		}
    		else {
    			bos.write(buf, 0, n);
    		}
    	}
    	is.close();
    	byte[] cd = bos.toByteArray();
    	return loadTraceClass(cd, new String[0]);
    }
    
    public boolean loadTraceClass(byte[] traceCode, String[] args) {
        Throwable capturedError = null;
        try {
            try {
                verify(traceCode);
            } catch (Throwable th) {
                capturedError = th;
                return false;
            }
            filter = new ClassFilter(onMethods);
            BTraceLogger.debugPrint("created class filter"); // NOI18N
            ClassWriter writer = InstrumentUtils.newClassWriter(traceCode);
            ClassReader reader = new ClassReader(traceCode);
            ClassVisitor visitor = new Preprocessor(writer);
            String traceName = className;
            BTraceLogger.dumpClass(traceName + "_orig", traceCode); // NOI18N
            if (!traceName.equals(className)) {
                BTraceLogger.debugPrint("class " + className + " renamed to " + traceName); // NOI18N
                // FIXME
                //            onCommand(new RenameCommand(className));
                visitor = new ClassRenamer(traceName, visitor);
            }
            className = traceName;
            try {
                BTraceLogger.debugPrint("preprocessing BTrace class " + className); // NOI18N
                InstrumentUtils.accept(reader, visitor);
                BTraceLogger.debugPrint("preprocessed BTrace class " + className); // NOI18N
                traceCode = writer.toByteArray();
            } catch (Throwable th) {
                capturedError = th;
                return false;
            }
            BTraceLogger.dumpClass(className + "_proc", traceCode); // NOI18N
            btraceCode = traceCode;
            BTraceLogger.debugPrint("creating BTraceRuntime instance for " + className); // NOI18N
            BTraceLogger.debugPrint("created BTraceRuntime instance for " + className); // NOI18N
            BTraceLogger.debugPrint("removing @OnMethod, @OnProbe methods"); // NOI18N
            byte[] codeBuf = removeMethods(traceCode);
            BTraceLogger.dumpClass(traceName, codeBuf);
            BTraceLogger.debugPrint("removed @OnMethod, @OnProbe methods"); // NOI18N
            // This extra BTraceRuntime.enter is needed to
            // check whether we have already entered before.
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (capturedError == null) {
                return true;
            } else {
                BTraceLogger.debugPrint(capturedError);
                throw new RuntimeException(capturedError);
            }
        }
    }

    private void verify(byte[] buf) {
        ClassReader reader = new ClassReader(buf);
        Verifier verifier = new Verifier(new ClassVisitor(Opcodes.ASM4){}, true, extRepo);
        BTraceLogger.debugPrint("verifying BTrace class"); // NOI18N
        InstrumentUtils.accept(reader, verifier);
        className = verifier.getClassName().replace('/', '.');
        BTraceLogger.debugPrint("verified '" + className + "' successfully"); // NOI18N
        onMethods = verifier.getOnMethods();
        for (OnMethod om : onMethods) {
            if (om.getClazz().startsWith("+")) {
                hasSubclassChecks = true;
                break;
            }
        }
    }

    private static byte[] removeMethods(byte[] buf) {
        ClassWriter writer = InstrumentUtils.newClassWriter(buf);
        ClassReader reader = new ClassReader(buf);
        InstrumentUtils.accept(reader, new MethodRemover(writer));
        return writer.toByteArray();
    }

    private static boolean isBTraceClass(String name) {
        return name.startsWith("net/java/btrace"); // NOI18N
    }

    /*
     * Certain classes like java.lang.ThreadLocal and it's
     * inner classes, java.lang.Object cannot be safely
     * instrumented with BTrace. This is because BTrace uses
     * ThreadLocal class to check recursive entries due to
     * BTrace's own functions. But this leads to infinite recursions
     * if BTrace instruments java.lang.ThreadLocal for example.
     * For now, we avoid such classes till we find a solution.
     */
    private static boolean isSensitiveClass(String name) {
        return name.equals("java/lang/Object") || // NOI18N
                name.startsWith("java/lang/ThreadLocal") || // NOI18N
                name.startsWith("sun/reflect") || // NOI18N
                name.equals("sun/misc/Unsafe") || // NOI18N
                name.startsWith("sun/security/") || // NOI18N
                name.equals("java/lang/VerifyError"); // NOI18N
    }

    private byte[] doTransform(Class<?> classBeingRedefined, final String cname, byte[] classfileBuffer) {
        BTraceLogger.debugPrint("client " + className + ": instrumenting " + cname); // NOI18N
//        classes.add(new WeakReference<Class<?>>(classBeingRedefined));
        return instrument(classBeingRedefined, cname, classfileBuffer);
    }

    private byte[] instrument(Class clazz, String cname, byte[] target) {
        byte[] instrumentedCode;
        try {
            ClassWriter writer = InstrumentUtils.newClassWriter(target);
            ClassReader reader = new ClassReader(target);
            Instrumentor i = new Instrumentor(clazz, className, btraceCode, onMethods, writer);
            InstrumentUtils.accept(reader, i);
            if (!i.hasMatch()) {
                BTraceLogger.debugPrint("*WARNING* No method was matched for class " + cname); // NOI18N
            } else {
                instrumentedClasses.add(cname.replace('/', '.'));
            }
            instrumentedCode = writer.toByteArray();
        } catch (Throwable th) {
            BTraceLogger.debugPrint(th);
            return null;
        }
        BTraceLogger.dumpClass(cname, instrumentedCode);
        return instrumentedCode;
    }
}
