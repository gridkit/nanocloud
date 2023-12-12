package org.gridkit.nanocloud;

import org.gridkit.nanocloud.telecontrol.isolate.IsolateConfig;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViConf.ClasspathConf;
import org.gridkit.vicluster.ViConf.ConsoleConf;
import org.gridkit.vicluster.ViConf.HookConf;
import org.gridkit.vicluster.ViConf.JvmConf;
import org.gridkit.vicluster.ViConf.RuntimeEx;
import org.gridkit.vicluster.ViConf.TypeConf;
import org.gridkit.vicluster.ViConfExtender;
import org.gridkit.vicluster.ViConfigurable;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeExtender;

/**
 * This class is a central reference point for various {@link ViNode} extenders.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class VX {

    public static final ViConfExtender<TypeConf> TYPE = new ViConfExtender<ViConf.TypeConf>() {
        @Override
        public TypeConf wrap(ViConfigurable node) {
            return TypeConf.at(node);
        }
    };

    /**
     * @see ConsoleConf
     */
    public static final ViConfExtender<ConsoleConf> CONSOLE = new ViConfExtender<ViConf.ConsoleConf>() {
        @Override
        public ConsoleConf wrap(ViConfigurable node) {
            return ConsoleConf.at(node);
        }
    };

    /**
     * @see ClasspathConf
     */
    public static final ViConfExtender<ClasspathConf> CLASSPATH = new ViConfExtender<ViConf.ClasspathConf>() {
        @Override
        public ClasspathConf wrap(ViConfigurable node) {
            return ClasspathConf.at(node);
        }
    };

    /**
     * Process/JVM configuration helper.
     *
     * @see JvmConf
     */
    public static final ViConfExtender<JvmConf> PROCESS = new ViConfExtender<JvmConf>() {
        @Override
        public JvmConf wrap(ViConfigurable node) {
            return JvmConf.at(node);
        }
    };

    /**
     * Tags node as local process node, provide access to {@link #PROCESS} configuration.
     *
     * @see JvmConf
     */
    public static final ViConfExtender<JvmConf> LOCAL = new ViConfExtender<JvmConf>() {
        @Override
        public JvmConf wrap(ViConfigurable node) {
            node.x(TYPE).setLocal();
            return JvmConf.at(node);
        }
    };


    public static final ViConfExtender<JvmConf> JVM = PROCESS;

    public static final ViConfExtender<IsolateConfig> ISOLATE = new ViConfExtender<IsolateConfig>() {

        @Override
        public IsolateConfig wrap(ViConfigurable node) {
            IsolateConfig conf = IsolateConfig.at(node);
            conf.setIsolateNodeType();
            return conf;
        }
    };

    public static final ViConfExtender<HookConf> HOOK = new ViConfExtender<HookConf>() {

        @Override
        public HookConf wrap(ViConfigurable node) {
            return HookConf.at(node);
        }
    };

    public static final ViNodeExtender<RuntimeEx> RUNTIME = new ViNodeExtender<RuntimeEx>() {

        @Override
        public RuntimeEx wrap(ViNode node) {
            return RuntimeEx.at(node);
        }
    };

    public static final ViConfExtender<RemoteEx> REMOTE = new ViConfExtender<RemoteEx>() {
        @Override
        public RemoteEx wrap(ViConfigurable node) {
            return RemoteEx.at(node).setRemoteNodeType();
        }
    };
}
