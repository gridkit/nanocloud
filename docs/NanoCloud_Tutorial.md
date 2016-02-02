NanoCloud tutorial
====

Introduction
----

[NanoCloud][nc] is an API and library allowing you seamlessly manipulate distributed execution of code.

Originally this API has started as a test library emulating distributed cluster in single JVM (for sake of test automation).
As API matured, idea to use exactly same API to manipulate real distributed clusters have become very attractive.

In particular, this library is positioned as backbone for distributed performance/stress testing.

Getting started with [NanoCloud][nc]
----

### Test project
Please checkout test project at 
[https://github.com/gridkit/nanocloud-getting-started](https://github.com/gridkit/nanocloud-getting-started).
This project contains code referenced in this tutorial.

All tutorial examples are implemented as runnable JUnit test and organized into few files:

 * [StartingWithLocalCloud.java][StartingWithLocalCloud.java] - executing code in other java processes on same box.
 * [StartingWithDistributedCloud.java][StartingWithDistributedCloud.java] - executing code remotely using SSH.
 * [BasicViNodeUsage.java][BasicViNodeUsage.java] - essentials of [NanoCloud][nc] API.
 * [TransaprentRmi.java][TransaprentRmi.java] - examples of "transparent" RMI between nodes.

Examples in Java sources above may be slightly ahead of tutorial text. 

### Quick introduction of [ViNode][vinode]
[NanoCloud][nc] API is build around concept of [ViNode][vinode]. With a little oversimplification, 
ViNode is a remote process which allow us to execute code. ViNode offers a set of method to executed code. 
ViNode could also be a group of real nodes (thus providing us simple way for parallel execution).

There are 3 standard type of nodes backing ViNode instances:

 * separate JVM, running locally
 * separate JVM, started remotely started via SSH
 * thread group inside of current JVM with option ClassLoader isolation (Isolate node)

Last option is extremely useful for debugging.

### Local cloud

Let's start with local nodes.

Open [StartingWithLocalCloud.java][StartingWithLocalCloud.java].

#### Step 1

````Java
    @Test
    public void test_hello_world__version1() {
        // Let's create simple cloud where slaves will run on same box with master
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        
        // This line says that 'node1' should exists
        // all initialization are lazy and asynchronous
        // so this line will not trigger any process creation
        cloud.node("node1");
        
        // two starts will match any node name
        ViNode allNodes = cloud.node("**");
        
        // let our node to say hello
        allNodes.exec(new Callable<Void>() {
        
            @Override
            public Void call() throws Exception {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("My name is '" + jvmName + "'. Hello!");
                return null;
            }
        });     
    }
````

In console you would see something like this:

    07:25:55.208 [ViNode[node1] init] WARN  o.g.vicluster.telecontrol.Classpath - Classpath entry is empty: C:\WarZone\spaces\nanocloud-getting-started\target\classes
    07:25:55.212 [ViNode[node1] init] WARN  o.g.vicluster.telecontrol.Classpath - Cannot copy URL content: file:/C:/WarZone/spaces/nanocloud-getting-started/target/classes/
    [node1] My name is '1456@ws4199'. Hello!

Important part is

    [node1] My name is '1456@ws4199'. Hello!

This is a result of `System.out.println()` from our runnable.
All outputs from nodes are redirected to parent process console and prefixed with node name for convenience.

#### Step 2

````Java
    @Test
    public void test_hello_world__version2() {
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();

        // let's create a few more nodes this time
        cloud.nodes("node1", "node2", "node3", "node4");
        
        // say hello
        cloud.node("**").exec(new Callable<Void>() {
        
            @Override
            public Void call() throws Exception {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("My name is '" + jvmName + "'. Hello!");
                return null;
            }
        });     
    }
````

Now we have created 4 different slave processes and got responses from all of them.

    07:27:12.972 [ViNode[node3] init] WARN  o.g.vicluster.telecontrol.Classpath - Classpath entry is empty: C:\WarZone\spaces\nanocloud-getting-started\target\classes
    07:27:12.980 [ViNode[node3] init] WARN  o.g.vicluster.telecontrol.Classpath - Cannot copy URL content: file:/C:/WarZone/spaces/nanocloud-getting-started/target/classes/
    [node1] My name is '7176@ws4199'. Hello!
    [node2] My name is '8456@ws4199'. Hello!
    [node3] My name is '8044@ws4199'. Hello!
    [node4] My name is '8636@ws4199'. Hello!

As in previous example, you also may see some empty classpath warning as a result of tutorial project setup.

#### Step 3

````Java
    @Test
    public void test_hello_world__version3() throws InterruptedException {
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        
        cloud.nodes("node1", "node2", "node3", "node4");
        
        // let's make sure that all nodes are initialized
        // before saying 'hello' this time.
        // touch() will force nodes to be initialized.
        cloud.node("**").touch();
        
        // Console output is pulled asynchronously so we have to give it
        // few milliseconds to catch up.
        Thread.sleep(300);
        
        // Now we should see quite good chorus
        cloud.node("**").exec(new Callable<Void>() {
        
            @Override
            public Void call() throws Exception {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("My name is '" + jvmName + "'. Hello!");
                return null;
            }
        });     
    }
````

Now with "warm up" we could see something like this in console.
    
    07:30:31.018 [ViNode[node1] init] WARN  o.g.vicluster.telecontrol.Classpath - Classpath entry is empty: C:\WarZone\spaces\nanocloud-getting-started\target\classes
    07:30:31.025 [ViNode[node1] init] WARN  o.g.vicluster.telecontrol.Classpath - Cannot copy URL content: file:/C:/WarZone/spaces/nanocloud-getting-started/target/classes/
    [node2] My name is '7284@ws4199'. Hello!
    [node4] My name is '8512@ws4199'. Hello!
    [node1] My name is '7368@ws4199'. Hello!
    [node3] My name is '7872@ws4199'. Hello!


Lazy initialization is used very aggressively in core of [NanoCloud][nc], because it allows palatalization 
of time consuming task such as process stating or classpath replication via SSH.

Warm up like in example below allows you to benefit for parallel initialization and yet have you runnable 
executed almost synchronously (but given nature of distributed system 'almost' could be very lax).

#### Step 4, customizing JVM command line

One reason to spawn execution to separate JVM could be configuring that JVM in special way.

Here is how you can do it via NanoCloud API.

````Java
    @Test
    public void test_jvm_args__version1() throws InterruptedException {
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        
        // let's create a couple of node1
        cloud.node("node1");
        cloud.node("node2");
        
        // now let's adjust JVM command line options used to start slave process
        cloud.node("node1").x(VX.PROCESS).addJvmArg("-Xms256m").addJvmArg("-Xmx256m");
        cloud.node("node2").x(VX.PROCESS).addJvmArgs("-Xms512m", "-Xmx512m");
        
        cloud.node("**").touch();
        
        // Let's see how much memory is available to our slaves
        reportMemory(cloud);            
    }   
````    

You should see something like this in console:

    [node1] My name is '3756@ws4199'. Memory limit is 247MiB
    [node2] My name is '5668@ws4199'. Memory limit is 494MiB

As you can see two slave JVMs have different memory configuration. 

Going distributed
-----

### Configuring remote access
Let's try running remote processes now.

Open [StartingWithDistributedCloud.java][StartingWithDistributedCloud.java] to follow this chapter of tutorial.

Running remote processes via SSH requires a little configuration.

You need few Unix boxes, you have SSH access to.
To run examples from this tutorial you need:

 * server with SSH access (either password or private key will do)
 * Java 6 (or greater) installed on that server (no GCJ please, it is not a java)
 * that's it

In example, I'm using hostnames "cbox1" - "cbox4" which are mapped to real addresses in may local `hosts` file. 
You can follow this approach or change hostnames in tutorial.

You should also prepare `ssh-credentials.prop` file and drop it to your user's home.
Please read [ssh-credential setup guidelines][ssh-credential-setup].

See also [ssh-credentials.sample][ssh-credentials.sample] as example.

#### Step 1. Running remote nodes
Once configuration is finished with could run some code.

````Java
    @Test
    public void test_distributed_hello_world__basic_example() throws InterruptedException {
        
        // How cloud will create its nodes is defined by configuration.
        // Helper method below will create a preconfigured cloud,
        // where node name is interpreted by hostname.
        // If you do not have paswordless SSH to that node, 
        // additional credentials configuration may be required.
        cloud = CloudFactory.createCloud();
        RemoteNode.at(cloud.node("**")).useSimpleRemoting();
        
        // "cbox1" - "cbox3" are hostnames of VMs I'm using for testing.
        // You can either put FDQN names of your servers below
        // or use /etc/hosts to map these short names (as I do).
        cloud.node("cbox1");
        cloud.node("cbox2");
        cloud.node("cbox3");
        
        // Optionally you may want to specify java executable.
        // Default value is "java", so if java is on your PATH you do not need to do it.
        RemoteNodeProps.at(cloud.node("**")).setRemoteJavaExec("java");
        
        // now we have 3 nodes configured to run across two servers
        // let them say hello
        
        // cloud.node("**").touch() will force immediate intialization
        // of all nodes currently declared in cloud.
        // It is optional, but it makes console out put less messy
        cloud.node("**").touch();
        
        // Say hello world
        cloud.node("**").exec(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("My name is '" + jvmName + "'. Hello!");
                Thread.sleep(10000);
                return null;
            }
        });
        
        // Finally, we will give console output a chance to reach us from remote node.
        // Console communications are asynchronous, so them may be delayed by few miliseconds. 
        Thread.sleep(300);
    }
