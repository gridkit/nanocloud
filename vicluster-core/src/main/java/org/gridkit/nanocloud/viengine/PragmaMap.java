package org.gridkit.nanocloud.viengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PragmaMap implements PragmaReader, PragmaWriter, Cloneable {

    private Set<String> keys = new LinkedHashSet<String>();
    
    private Map<String, String> links = new HashMap<String, String>();
    private Map<String, Object> values = new HashMap<String, Object>();
    
    private List<String> defaultWildCards = new ArrayList<String>();
    
    public PragmaMap() {        
    }
    
    public PragmaMap clone() {
        try {
            PragmaMap that = (PragmaMap) super.clone();
            that.keys = new LinkedHashSet<String>(keys);
            that.links = new HashMap<String, String>(links);
            that.values = new HashMap<String, Object>(values);
            that.defaultWildCards = new ArrayList<String>(defaultWildCards);
            return that;
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    @Override
    public void copyTo(PragmaWriter target) {
        copyTo(target, false);
    }
    
    @Override
    public void copyTo(PragmaWriter target, boolean omitExisting) {
        for(String key: keys) {
            if (omitExisting && target.isPresent(key)) {
                continue;
            }
            if (values.containsKey(key)) {
                target.set(key, values.get(key));
            }
            else if (links.containsKey(key)) {
                target.link(key, links.get(key));
            }
            else {
                String lazyKey = lazyKey(key);
                if (values.get(lazyKey) != null) {
                    if (!target.isPresent(lazyKey) || !omitExisting) {
                        target.setLazy(key, (LazyPragma)values.get(lazyKey));
                    }
                }
                else {
                    throw new RuntimeException("Unknown key: " + key);
                }
            }
        }
    }
    
    public void set(String key, Object value) {
        if (key == null) {
            throw new NullPointerException("'key' should not be null");
        }
        addKey(key);
        links.remove(key);
        values.put(key, value);
    }

    public void setLazy(String key, LazyPragma lazy) {
        if (key == null) {
            throw new NullPointerException("'key' should not be null");
        }
        if (lazy == null) {
            throw new NullPointerException("'lazy' should not be null");
        }
        addKey(key);
        set(lazyKey(key), lazy);
    }

    public void link(String key, String link) {
        if (key == null) {
            throw new NullPointerException("'key' should not be null");
        }
        if (link == null) {
            throw new NullPointerException("'link' should not be null");
        }
        addKey(key);
        values.remove(key);
        links.put(key, link);        
    }

    @Override
    public boolean isPresent(String key) {
        if (key == null) {
            throw new NullPointerException("'key' should not be null");
        }
        if (!keys.contains(key)) {
            return false;
        }
        else {
            if (values.containsKey(key)) {
                return values.get(key) != null;
            }
            else if (links.containsKey(key)) {
                String link = links.get(key);
                link = tryLinkKey(link);
                pushLink(link);
                try {
                    return isPresent(link);
                }
                finally{
                    popLink(link);
                }
            }
            else {
                // lazy value is assumed non-null
                return true;
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (key == null) {
            throw new NullPointerException("'key' should not be null");
        }
        if (key.indexOf('?') >= 0 || key.indexOf('*') >= 0) {
            throw new IllegalArgumentException("Wild cards is not supported: " + key);
        }
        Object value = getValue(key);
        if (value != null) {
            return (T)value;
        }
        LazyPragma lazy = getFactory(key);
        if (lazy != null) {
            if (!keys.contains(key)) {
                keys.add(key);
            }
            value = lazy.resolve(key, this);
            values.put(key, value);
            return (T)value;
        }
        String defaultKey = Pragma.DEFAULT + key;
        value = getValue(defaultKey);
        if (value != null) {
            return (T)value;
        }
        for(String dwc: defaultWildCards) {
            Pattern re = GlobHelper.translate(dwc, ".:");
            if (re.matcher(defaultKey).matches()) {
                value = getValue(dwc);
                if (value != null) {
                    return (T)value;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T getFactory(String origkey) {
        if (origkey == null) {
            throw new NullPointerException("'key' should not be null");
        }
        String key = Pragma.LAZY + origkey;
        Object value = getValue(key);
        if (value != null) {
            return (T)value;
        }
        String defaultKey = Pragma.DEFAULT + key;
        value = getValue(defaultKey);
        if (value != null) {
            return (T)value;
        }
        for(String dwc: defaultWildCards) {
            Pattern re = GlobHelper.translate(dwc, ".:");
            if (re.matcher(defaultKey).matches()) {
                value = getValue(dwc);
                if (value != null) {
                    return (T)value;
                }
            }
        }
        return null;
    }

    @Override
    public List<String> match(String glob) {
        List<String> mkeys = new ArrayList<String>();
        Pattern re = GlobHelper.translate(glob, ":.");
        for(String key: keys) {
            if (!key.startsWith(Pragma.DEFAULT)) {
                if (re.matcher(key).matches() && isPresent(key)) {
                    mkeys.add(key);
                }
            }
        }
        return mkeys;
    }

    @Override
    public String describe(String key) {
        if (!keys.contains(key)) {
            return null;
        }
        else {
            if (getFactory(key) != null) {
                return "{" + getFactory(key) + "} " + String.valueOf(values.get(key)); 
            }
            else if (values.containsKey(key)) {
                return String.valueOf(values.get(key));
            }
            else if (links.containsKey(key)) {
                String link = links.get(key);
                link = tryLinkKey(link);
                pushLink(link);
                try {
                    return "{-> " + link + "} " + describe(link);
                }
                finally{
                    popLink(link);
                }
            }
            else {
                return null;
            }
        }
    }

    private Object getValue(String key) {
        if (!keys.contains(key)) {
            return null;
        }
        else {
            if (values.containsKey(key)) {
                return values.get(key);
            }
            else if (links.containsKey(key)) {
                String link = links.get(key);
                String linkKey = linkKey(link);
                pushLink(linkKey);
                try {
                    Object value = get(linkKey);
                    if (!link.startsWith("?") && value == null) {
                        throw new IllegalArgumentException("Link has no value '" + linkKey + "' at key '" + key + "'");
                    }
                    return value;
                }
                finally{
                    popLink(linkKey);
                }
            }
            else {                
                return null;
            }
        }
    }

    private void addKey(String key) {
        if (!keys.contains(key)) {
            keys.add(key);
        }
        if (key.indexOf('*') >= 0 || key.indexOf('?') >= 0) {
            if (key.startsWith(Pragma.DEFAULT)) {
                defaultWildCards.remove(key);
                defaultWildCards.add(0, key);
            }
            else {
                throw new IllegalArgumentException("Wild cards are not allowed: " + key);
            }
        }
    }
    
    private String lazyKey(String key) {
        if (key.startsWith(Pragma.DEFAULT)) {
            return Pragma.DEFAULT + Pragma.LAZY + key.substring(Pragma.DEFAULT.length());
        }
        else {
            return Pragma.LAZY + key;
        }
    }
    
    private static final Pattern NAME_REF = Pattern.compile("\\$\\{([^\\}]+)\\}");
    
    private String tryLinkKey(String link) {
        try {
            return linkKey(link);
        }
        catch(UnresolvableLinkException e) {
            return "";
        }
    }
    
    private String linkKey(String link) {
        String key = link;
        if (link.startsWith("?")) {
            key = link.substring(1);
        }
        int n = 0;
        while(true) {
            Matcher m = NAME_REF.matcher(key);
            if (m.find(n)) {
                String p = m.group();
                String rkey = Pragma.PROP + m.group(1);
                Object v = get(rkey);
                if (v == null) {
                    if (!link.startsWith("?")) {
                        throw new UnresolvableLinkException("Cannot resolve link " + link);
                    }
                    n = m.end();
                    continue;
                }             
                key = key.replace(p, String.valueOf(v));
                n = 0;
                continue;
            }            
            else {
                break;
            }
        }
        return key;
    }

    private static ThreadLocal<Set<String>> LINK_STACK = new ThreadLocal<Set<String>>();
    
    private void pushLink(String link) {
        Set<String> stack = LINK_STACK.get();
        if (stack == null) {
            stack = new LinkedHashSet<String>();
            LINK_STACK.set(stack);
        }
        if (stack.contains(link)) {
            throw new RuntimeException("Cyclic link [" + link + "] + " + stack.toString());
        }
        stack.add(link);
    }
    
    private void popLink(String link) {
        Set<String> stack = LINK_STACK.get();
        if (stack != null) {
            stack.remove(link);
        }        
    }
    
//    private Set<String> swapLinkStack(Set<String> stack) {
//        Set<String> copy = LINK_STACK.get();
//        LINK_STACK.set(stack);
//        return copy;
//    }
    
    private static class UnresolvableLinkException extends IllegalArgumentException {

        private static final long serialVersionUID = 20140619L;

        public UnresolvableLinkException(String s) {
            super(s);
        }
    }
}
