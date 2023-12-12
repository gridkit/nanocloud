package org.gridkit.nanocloud.viengine;

/**
 * {@link NodeTrigger} allows to hook action to event of certain property being set on {@link PragmaMap}.
 * Trigger is removed after invocation.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface NodeTrigger {

    public static final KeyMatcher ALL = new KeyMatcher() {

        @Override
        public boolean evaluate(String key) {
            return true;
        }
    };

    public KeyMatcher keyMatcher();

    public boolean evaluate(PragmaWriter context);

    public interface KeyMatcher {

        boolean evaluate(String key);

    }

    public class SinglePropMatcher implements KeyMatcher {

        private final String propName;

        public SinglePropMatcher(String propName) {
            this.propName = propName;
        }

        @Override
        public boolean evaluate(String key) {
            return propName.equals(key);
        };
    }
}