````

For my virtual cluster I could see following output:

    07:41:18.099 [ViNode[cbox3] init] WARN  o.g.vicluster.telecontrol.Classpath - Classpath entry is empty: C:\WarZone\spaces\nanocloud-getting-started\target\classes
    07:41:18.102 [ViNode[cbox3] init] WARN  o.g.vicluster.telecontrol.Classpath - Cannot copy URL content: file:/C:/WarZone/spaces/nanocloud-getting-started/target/classes/
    [cbox1] My name is '1939@cbox1'. Hello!
    [cbox3] My name is '1461@cbox3'. Hello!
    [cbox2] My name is '1586@cbox2'. Hello!

API call for remote execution is same as for local but behind of scene few more things are happening.

In particular:

 * Classpath of master (parent) JVM is replicated to remote host via SSH (and cached there)
 * RPC comunitation between master and slave JVM is tunneled via SSH TCP tunnel (to avoid possible firewall issues) 

#### Step 2. Mixing remote and embedded node
Now, we can run our code remotely. But imagine you want to debug your code. 
One option is to use remote debugging. But in many cases you could just run node you want to debug in master process thus avoiding these hurdles.

Let's modify previous example to run just one node in master JVM process.

````Java
    @Test
    public void test_distributed_hello_world__with_debug() throws InterruptedException {
        
        // Let's create a SSH cloud as in our first example
        cloud = CloudFactory.createCloud();
        RemoteNode.at(cloud.node("**")).useSimpleRemoting();
        
        cloud.node("cbox1");
        cloud.node("cbox2");
                        
        // But now, we would like to debug one of slave processes.
        // If it could be run on your desktop (no OS dependencies, etc),
        // you could easy redirect one of slaves to run inside of master JVM.
        // You can achieve this but using either in-process or isolate node type
        ViProps.at(cloud.node("cbox1")).setIsolateType();       
        
        ViNode allNodes = cloud.node("**");

        // warming up cluster as usual
        allNodes.touch();
        
        System.out.println("Master JVM name is '" + ManagementFactory.getRuntimeMXBean().getName() + "'");
        // you can set break point in runnable and catch cbox1.node1 executing it
        // other vi-nodes are running as separate processes, so they out of reach
        allNodes.exec(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("My name is '" + jvmName + "'. Hello!");
                return null;
            }
        });
        
        // You could notice, that output of "in-process" vi-node 
        // are still prefixed for your convenience.
        // Same isolation applies to system properties too. 
        
        // Quick wait to catch up with console
        Thread.sleep(300);
    }   
