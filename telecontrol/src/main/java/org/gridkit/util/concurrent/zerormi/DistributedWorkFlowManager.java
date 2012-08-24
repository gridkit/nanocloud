package org.gridkit.util.concurrent.zerormi;

public class DistributedWorkFlowManager {

	
	public interface BarrierBuilder {

		public BarrierBuilder party(String name);
		
		public ActionBuilder<BarrierBuilder> initialActions();
		
		public ConditionBuilder<BarrierBuilder> passConditions();
		
		public ActionBuilder<BarrierBuilder> enterActions();
		
		public ActionBuilder<BarrierBuilder> leaveActions();
		
	}
	
	public interface ActionBuilder<T> {
		
		public T done();
	}

	public interface ConditionBuilder<T> {
		
		public ConditionBuilder accure(String resource);
		
		public T done();
	}
}
