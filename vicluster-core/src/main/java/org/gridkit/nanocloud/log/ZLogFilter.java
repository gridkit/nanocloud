package org.gridkit.nanocloud.log;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.gridkit.zerormi.zlog.LogLevel;
import org.gridkit.zerormi.zlog.LogStream;
import org.gridkit.zerormi.zlog.ZLogger;

public class ZLogFilter implements ConfigurableZLogger {

	private ZLogger root;
	private Map<String, LogLevel> filter = new TreeMap<String, LogLevel>();
	private volatile Map<String, LogLevel> cacheFilter = new HashMap<String, LogLevel>();

	public ZLogFilter(ZLogger root, LogLevel level) {
		this.root = new FilteredZLogger(root, "");
		this.filter.put("", level);
	}
	
	
	
	public ZLogger getLogger(String path) {
		return root.getLogger(path);
	}



	public LogStream get(String path, LogLevel level) {
		return root.get(path, level);
	}



	public LogStream fatal() {
		return root.fatal();
	}



	public LogStream critical() {
		return root.critical();
	}



	public LogStream warn() {
		return root.warn();
	}



	public LogStream info() {
		return root.info();
	}



	public LogStream verbose() {
		return root.verbose();
	}



	public LogStream debug() {
		return root.debug();
	}



	public LogStream trace() {
		return root.trace();
	}



	@Override
	public synchronized void setLevel(String path, LogLevel level) {
		if (level == null) {
			if (path.length() == 0) {
				throw new IllegalArgumentException("Cannot unset default log level");
			}
			filter.remove(path);
		}
		else {
			filter.put(path, level);
		}
		cacheFilter = new HashMap<String, LogLevel>();
	}
	
	protected LogLevel getLevel(String path) {
		Map<String, LogLevel> cf = cacheFilter;
		LogLevel ll = cf.get(path);
		if (ll == null) {
			synchronized(this) {
				String p = path;
				while(ll == null) {
					ll = filter.get(p);
					p = parent(p);
				}
				cf = new HashMap<String, LogLevel>(cacheFilter);
				cf.put(path, ll);
				cacheFilter = cf;
			}
		}
		return ll;
	}

	private String parent(String p) {
		if (p.length() == 0) {
			throw new IllegalArgumentException();
		}
		int n = p.lastIndexOf('.');
		return n < 0 ? "" : p.substring(0, n);
	}
	
	private class FilteredZLogger implements ZLogger {
		
		private ZLogger logger;
		private String path;
		
		public FilteredZLogger(ZLogger logger, String path) {
			this.logger = logger;
			this.path = path;
		}

		@Override
		public ZLogger getLogger(String path) {
			String ln = this.path;
			if (path != null && path.length() > 0) {
				ln += (ln.length() == 0 ? "" : ".") + path;
			}
			return new FilteredZLogger(logger, ln);
		}

		@Override
		public LogStream get(String path, LogLevel level) {
			level.toString();
			String ln = this.path;
			if (path != null && path.length() > 0) {
				ln += (ln.length() == 0 ? "" : ".") + path;
			}
			return new FilteredLogStream(logger.get(path, level), ln, level);
		}

		@Override
		public LogStream fatal() {
			return new FilteredLogStream(logger.fatal(), path, LogLevel.FATAL);
		}

		@Override
		public LogStream critical() {
			return new FilteredLogStream(logger.critical(), path, LogLevel.CRITICAL);
		}

		@Override
		public LogStream warn() {
			return new FilteredLogStream(logger.warn(), path, LogLevel.WARN);
		}

		@Override
		public LogStream info() {
			return new FilteredLogStream(logger.info(), path, LogLevel.INFO);
		}

		@Override
		public LogStream verbose() {
			return new FilteredLogStream(logger.verbose(), path, LogLevel.VERBOSE);
		}

		@Override
		public LogStream debug() {
			return new FilteredLogStream(logger.debug(), path, LogLevel.DEBUG);
		}

		@Override
		public LogStream trace() {
			return new FilteredLogStream(logger.trace(), path, LogLevel.TRACE);
		}
	}
	
	private class FilteredLogStream implements LogStream {
		
		private LogStream stream;
		private String path;
		private LogLevel level;
		
		public FilteredLogStream(LogStream stream, String path, LogLevel level) {
			this.stream = stream;
			this.path = path;
			this.level = level;
		}

		@Override
		public boolean isEnabled() {
			return level.ordinal() >= getLevel(path).ordinal();
		}

		@Override
		public void log(String message) {
			if (isEnabled()) {
				stream.log(message);
			}
		}

		@Override
		public void log(Throwable e) {
			if (isEnabled()) {
				stream.log(e);
			}
		}

		@Override
		public void log(String message, Throwable e) {
		    if (isEnabled()) {
		        stream.log(message, e);
		    }
		}

		@Override
		public void log(String format, Object... argument) {
			if (isEnabled()) {
				stream.log(format, argument);
			}
		}
	}
}
