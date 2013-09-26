package org.gridkit.vicluster;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for config dictionaries.
 * Dictionaries should be defined in this package, to limit visibility of support classes.  
 */
abstract class GenericConfig {
	
	static ValueParser<Integer> INT_PARSER = new IntegerParser();
	static ValueParser<Long> LONG_PARSER = new LongParser();
	static ValueParser<String> STRING_PARSER = new StringParser();
	static ValueParser<Boolean> BOOLEAN_PARSER = new BooleanParser();
	static ValueParser<Long> TIME_INTERVAL_PARSER = new TimeIntervalParser();

	abstract Object readRawProp(String name);

	abstract void setRawProp(String name, Object value);
	
	boolean readBoolean() {
		return readProp(BOOLEAN_PARSER);
	}

	int readInt() {
		return readProp(INT_PARSER);
	}

	long readLong() {
		return readProp(LONG_PARSER);
	}

	String readString() {
		return readProp(STRING_PARSER);
	}

	long readTimeInterval() {
		return readProp(TIME_INTERVAL_PARSER);
	}
	
	@SuppressWarnings("unchecked")
	<T> T readObject() {
		return (T)readRawProp(getAssociatedPropName());
	}
	
	<T> T readProp(ValueParser<T> parser) {
		String propName = getAssociatedPropName();
		String defVal = getAssociatedDefaultValue();
		Map[] mapping = getAssociatedMapping();
		String value = (String) readRawProp(propName);
		if (value != null) {
			try {
				String mappedVal = mapPropValue(value, mapping);
				T result = parser.parse(mappedVal);
				return result;
			}
			catch(RuntimeException e) {
				throw new IllegalArgumentException("Failed to read property \"" + propName + "\", cannot parse '" + value + "'", e);				
			}
		}
		else {
			if (defVal == null) {
				return null;
//				raiseMissingProp(propName);
//				throw new Error("Unreachable");
			}
			else {
				try {
					String mappedVal = mapPropValue(defVal, mapping);
					T result = parser.parse(mappedVal);
					return result; 
				}
				catch(RuntimeException e) {
					throw new IllegalArgumentException("Failed to read property \"" + propName + "\", cannot parse '" + defVal + "'", e);				
				}
			}
		}
	}
	
	void setProp(Object value) {
		setRawProp(getAssociatedPropName(), value);
	}

	void setProp(String suffix, Object value) {
		setRawProp(getAssociatedPropName() + suffix, value);
	}
	
