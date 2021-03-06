package com.koch.ambeth.extscanner.model;

import com.koch.ambeth.util.collections.ArrayList;

public class AnnotationEntry extends AbstractSourceFileAware
		implements Comparable<AnnotationEntry> {
	public final String simpleName;

	public final String annotationName;

	public final String moduleName;

	public String labelName;

	public final ArrayList<TypeEntry> usedInTypes = new ArrayList<>();

	public AnnotationEntry(String fqName, String simpleName, String annotationLabelName,
			String moduleName) {
		annotationName = fqName;
		this.simpleName = simpleName;
		labelName = annotationLabelName;
		this.moduleName = moduleName;
	}

	@Override
	public int compareTo(AnnotationEntry o) {
		return simpleName.compareTo(o.simpleName);
	}
}
