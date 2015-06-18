package edu.cuny.citytech.defaultrefactoring.ui.refactorings;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.Flags;
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
	private Set<IMethod> methods;

	/**
	 * Creates a new refactoring with the given methods to refactor.
	 * 
	 * @param methods
	 *            The methods to refactor.
	 */
	public MigrateSkeletalImplementationToInterfaceRefactoring(
			IMethod... methods) {
		this.methods = new HashSet<IMethod>(Arrays.asList(methods));
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
		// TODO Can't be a static method (for now).
		// TODO Can't be part of an annotation (at least for now; this should be
		// checked on the declaring type).
		// TODO No enum methods.
		try {
			pm.beginTask(
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CheckingPreconditions,
					methods.size());

			if (this.methods.isEmpty())
				status.addFatalError(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_MethodsNotSpecified);
			else {
				for (Iterator<IMethod> iterator = methods.iterator(); iterator
						.hasNext();) {
					IMethod method = iterator.next();

					if (!method.exists()) {
						removeMethod(
								Messages.MigrateSkeletalImplementationToInferfaceRefactoring_MethodDoesNotExist,
								method, iterator, status, pm);
						iterator.remove();
						pm.worked(1);
					} else if (method.isBinary() || method.isReadOnly()) {
						removeMethod(
								Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CantChangeMethod,
								method, iterator, status, pm);
						iterator.remove();
						pm.worked(1);
					} else if (!method.isStructureKnown()) {
						removeMethod(
								Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CUContainsCompileErrors,
								method, iterator, status, pm);
					} else if (method.isConstructor()) {
						removeMethod(
								Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoConstructors,
								method, iterator, status, pm);
					} else if (method.getAnnotations().length > 0) {
						removeMethod(
								Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoAnnotations,
								method, iterator, status, pm);
					} else if (Flags.isStatic(method.getFlags())) {
						removeMethod(
								Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoStaticMethods,
								method, iterator, status, pm);
					}
				}
			}

			if (this.methods.isEmpty())
				status.addError(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_PreconditionFailed);

		} finally {
			pm.done();
		}

		return status;
	}

	private static void removeMethod(String message, IMethod method,
			Iterator<IMethod> iterator, final RefactoringStatus status,
			IProgressMonitor pm) {
		status.addWarning(MessageFormat.format(message, method.getElementName()));
		iterator.remove();
		pm.worked(1);
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