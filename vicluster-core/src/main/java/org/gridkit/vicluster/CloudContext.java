package org.gridkit.vicluster;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;

public interface CloudContext {

    public <T> T lookup(ServiceKey<T> key);

    public <T> T lookup(ServiceKey<T> key, Callable<T> provider);

    public <T> T lookup(ServiceKey<T> key, ServiceProvider<T> provider);

    public void addFinalizer(Runnable finalizer);

    public static interface ServiceProvider<T> {

        public T getService(CloudContext context);

    }

    public static class ServiceKey<T> {

        private Class<T> type;
        private java.util.Map<String, String> props = new LinkedHashMap<String, String>();

        public ServiceKey(Class<T> type) {
            this(type, Collections.<String, String>emptyMap());
        }

        public ServiceKey(Class<T> type, java.util.Map<String, String> keyProps) {
            this.type = type;
            props.putAll(keyProps);
        }

        public java.util.Map<String, String> asComparableMap() {
            return new TreeMap<String, String> (props);
        }

        public Class<T> getType() {
            return type;
        }

        public ServiceKey<T> with(String key, String value) {
            ServiceKey<T> that = new ServiceKey<T>(type, props);
            that.props.put(key, value);
            return that;
        }

        @Override
        public int hashCode() {
            return asComparableMap().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ServiceKey) {
                return asComparableMap().equals(((ServiceKey<?>) obj).asComparableMap());
            }
            else {
                return false;
            }
        }

        public List<Class<?>> getClassHierary() {
            List<Class<?>> h = new ArrayList<Class<?>>();
            collectHierarchy(h, type);
            return h;
        }

        private void collectHierarchy(List<Class<?>> h, Class<?> t) {
            if (!h.contains(t)) {
                h.add(t);
            }
            if (t.getInterfaces() != null) {
                for(Class<?> i: t.getInterfaces()) {
                    collectHierarchy(h, i);
                }
            }
            if (t != Object.class && t.getSuperclass() != null) {
                collectHierarchy(h, t.getSuperclass());
            }
        }

        @Override
		public String toString() {
            return type.getSimpleName() + props.toString();
        }
    }

    public static class Helper {

        public static <T> ServiceKey<T> key(Class<T> type) {
            return new ServiceKey<T>(type);
        }

        public static <T> ServiceKey<T> key(Class<T> type, String propName, String value) {
            return new ServiceKey<T>(type, Collections.singletonMap(propName, value));
        }


        public static <T> ServiceProvider<T> reflectionProvider(final Class<? extends T> type, final String finalizerMethod) {
            try {
                if (finalizerMethod != null) {
                    // verify finalizer
                    type.getMethod(finalizerMethod);
                }
                Constructor<?> c = type.getConstructor();
                if (!Modifier.isPublic(c.getModifiers())) {
                    throw new RuntimeException("Class " + type.getName() + " does not have public no argument constructor");
                }
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            ServiceProvider<T> provider = new ServiceProvider<T>() {

                @SuppressWarnings("deprecation")
                @Override
                public T getService(CloudContext context) {
                    try {
                        T service = (T)type.newInstance();
                        if (finalizerMethod != null) {
                            context.addFinalizer(reflectionFinalizer(service, finalizerMethod));
                        }
                        return service;
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            };


            return provider;
        }

        public static Runnable closeableFinalizer(final Closeable obj) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        obj.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            };
        }

        public static Runnable reflectionFinalizer(final Object obj, String finalizerMethod) {
            try {
                final Method m = obj.getClass().getMethod(finalizerMethod);
                m.setAccessible(true);
                return new Runnable() {
                    @Override
                    public void run() {
                        try {
                            m.invoke(obj);
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            if (e.getCause() instanceof RuntimeException) {
                                throw (RuntimeException)e.getCause();
                            }
                            else if (e.getCause() instanceof Error) {
                                throw (Error)e.getCause();
                            }
                            else {
                                throw new RuntimeException(e.getCause());
                            }
                        }
                    }

                    @Override
                    public String toString() {
                        return "FIN[" + m.getName() + "@" + obj.toString() + "]";
                    }
                };
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
