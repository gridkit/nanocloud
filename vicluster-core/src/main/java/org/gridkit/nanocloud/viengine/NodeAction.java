package org.gridkit.nanocloud.viengine;

import java.util.concurrent.ExecutionException;

public interface NodeAction {

    public void run(PragmaWriter context) throws ExecutionException;

}
