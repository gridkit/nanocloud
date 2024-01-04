package org.gridkit.nanocloud.viengine;

import java.util.List;
import java.util.concurrent.ExecutionException;

class InitConnectorAction extends AbstractNodeAction {

    public static final InitConnectorAction INSTANCE = new InitConnectorAction();

    private InArg<String> targetUrl = required("remote:target-url");

    @Override
    protected void run() throws ExecutionException {
        String targetUrl = transform(this.targetUrl.get());
        List<NodeAction> providers = getContext().collect(Pragma.REMOTE_PROTOCOL + "**", NodeAction.class);
        for (NodeAction provider: providers) {
            provider.run(getContext());
            if (getContext().get(Pragma.RUNTIME_HOST_CONNECTOR) != null) {
                break;
            }
        }
        if (getContext().get(Pragma.RUNTIME_HOST_CONNECTOR) == null) {
            BootAnnotation.fatal(getContext(), "No connector found for URL '%s'", targetUrl);
        }
    }
}
