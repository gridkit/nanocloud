package org.gridkit.vicluster;

import java.io.Serializable;

public class Label implements ViMarker<String>, Serializable {

	private static final long serialVersionUID = 20120921L;

	private static final String LABEL = "LABEL";
	
	private final String label;
	
	public Label(String label) {
		this.label = label;
	}

	@Override
	public String getName() {
		return LABEL;
	}

	@Override
	public String getValue() {
		return label;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Label other = (Label) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}

	public String toString() {
		return LABEL;
	}
}
