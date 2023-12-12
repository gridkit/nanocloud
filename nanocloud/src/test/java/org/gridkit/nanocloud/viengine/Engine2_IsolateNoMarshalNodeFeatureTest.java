package org.gridkit.nanocloud.viengine;

import static org.gridkit.nanocloud.VX.ISOLATE;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.gridkit.nanocloud.ViNodeFeatureTest;
import org.junit.Assume;
import org.junit.Test;

public class Engine2_IsolateNoMarshalNodeFeatureTest extends ViNodeFeatureTest {

    @Override
    protected void assumeOutOfProcess() {
        Assume.assumeTrue(false);
    }

    @Override
    protected void assumeClassIsolation() {
        Assume.assumeTrue(false);
    }


    @Override
    public void initCloud() {
        cloud = Engine2.createCloud();
        cloud.x(ISOLATE)
            .shareAllClasses(true)
            .noMarshal(true);
    }

    @Test
    @Override
    public void verify_isolation_static_with_void_callable() {
        super.verify_isolation_static_with_callable();
    }

    @Test
    @Override
    public void verify_isolation_static_with_callable() {
        super.verify_isolation_static_with_callable();
    }

    @Test
    @Override
    public void verify_isolation_static_with_runnable() {
        super.verify_isolation_static_with_runnable();
    }

    @Override
    public void verify_classpath_class_sharing() {
        // class sharing is not supported by local nodes, obviously
    }

    @Test
    @Override
    public void verify_isolation_system_properties() throws Exception {
        super.verify_isolation_system_properties();
    }

    @Test
    @Override
    public void verify_execution_stack_trace_locality() {
        super.verify_execution_stack_trace_locality();
    }

    @Test
    @Override
    public void verify_execution_transparent_proxy_stack_trace() {
        super.verify_execution_transparent_proxy_stack_trace();
    }

    @Test
    @Override
    public void verify_execution_transitive_transparent_proxy_stack_trace() {
        super.verify_execution_transitive_transparent_proxy_stack_trace();
    }

    @Test
    @Override
    public void verify_classpath_extention() throws IOException, URISyntaxException {
        super.verify_classpath_extention();
    }

    @Test
    @Override
    public void verify_classpath_limiting() throws MalformedURLException, URISyntaxException {
        super.verify_classpath_limiting();
    }

    @Test
    @Override
    public void verify_classpath_dont_inherit_cp() {
        super.verify_classpath_dont_inherit_cp();
    }

    @Test
    @Override
    public void verify_classpath_handle_NoDefClassFound(){
        super.verify_classpath_handle_NoDefClassFound();
    }

    @Test
    @Override
    public void verify_classpath_handle_NoDefClassFound_on_return(){
        super.verify_classpath_handle_NoDefClassFound_on_return();
    }

    @Test
    @Override
    public void verify_classpath_inherit_cp_true() throws IOException, URISyntaxException {
        super.verify_classpath_inherit_cp_true();
    }

    @Test
    @Override
    public void verify_classpath_inherit_cp_default_true() {
        super.verify_classpath_inherit_cp_default_true();
    }

    @Test
    @Override
    public void verify_execution_annonimous_primitive_in_args() {
        super.verify_execution_annonimous_primitive_in_args();
    }

    @Test
    @Override
    public void verify_runtime_process_new_env_variable() {
        super.verify_runtime_process_new_env_variable();
    }

    @Test
    @Override
    public void verify_runtime_process_env_variable_removal() {
        super.verify_runtime_process_env_variable_removal();
    }

    @Test
    @Override
    public void verify_jvm_single_arg_passing() {
        super.verify_jvm_single_arg_passing();
    }

    @Test
    @Override
    public void verify_jvm_multiple_args_passing() {
        super.verify_jvm_multiple_args_passing();
    }

    @Test
    @Override
    public void verify_jvm_invalid_arg_error() {
        super.verify_jvm_invalid_arg_error();
    }

    @Test
    @Override
    public void verify_runtime_process_working_dir() throws IOException {
        super.verify_runtime_process_working_dir();
    }

    @Test
    @Override
    public void verify_jvm_agent() throws Exception {
        super.verify_jvm_agent();
    }

    @Test
    @Override
    public void verify_jvm_agent_with_options() throws Exception {
        super.verify_jvm_agent_with_options();
    }

    @Test
    @Override
    public void verify_jvm_agent_multiple_agents() throws Exception {
        super.verify_jvm_agent_multiple_agents();
    }
}
