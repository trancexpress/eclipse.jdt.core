/*******************************************************************************
 * Copyright (c) 2025 Advantest and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Advantest - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.builder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.tests.util.Util;

public class TestIgnoreClasspathChangesOnIncrementalBuild extends BuilderTests {

	public TestIgnoreClasspathChangesOnIncrementalBuild(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(TestIgnoreClasspathChangesOnIncrementalBuild.class);
	}

	private static final String IGNORE_CLASSPATH_CHANGES_ON_INCREMENTAL_BUILD= "org.eclipse.jdt.core.ignoreClasspathChangesOnIncrementalBuild";

	private boolean oldAutoBuilding;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		System.setProperty(IGNORE_CLASSPATH_CHANGES_ON_INCREMENTAL_BUILD, Boolean.TRUE.toString());
		this.oldAutoBuilding = env.isAutoBuilding();
		env.setAutoBuilding(false);
	}

	@Override
	public void tearDown() throws Exception {
		try {
			env.setAutoBuilding(this.oldAutoBuilding);
			System.clearProperty(IGNORE_CLASSPATH_CHANGES_ON_INCREMENTAL_BUILD);
		} finally {
			super.tearDown();
		}
	}

	public void testDifferentSourceFolder() throws Exception {
		IPath projectPath= env.addProject("testDifferentSourceFolder");
		env.removePackageFragmentRoot(projectPath, "");

		String srcFolder1= "src1";
		String outputFolder = "bin";
		String srcFolder2= "src2";

		env.addExternalJars(projectPath, Util.getJavaClassLibs());
		IPath root = env.addPackageFragmentRoot(projectPath, srcFolder1, null, null, outputFolder);
		String str= """
				package builder.mytests;
				public class Main {
					public static void main(String[] args) {
						System.out.println("Hello world");
					}
				}
				"""; //$NON-NLS-1$
		env.addClass(root, "builder.mytests.Main", str);
		env.fullBuild(projectPath);
		Path outputFolderPath= env.getWorkspaceRootPath().append(projectPath).append(outputFolder).toPath();

		IProject project = env.getProject(projectPath);
		project.getFolder(srcFolder1).copy(project.getFolder(srcFolder2).getFullPath(), true, null);

		Map<Path, FileTime> timestamps1= timestamps(outputFolderPath);

		env.removePackageFragmentRoot(projectPath, srcFolder1);
		env.fullBuild(projectPath);

		Map<Path, FileTime> timestamps2= timestamps(outputFolderPath);
		assertEquals(timestamps1.toString(), timestamps2.toString());

		env.addPackageFragmentRoot(projectPath, srcFolder2, null, null, outputFolder);
		env.incrementalBuild(projectPath);

		Map<Path, FileTime> timestamps3= timestamps(outputFolderPath);
		assertEquals(timestamps1.toString(), timestamps3.toString());
	}

	public void testSameSourceFolder() throws Exception {
		IPath projectPath= env.addProject("testSameSourceFolder");
		env.removePackageFragmentRoot(projectPath, "");

		String srcFolder= "src";
		String outputFolder = "bin";

		env.addExternalJars(projectPath, Util.getJavaClassLibs());
		IPath root = env.addPackageFragmentRoot(projectPath, srcFolder, null, null, outputFolder);
		String str= """
				package builder.mytests;
				public class Main {
					public static void main(String[] args) {
						System.out.println("Hello world");
					}
				}
				"""; //$NON-NLS-1$
		env.addClass(root, "builder.mytests.Main", str);
		env.fullBuild(projectPath);
		Path outputFolderPath= env.getWorkspaceRootPath().append(projectPath).append(outputFolder).toPath();

		Map<Path, FileTime> timestamps1= timestamps(outputFolderPath);

		IPath rootPath = env.getPackageFragmentRootPath(projectPath, srcFolder);
		env.removeEntry(projectPath, rootPath);
		env.fullBuild(projectPath);

		Map<Path, FileTime> timestamps2= timestamps(outputFolderPath);
		assertEquals(timestamps1.toString(), timestamps2.toString());

		env.addPackageFragmentRoot(projectPath, srcFolder, null, null, outputFolder);
		env.incrementalBuild(projectPath);

		Map<Path, FileTime> timestamps3= timestamps(outputFolderPath);
		assertEquals(timestamps1.toString(), timestamps3.toString());
	}

	private static Map<Path, FileTime> timestamps(Path outputFolderPath) throws IOException {
		Map<Path, FileTime> timestamps = new LinkedHashMap<>();
		try (Stream<Path> s= Files.walk(outputFolderPath)) {
			s.forEach(path -> {
				try {
					timestamps.put(path, Files.getLastModifiedTime(path));
				} catch (IOException e) {
					AssertionFailedError ex = new AssertionFailedError("Exception occurred while visiting output file: " + path);
					ex.initCause(e);
					throw ex;
				}
			});
		}
		return timestamps;
	}
}