````

Output would contain something like this

    Installing java.lang.System multiplexor
    07:43:33.938 [ViNode[cbox2] init] WARN  o.g.vicluster.telecontrol.Classpath - Classpath entry is empty: C:\WarZone\spaces\nanocloud-getting-started\target\classes
    07:43:33.941 [ViNode[cbox2] init] WARN  o.g.vicluster.telecontrol.Classpath - Cannot copy URL content: file:/C:/WarZone/spaces/nanocloud-getting-started/target/classes/
    Master JVM name is '8800@ws4199'
    [cbox1] My name is '8800@ws4199'. Hello!
    [cbox2] My name is '1698@cbox2'. Hello!

You can see that "cbox1" actually were running locally in a JVM of master process. So you cloud debug it freely.
Output of "cbox1" is still prefixed. 

All threads of "cbox1" are also running in dedicated thread group, 
so it could be forcefully killed (it is impossible to reliably kill thread in JVM, but we are doing our best).

 [nc]: https://github.com/gridkit/nanocloud
 [vinode]: https://github.com/gridkit/nanocloud/blob/vicluster-0.8/vicluster-core/src/main/java/org/gridkit/vicluster/ViNode.java
 [ssh-credential-setup]: NanoCloud_Configuring_SSH_credentials.md
 [StartingWithLocalCloud.java]: https://github.com/gridkit/nanocloud-getting-started/blob/master/src/test/java/org/gridkit/lab/examples/nanocloud/StartingWithLocalCloud.java
 [StartingWithDistributedCloud.java]: https://github.com/gridkit/nanocloud-getting-started/blob/master/src/test/java/org/gridkit/lab/examples/nanocloud/StartingWithDistributedCloud.java
 [BasicViNodeUsage.java]: https://github.com/gridkit/nanocloud-getting-started/blob/master/src/test/java/org/gridkit/lab/examples/nanocloud/BasicViNodeUsage.java
 [TransaprentRmi.java]: https://github.com/gridkit/nanocloud-getting-started/blob/master/src/test/java/org/gridkit/lab/examples/nanocloud/TransaprentRmi.java
 [ssh-credentials.sample]: https://github.com/gridkit/nanocloud-getting-started/blob/master/src/test/resources/ssh-credentials.sample