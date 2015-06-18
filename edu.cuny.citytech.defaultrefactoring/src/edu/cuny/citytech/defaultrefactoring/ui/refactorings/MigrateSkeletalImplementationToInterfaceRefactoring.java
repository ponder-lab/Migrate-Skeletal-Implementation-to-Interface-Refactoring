package edu.cuny.citytech.defaultrefactoring.ui.refactorings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.citytech.defaultrefactoring.ui.messages.Messages;
import edu.cuny.citytech.refactoring.common.Refactoring;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 */
public class MigrateSkeletalImplementationToInterfaceRefactoring extends
		Refactoring {

	/**
	 * The methods to refactor.
	 */
	private IMethod[] methods;

	/**
	 * Creates a new refactoring with the given methods to refactor.
	 * 
	 * @param methods
	 *            The methods to refactor.
	 */
	public MigrateSkeletalImplementationToInterfaceRefactoring(IMethod[] methods) {
		this.methods = methods;
	}

	/**
	 * Default ctor.
	 */
	public MigrateSkeletalImplementationToInterfaceRefactoring() {
	}

	@Override
	public String getName() {
		return Messages.MigrateSkeletalImplementationToInferfaceRefactoring_Name;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus status = new RefactoringStatus();
		// TODO Probably should make sure that the enclosing type implements an
		// interface.
		return status;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus status = new RefactoringStatus();
		// TODO Auto-generated method stub
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {

		try {
			pm.beginTask(
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CreatingChange,
					1);
			
			return new NullChange(getName());
		} finally {
			pm.done();
		}
	}
}