package edu.cuny.citytech.defaultrefactoring.ui.plugins;

import org.osgi.framework.BundleContext;

import edu.cuny.citytech.defaultrefactoring.ui.descriptors.MigrateSkeletalImplementationToInterfaceRefactoringDescriptor;
import edu.cuny.citytech.refactoring.common.RefactoringPlugin;

public class MigrateSkeletalImplementationToInterfaceRefactoringPlugin extends RefactoringPlugin {
	
	private static MigrateSkeletalImplementationToInterfaceRefactoringPlugin plugin;
	
	public static RefactoringPlugin getDefault() {
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		plugin = this;
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/* (non-Javadoc)
	 * @see edu.cuny.citytech.refactoring.common.RefactoringPlugin#getRefactoringId()
	 */
	@Override
	protected String getRefactoringId() {
		return MigrateSkeletalImplementationToInterfaceRefactoringDescriptor.REFACTORING_ID;
	}
}