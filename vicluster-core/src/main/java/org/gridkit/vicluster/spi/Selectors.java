package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Selectors {
	
	public static Selector matchAll(Iterable<Map.Entry<String, Object>> attrList) {
		List<Selector> selectors = new ArrayList<Selector>();
		for(Map.Entry<String, Object> a: attrList) {
			selectors.add(is(a.getKey(), a.getValue()));
		}
		return allOf(selectors.toArray(new Selector[selectors.size()]));
	}
	
	public static Selector id(String id) {
		return is(AttrBag.ID, id);
	}

	public static Selector name(String name, String type) {
		return allOf(is(AttrBag.NAME, name), is(AttrBag.TYPE, type));
	}

	public static Selector is(String name, Object value) {
		return new EqualsLast(name, value);
	}

	public static Selector has(String name, Object value) {
		return new EqualsAny(name, value);
	}

	public static Selector isNotSet(String name) {
		return new NotSet(name);
	}

	public static Selector isDefined(String name) {
		return not(isNotSet(name));
	}

	public static Selector not(Selector s) {
		return new Not(s);
	}

	public static Selector allOf(Collection<Selector> s) {
		return allOf(s.toArray(new Selector[s.size()]));
	}

	public static Selector allOf(Selector... s) {
		return new And(s);
	}

	public static Selector anyOf(Collection<Selector> s) {
		return allOf(s.toArray(new Selector[s.size()]));
	}
	
	public static Selector anyOf(Selector... s) {
		return new Or(s);
	}

	public static Selector match(String name, String regex) {
		return new RegexLast(name, Pattern.compile(regex));
	}
	
	private static class Not implements Selector {
		
		private Selector nested;

		public Not(Selector nested) {
			super();
			this.nested = nested;
		}

		@Override
		public boolean match(AttrBag bag) {
			return !nested.match(bag);
		}
	}
	
	private static class And implements Selector {
		
		private Selector[] nested;
		
		public And(Selector[] nested) {
			this.nested = nested;
		}
		
		@Override
		public boolean match(AttrBag bag) {
			for(Selector n: nested) {
				if (!n.match(bag)) {
					return false;
				}
			}
			return true;
		}
	}

	private static class Or implements Selector {
		
		private Selector[] nested;
		
		public Or(Selector[] nested) {
			this.nested = nested;
		}
		
		@Override
		public boolean match(AttrBag bag) {
			for(Selector n: nested) {
				if (n.match(bag)) {
					return true;
				}
			}
			return false;
		}
	}
	
	private static class RegexLast extends MatchLast {
		
		private Pattern pattern;

		public RegexLast(String attrName, Pattern pattern) {
			super(attrName);
			this.pattern = pattern;
		}

		@Override
		protected boolean match(Object v) {
			if (v instanceof String) {
				return pattern.matcher((String)v).matches();
			}
			else {
				return false;
			}
		}
	}
	
	private static class EqualsLast extends MatchLast {
		
		private Object value;
		
		public EqualsLast(String attrName, Object value) {
			super(attrName);
			this.value = value;
		}
		
		@Override
		protected boolean match(Object v) {
			return value.equals(v);
		}
	}

	private static class EqualsAny extends MatchAny {
		
		private Object value;
		
		public EqualsAny(String attrName, Object value) {
			super(attrName);
			this.value = value;
		}
		
		@Override
		protected boolean match(Object v) {
			return value.equals(v);
		}
	}

	private static class NotSet extends MatchLast {
		
		public NotSet(String attrName) {
			super(attrName);
		}
		
		@Override
		protected boolean match(Object v) {
			return v == null;
		}
	}
	
	private static abstract class AttrSelector implements Selector {
		
		protected String attrName;
		
		public AttrSelector(String attrName) {
			this.attrName = attrName;
		}
	}
	
	private static abstract class MatchAny extends AttrSelector {

		public MatchAny(String attrName) {
			super(attrName);
		}

		protected abstract boolean match(Object v);
		
		@Override
		public boolean match(AttrBag bag) {
			for(Object v : bag.getAll(attrName)) {
				if (match(v)) {
					return true;
				}
			}			
			return false;
		}		
	}

	private static abstract class MatchLast extends AttrSelector {
		
		public MatchLast(String attrName) {
			super(attrName);
		}
		protected abstract boolean match(Object v);
		
		@Override
		public boolean match(AttrBag bag) {
			return match(bag.getLast(attrName));
		}		
	}
}
