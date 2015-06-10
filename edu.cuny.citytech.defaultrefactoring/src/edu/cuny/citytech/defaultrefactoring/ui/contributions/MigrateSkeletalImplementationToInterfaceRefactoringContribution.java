package edu.cuny.citytech.defaultrefactoring.ui.contributions;

import java.util.Map;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

import edu.cuny.citytech.defaultrefactoring.ui.descriptors.MigrateSkeletalImplementationToInterfaceRefactoringDescriptor;
import edu.cuny.citytech.refactoring.common.RefactoringContribution;

public class MigrateSkeletalImplementationToInterfaceRefactoringContribution
		extends RefactoringContribution {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.RefactoringContribution#createDescriptor
	 * (java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * java.util.Map, int)
	 */
	@Override
	public RefactoringDescriptor createDescriptor(String id, String project,
			String description, String comment,
			@SuppressWarnings("rawtypes") Map arguments, int flags)
			throws IllegalArgumentException {
		return new MigrateSkeletalImplementationToInterfaceRefactoringDescriptor(
				project, description, comment, arguments);
	}
}
