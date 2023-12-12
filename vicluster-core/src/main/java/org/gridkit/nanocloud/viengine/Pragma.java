package org.gridkit.nanocloud.viengine;


public interface Pragma {


    public String PROP = "prop:";
    public String DEFAULT = "default:";
    public String INSTANTIATE = "new-instance:";
    public String LAZY = "lazy:";

    public String VINODE_NAME = "prop:vinode.name";
    public String VINODE_TYPE = "prop:vinode.type";

    public String BOOT_PHASE = "boot:phase";
    public String BOOT_SEQUENCE = "boot:boot-sequence";
    public String BOOT_PHASE_PRE = "boot:phase-pre.";
    public String BOOT_PHASE_POST = "boot:phase-post.";
    public String BOOT_ANNOTATION = "boot:annotation.";
    public String BOOT_ACTION = "boot:action.";
    public String BOOT_VALIDATOR = "boot:validator.";
    public String BOOT_TYPE_INITIALIZER = "boot:type-initializer";

    public String LOGGER_FACTORY = "logger:factory:";
    public String LOGGER_NAME = "logger:name:";
    public String LOGGER_LEVEL = "logger:level:";
    public String LOGGER_STREAM = "logger:stream:";

    public String NODE_CLOUD_CONTEXT = "node:cloud";
    public String NODE_NAME = "node:name";
    public String NODE_TYPE = "node:type";

    public String NODE_STARTUP_HOOK = "node:startup-hook.";
    public String NODE_PRE_SHUTDOWN_HOOK = "node:pre-shutdown-hook.";
    public String NODE_SHUTDOWN_HOOK = "node:shutdown-hook.";
    public String NODE_POST_SHUTDOWN_HOOK = "node:post-shutdown-hook.";

    public String NODE_PRAGMA_HANDLER = "node:pragma-handler:";

    public String NODE_TRIGGER = "#trigger:";
    public String NODE_FINALIZER = "#finalizer:";

    public String RUNTIME_CLASSPATH = "node-runtime:classpath";
    public String RUNTIME_SHALLOW_CLASSPATH = "node-runtime:shallow-classpath";
    public String RUNTIME_AGENTS = "node-runtime:agents";
    public String RUNTIME_HOST_CONTROL_CONSOLE = "node-runtime:host-control-console";
    public String RUNTIME_REMOTING_SESSION = "node-runtime:remoting-session";
    public String RUNTIME_REMOTING_SESSION_WRAPER = "node-runtime:remoting-session-wrapper.";
    public String RUNTIME_PROCESS_LAUNCHER = "node-runtime:process-launcher";
    public String RUNTIME_MANAGED_PROCESS = "node-runtime:managed-process";
    public String RUNTIME_EXECUTOR = "node-runtime:executor";
    public String RUNTIME_STOP_SWITCH = "node-runtime:stop-switch";
    public String RUNTIME_KILL_SWITCH = "node-runtime:kill-switch";
    public String RUNTIME_TEXT_TERMINAL = "node-runtime:text-terminal";

    public String REMOTE_PROTOCOL = "remote-protocol:";

    // Remote node internal
    public String RUNTIME_HOST_CONNECTOR = "node-runtime:remote-host-connector";
    public String RUNTIME_REMOTE_CONNECTION_MANAGE = "node-runtime:remote-connection-manager";

}
