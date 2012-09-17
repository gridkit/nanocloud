package org.gridkit.zerormi;

import java.io.IOException;


public interface Superviser {

	public static int CODE_TERMINATION = 400; 
	public static int CODE_TERMINATION_GRACEFUL = 401; 

	public static int CODE_WARNING = 300;
	public static int CODE_WARNING_MAX = 399;
	
	public static int CODE_ERROR = 500;
	public static int CODE_ERROR_UNEXPECTED = 501;
	public static int CODE_ERROR_IO = 510;
	public static int CODE_ERROR_MAX = 599;
	
	public void onWarning(SuperviserEvent event);

	public void onTermination(SuperviserEvent event);

	public void onFatalError(SuperviserEvent event);

	
	public static class SuperviserEvent {
		
		public static SuperviserEvent newClosedEvent(Object source) {
			return new SuperviserEvent(source, CODE_TERMINATION_GRACEFUL, "", null);
		}

		public static SuperviserEvent newStreamError(Object source, IOException e) {
			return new SuperviserEvent(source, CODE_ERROR_IO, "", e);
		}
		
		public static SuperviserEvent newUnexpectedError(Object source, Throwable e) {
			return new SuperviserEvent(source, CODE_ERROR_UNEXPECTED, e.getMessage(), e);
		}

		public static SuperviserEvent newUnexpectedError(Object source, String reason, Object... info) {
			return new SuperviserEvent(source, CODE_ERROR_UNEXPECTED, reason, null, info);
		}
		
		public static SuperviserEvent newWarning(Object source, String reason, Object... info) {
			return new SuperviserEvent(source, CODE_WARNING, reason, null, info);
		}
		
		public static SuperviserEvent newWarning(Object source, Throwable e) {
			return new SuperviserEvent(source, CODE_WARNING, e.getMessage(), e);
		}
		
		private Object component;
		private int eventCode;
		private String description;
		private Throwable exception;
		private Object[] additionalInfo;

		public SuperviserEvent(Object component, int eventCode, String description, Throwable exception, Object... additionalInfo) {
			this.component = component;
			this.eventCode = eventCode;
			this.description = description;
			this.exception = exception;
			this.additionalInfo = additionalInfo;
		}

		public boolean isWarning() {
			return eventCode >= CODE_WARNING && eventCode < CODE_WARNING_MAX;
		}

		public boolean isError() {
			return eventCode >= CODE_ERROR && eventCode < CODE_ERROR_MAX;
		}
		
		public Object getComponent() {
			return component;
		}

		public void setComponent(Object component) {
			this.component = component;
		}

		public int getEventCode() {
			return eventCode;
		}

		public void setEventCode(int eventCode) {
			this.eventCode = eventCode;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Throwable getException() {
			return exception;
		}

		public void setException(Throwable exception) {
			this.exception = exception;
		}

		public Object[] getAdditionalInfo() {
			return additionalInfo;
		}

		public void setAdditionalInfo(Object[] additionalInfo) {
			this.additionalInfo = additionalInfo;
		}
		
		public String toString() {
			return eventCode + " " + getMessage();
		}

		public  String getMessage() {
			if (description != null && description.length() > 0) {
				String format = description;
				if (description.contains("%error")) {
					format = description.replace((CharSequence)"%error", String.valueOf(exception));
				}
				try {
					return String.format(format, additionalInfo);
				}
				catch(Exception e) {
					return format;
				}
			}
			else {
				return String.valueOf(exception);
			}
		}
	}
	
	public static abstract class GenericSuperviser implements Superviser {

		@Override
		public void onWarning(SuperviserEvent event) {
			report(event);			
		}

		@Override
		public void onTermination(SuperviserEvent event) {
			report(event);
			onTerminate(event.getComponent());
		}

		@Override
		public void onFatalError(SuperviserEvent event) {
			report(event);			
			onError(event.getComponent());
		}

		protected abstract void report(SuperviserEvent event);	

		protected abstract void onTerminate(Object object);	

		protected abstract void onError(Object object);	
	}
}