	private static String mapPropValue(String val, Map[] mapping) {
		for(Map m: mapping) {
			if (m.caseSensitive()) {
				if (m.from().equals(val)) {
					return m.to(); 
				}
			}
			else {
				if (m.from().equalsIgnoreCase(val)) {
					return m.to();
				}
			}
		}
		return val;
	}

//	private static void raiseMissingProp(String propName) {
//		throw new IllegalArgumentException("Configuration property is not set: " + propName);	
//	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	String getAssociatedPropName() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for (int i = 1; i != stack.length; ++i) {
			try {
				Class cls = this.getClass().getClassLoader().loadClass(stack[i].getClassName());
				Method m = cls.getMethod(stack[i].getMethodName());
				PropName config = m.getAnnotation(PropName.class);
				return config.value();
			} catch (Exception e) {
				continue;
			}
		}
		throw new IllegalArgumentException("Cannot find @PropName annotation at call stack");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	String getAssociatedDefaultValue() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for (int i = 1; i != stack.length; ++i) {
			try {
				Class cls = this.getClass().getClassLoader().loadClass(stack[i].getClassName());
				Method m = cls.getMethod(stack[i].getMethodName());
				PropName config = m.getAnnotation(PropName.class);
				if (config != null) {
					try {
						Default defaultValue = m.getAnnotation(Default.class);
						if (defaultValue != null) {
							return defaultValue.value();
						}
						DefaultNull nullDefault = m.getAnnotation(DefaultNull.class);
						if (nullDefault != null) {
							return null;
						}
						
						return null;
					}
					catch(Exception e) {
						return null;
					}
				}
			} catch (Exception e) {
				continue;
			}
		}
		throw new IllegalArgumentException("Cannot find @PropName annotation at call stack");		
	}	
	
	private static Map[] EMPTY_MAPPING = new Map[0];
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map[] getAssociatedMapping() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for (int i = 1; i != stack.length; ++i) {
			try {
				Class cls = Class.forName(stack[i].getClassName());
				Method m = cls.getMethod(stack[i].getMethodName());
				PropName config = m.getAnnotation(PropName.class);
				if (config != null) {
					try {
						Mapping mapping = m.getAnnotation(Mapping.class);
						return mapping.value();
					}
					catch(Exception e) {
						return EMPTY_MAPPING;
					}
				}
			} catch (Exception e) {
				continue;
			}
		}
		throw new IllegalArgumentException("Cannot find @PropName annotation at call stack");				
	}
	
	interface ValueParser<T> {
		public T parse(String text);		
	}
	
	private static class BooleanParser implements ValueParser<Boolean> {
		@Override
		public Boolean parse(String text) {
			return Boolean.valueOf(text);
		}
	}

	private static class IntegerParser implements ValueParser<Integer> {
		@Override
		public Integer parse(String text) {
			return Integer.valueOf(text);
		}
	}

	private static class LongParser implements ValueParser<Long> {
		@Override
		public Long parse(String text) {
			return Long.valueOf(text);
		}
	}

	private static class StringParser implements ValueParser<String> {
		@Override
		public String parse(String text) {
			return text;
		}
	}

	private static class TimeIntervalParser implements ValueParser<Long> {
		@Override
		public Long parse(String text) {
			return toMillis(text);
		}
	}
	
	private static final java.util.Map<String, TimeUnit> timeUnitAlias = new HashMap<String, TimeUnit>();
    
    static {
        timeUnitAlias.put("ms", TimeUnit.MILLISECONDS);
        timeUnitAlias.put("s",  TimeUnit.SECONDS);
        timeUnitAlias.put("m",  TimeUnit.MINUTES);
        timeUnitAlias.put("h",  TimeUnit.HOURS);
        timeUnitAlias.put("d",  TimeUnit.DAYS);
    }
    
    private static final String singleDurationRegex = "(\\d+)\\s*(\\w+)";
    private static final Pattern singleDurationPattern = Pattern.compile(singleDurationRegex);
    
    private static int DURATION_GROUP = 1;
    private static int TIME_UNIT_GROUP = 2;
    
    private static final String multipleDurationRegex = String.format("(%s\\s+)+", singleDurationRegex);
    private static final Pattern multipleDurationPattern = Pattern.compile(multipleDurationRegex);
    
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    
    private static long toMillis(String rawStr) {
        if (rawStr == null)
            throw new NullPointerException("Null argument is not allowed");
        
        String str = rawStr.trim() + " ";
        Matcher matcher = multipleDurationPattern.matcher(str);
        
        if (!matcher.matches())
            throw new IllegalArgumentException(String.format("'%s' doesn't match duration pattern", rawStr));
        
        matcher = singleDurationPattern.matcher(str);
        
        BigInteger result = BigInteger.ZERO;
        
        while (matcher.find()) {
            String unitAlias = matcher.group(TIME_UNIT_GROUP).toLowerCase();
            TimeUnit timeUnit = timeUnitAlias.get(unitAlias);
            
            if (timeUnit == null) {
                throw new IllegalArgumentException(String.format("Unknown time unit alias '%s' in '%s'", unitAlias, rawStr));
            }
            
            long summand;
            try {
                summand = Long.valueOf(matcher.group(DURATION_GROUP));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("Number overflow for duration '%s'", rawStr), e);
            }
            
            result = result.add(BigInteger.valueOf(timeUnit.toMillis(summand)));
        }
        
        if (result.compareTo(LONG_MAX) == 1)
            throw new IllegalArgumentException(String.format("Number overflow for duration '%s'", rawStr));
        
        return result.longValue();
    }    
}

/**
 * Default value for property
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@interface Default {

	public String value();
	
}

/**
 * Default value for property is <code>null</code>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@interface DefaultNull {	
}

/**
 * Property name
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@interface PropName {

	public String value();
	
}

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Mapping {

	Map[] value();
	
}

@interface Map {
	String from();
	String to();
	boolean caseSensitive() default false;
}
