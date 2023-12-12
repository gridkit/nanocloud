/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster.isolate;

import java.net.MalformedURLException;
import java.net.URL;

import org.gridkit.vicluster.ViConfigurable;
import org.gridkit.vicluster.ViNodeProps;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class IsolateProps implements ViNodeProps {

    public static String PREFIX = "isolate:";

    public static String NAME = "isolate:name";

    /** Use for packages to be isolate */
    public static String PACKAGE = "isolate:package:";

    /** Use for classes to be delegated to parent classloader */
    public static String SHARED = "isolate:shared:";

    /** Use for adding additional URLs to classpath */
    public static String CP_INCLUDE = "isolate:cp-include:";

    /** Use for prohibiting URLs in classpath */
    public static String CP_EXCLUDE = "isolate:cp-exclude:";

    public static String ISOLATE_PACKAGE = "isolate:isolate-package:";

    public static String SHARE_PACKAGE = "isolate:share-package:";

    public static String ISOLATE_CLASS = "isolate:isolate-class:";

    public static String SHARE_CLASS = "isolate:share-class:";

    public static String ISOLATE_URL = "isolate:isolate-url:";

    public static String SHARE_URL = "isolate:share-url:";

    public static IsolateProps at(ViConfigurable config) {
        return new IsolateProps(config);
    }

    protected final ViConfigurable config;

    protected IsolateProps(ViConfigurable config) {
        this.config = config;
    }

    public IsolateProps isolatePackage(String pack) {
        config.setProp(ISOLATE_PACKAGE + pack, "");
        return this;
    }

    public IsolateProps shareAll() {
        config.setProp(SHARE_PACKAGE + "", "");
        return this;
    }

    public IsolateProps sharePackage(String pack) {
        config.setProp(SHARE_PACKAGE + pack, "");
        return this;
    }

    public IsolateProps isolateClass(String className) {
        config.setProp(ISOLATE_CLASS + className, "");
        return this;
    }

    public IsolateProps shareClass(String className) {
        config.setProp(SHARE_CLASS + className, "");
        return this;
    }

    public IsolateProps isolateClass(Class<?> c) {
        while(c.getDeclaringClass() != null) {
            c = c.getDeclaringClass();
        }
        isolateClass(c.getName());
        return this;
    }

    public IsolateProps shareClass(Class<?> c) {
        while(c.getDeclaringClass() != null) {
            c = c.getDeclaringClass();
        }
        shareClass(c.getName());
        return this;
    }

    public IsolateProps isolateUrl(String url) {
        config.setProp(ISOLATE_URL + url, "");
        return this;
    }

    public IsolateProps isolateSource(String resource) {
        isolateUrl(toRootUrl(resource));
        return this;
    }

    public IsolateProps shareSource(String resource) {
        shareUrl(toRootUrl(resource));
        return this;
    }

    public IsolateProps isolateSource(Class<?> c) {
        isolateUrl(toRootUrl(c));
        return this;
    }

    public IsolateProps shareSource(Class<?> c) {
        shareUrl(toRootUrl(c));
        return this;
    }

    public IsolateProps shareUrl(String url) {
        config.setProp(SHARE_URL + url, "");
        return this;
    }

    public static String toRootUrl(String resource) {
        URL path = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (path == null) {
            throw new IllegalArgumentException("Cannot locate resource on classpath '" + resource + "'");
        }
        String sp = path.toString();
        try {
            if (sp.endsWith(resource)) {
                if (resource.startsWith("/")) {
                    resource = resource.substring(1);
                }
                return new URL(sp.substring(0, sp.length() - resource.length())).toString();
            }
        } catch (MalformedURLException e) {
        }
        throw new IllegalArgumentException("Cannot find root. URL: " + path + " Relative path: " + resource);
    }

    public static String toRootUrl(Class<?> c) {
        while(c.getDeclaringClass() != null) {
            c = c.getDeclaringClass();
        }
        return toRootUrl(c.getName().replace('.', '/') + ".class");
    }
}
