package org.gridkit.nanocloud.viengine;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class InitConnectorAction extends AbstractNodeAction {

    public static final InitConnectorAction INSTANCE = new InitConnectorAction();

    @SuppressWarnings("unused")
    private InArg<String> targetUrl = required("remote:target-url");

    @Override
    protected void run() throws ExecutionException {
        List<NodeAction> providers = getContext().collect(Pragma.REMOTE_PROTOCOL + "**", NodeAction.class);
        for (NodeAction provider: providers) {
            provider.run(getContext());
            if (getContext().get(Pragma.RUNTIME_HOST_CONNECTOR) != null) {
                break;
            }
        }
    }
}
