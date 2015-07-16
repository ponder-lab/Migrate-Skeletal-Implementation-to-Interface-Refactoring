/**
 * 
 */
package edu.cuny.citytech.defaultrefactoring.core.descriptors;

import java.util.Map;

import edu.cuny.citytech.defaultrefactoring.core.utils.Util;
import edu.cuny.citytech.refactoring.common.core.Refactoring;
import edu.cuny.citytech.refactoring.common.core.RefactoringDescriptor;

/**
 * @author raffi
 *
 */
public class MigrateSkeletalImplementationToInterfaceRefactoringDescriptor
		extends RefactoringDescriptor {

	public static final String REFACTORING_ID = "edu.cuny.citytech.defaultrefactoring.migrate.skeletal.implementation.to.interface"; //$NON-NLS-1$

	public MigrateSkeletalImplementationToInterfaceRefactoringDescriptor(
			String project, String description, String comment,
			@SuppressWarnings("rawtypes") Map arguments) {
		// TODO: May need an API change flag here as well.
		super(REFACTORING_ID, project, description, comment, arguments);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.cuny.citytech.refactoring.common.RefactoringDescriptor#createRefactoring
	 * ()
	 */
	@Override
	protected Refactoring createRefactoring() {
		org.eclipse.ltk.core.refactoring.Refactoring refactoring = Util.createRefactoring();
		return Util.createRefactoring(refactoring);
	}
}
