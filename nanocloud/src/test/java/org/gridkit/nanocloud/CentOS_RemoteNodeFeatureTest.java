package org.gridkit.nanocloud;

import static org.gridkit.nanocloud.RemoteNode.REMOTE;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CentOS_RemoteNodeFeatureTest extends ViNodeFeatureTest {

    @BeforeClass
    public static void check_cbox1() {
        Cloud c = CloudFactory.createCloud();
        try {
            c.node("**").x(REMOTE)
                .useSimpleRemoting()
                .setRemoteHost("cbox1");

            c.node("test").touch();
            c.shutdown();
        }
        catch(Exception e) {
            e.printStackTrace();
            Assume.assumeTrue(false);
        }
        finally {
            c.shutdown();
        }
    }

    @Before
    @Override
    public void initCloud() {
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(REMOTE)
            .useSimpleRemoting()
            .setRemoteHost("cbox1");
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

    @Override
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
    public void test_handle_NoDefClassFound_on_return(){
        super.test_handle_NoDefClassFound_on_return();
    }

    @Test
    @Override
    public void test_inherit_cp_true() throws IOException, URISyntaxException {
        super.test_inherit_cp_true();
    }

    @Test
    @Override
    public void test_inherit_cp_shallow() throws IOException, URISyntaxException {
        super.test_inherit_cp_shallow();
    }

    @Test
    @Override
    public void test_inherit_cp_default_true() {
        super.test_inherit_cp_default_true();
    }

    @Test
    @Override
    public void test_classpath_limiting() throws MalformedURLException, URISyntaxException {
        super.test_classpath_limiting();
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

    @Test
    @Override
    public void verify_lifecycle_listener_receive_exit_code() throws Exception {
        super.verify_lifecycle_listener_receive_exit_code();
    }

    @Test
    @Override
    public void verify_lifecycle_listener_with_invalid_executable() throws Exception {
        super.verify_lifecycle_listener_with_invalid_executable();
    }

    @Test
    @Override
    public void verify_lifecycle_listener_with_invalid_arg() throws Exception {
        super.verify_lifecycle_listener_with_invalid_arg();
    }

    @Test
    @Override
    public void verify_exit_code_is_available() throws Exception {
        super.verify_exit_code_is_available();
    }

    @Test
    @Override
    public void verify_exit_code_is_reported() {
        super.verify_exit_code_is_reported();
    }
}
