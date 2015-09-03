package org.gridkit.nanocloud.viengine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.gridkit.nanocloud.ViNodeFeatureTest;
import org.gridkit.nanocloud.VX;
import org.junit.Test;

public class Engine2_LocalNodeFeatureTest extends ViNodeFeatureTest {

    @Override
    public void initCloud() {
        cloud = Engine2.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
    }
    
    @Test
    @Override
    public void verify_isolated_static_with_void_callable() {
        super.verify_isolated_static_with_callable();
    }

    @Test
    @Override
    public void verify_isolated_static_with_callable() {
        super.verify_isolated_static_with_callable();
    }

    @Test
    @Override
    public void verify_isolated_static_with_runnable() {
        super.verify_isolated_static_with_runnable();
    }

    public void verify_class_exclusion() {
        // class sharing is not supported by local nodes, obviously
    }       

    @Test
    @Override
    public void verify_property_isolation() throws Exception {
        super.verify_property_isolation();
    }
    
    @Test
    @Override
    public void verify_exec_stack_trace_locality() {
        super.verify_exec_stack_trace_locality();
    }

    @Test
    @Override
    public void verify_transparent_proxy_stack_trace() {
        super.verify_transparent_proxy_stack_trace();
    }

    @Test
    @Override
    public void verify_transitive_transparent_proxy_stack_trace() {
        super.verify_transitive_transparent_proxy_stack_trace();
    }

    @Test
    @Override
    public void test_classpath_extention() throws IOException, URISyntaxException {
        super.test_classpath_extention();
    }

    @Test
    @Override
    public void test_classpath_limiting() throws MalformedURLException, URISyntaxException {
        super.test_classpath_limiting();
    }

    @Test
    @Override
    public void test_dont_inherit_cp() {
        super.test_dont_inherit_cp();
    }

    @Test
    @Override
    public void test_handle_NoDefClassFound(){
        super.test_handle_NoDefClassFound();
    }

    @Test
    @Override
    public void test_inherit_cp_true() throws IOException, URISyntaxException {
        super.test_inherit_cp_true();
    }

    @Test
    @Override
    public void test_inherit_cp_default_true() {
        super.test_inherit_cp_default_true();
    }

    @Test
    @Override
    public void test_annonimous_primitive_in_args() {
        super.test_annonimous_primitive_in_args();
    }

    @Test
    @Override
    public void verify_new_env_variable() {
        super.verify_new_env_variable();
    }

    @Test
    @Override
    public void verify_env_variable_removal() {
        super.verify_env_variable_removal();
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
    public void verify_slave_working_dir() throws IOException {
        super.verify_slave_working_dir();
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
