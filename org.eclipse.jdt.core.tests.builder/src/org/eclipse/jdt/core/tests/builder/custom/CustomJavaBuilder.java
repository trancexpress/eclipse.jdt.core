package org.eclipse.jdt.core.tests.builder.custom;

import org.eclipse.jdt.internal.core.builder.JavaBuilder;

public class CustomJavaBuilder extends JavaBuilder {

	public static final String IGNORE_CLASSPATH_CHANGES_ON_INCREMENTAL_BUILD = "org.eclipse.jdt.core.ignoreClasspathChangesOnIncrementalBuild"; //$NON-NLS-1$

	@Override
	protected boolean hasClasspathChanged() {
		return !Boolean.getBoolean(IGNORE_CLASSPATH_CHANGES_ON_INCREMENTAL_BUILD) && super.hasClasspathChanged();
	}
}
