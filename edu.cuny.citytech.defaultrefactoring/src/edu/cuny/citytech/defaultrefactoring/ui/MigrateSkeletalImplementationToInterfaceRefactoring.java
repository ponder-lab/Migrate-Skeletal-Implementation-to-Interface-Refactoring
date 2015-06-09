package edu.cuny.citytech.defaultrefactoring.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class MigrateSkeletalImplementationToInterfaceRefactoring extends Refactoring {

	// The plug-in ID
	public static final String PLUGIN_ID = "edu.cuny.citytech.defaultrefactoring.ui"; //$NON-NLS-1$

	// The shared instance
	private static MigrateSkeletalImplementationToInterfaceRefactoring plugin;
	
	/**
	 * The constructor
	 */
	public MigrateSkeletalImplementationToInterfaceRefactoring() {
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MigrateSkeletalImplementationToInterfaceRefactoring getDefault() {
		return plugin;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}
}
