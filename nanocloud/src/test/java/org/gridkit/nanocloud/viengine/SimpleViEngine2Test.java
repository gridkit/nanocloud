package org.gridkit.nanocloud.viengine;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.Test;

public class SimpleViEngine2Test {

    @Test
    public void helo_node_local() {
        Cloud cloud = Engine2.createCloud();
        
        ViNode node = cloud.node("a");
        node.x(VX.TYPE).setLocal();
        
        node.exec(new Runnable() {
            
            @Override
            public void run() {
                System.out.println("Helo from " + System.getProperty("vinode.name"));
            }
        }); 
        
        cloud.shutdown();
    }
    
}
