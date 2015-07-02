package edu.cuny.citytech.defaultrefactoring.core.refactorings;

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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.citytech.defaultrefactoring.core.messages.Messages;
import edu.cuny.citytech.refactoring.common.core.Refactoring;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 */
public class MigrateSkeletalImplementationToInterfaceRefactoring extends Refactoring {

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
	public MigrateSkeletalImplementationToInterfaceRefactoring(IMethod... methods) {
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
		try {
			pm.beginTask(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CheckingPreconditions,
					methods.size());

			if (this.methods.isEmpty())
				status.addFatalError(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_MethodsNotSpecified);
			else {
				for (Iterator<IMethod> iterator = methods.iterator(); iterator.hasNext();) {
					IMethod method = iterator.next();

					checkMethodLevelInitialConditions(method, iterator, status, pm);
					checkDeclaringTypeLevelInitialConditions(method, iterator, status, pm);
				}
			}

			if (this.methods.isEmpty())
				status.addError(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_PreconditionFailed);

		} finally {
			pm.done();
		}

		return status;
	}

	/**
	 * @param method
	 *            The method for which to check the enclosing type.
	 * @param iterator
	 * @param status
	 * @param pm
	 * @throws JavaModelException
	 */
	protected void checkDeclaringTypeLevelInitialConditions(IMethod method, Iterator<IMethod> iterator,
			RefactoringStatus status, IProgressMonitor pm) throws JavaModelException {
		// TODO Probably should make sure that the enclosing type implements an
		// interface.
		IType declaringType = method.getDeclaringType();

		if (declaringType.isInterface()) {
			// Should not support methods already in interfaces.
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInInterfaces, method,
					iterator, status, pm);
		} else if (declaringType.isAnonymous()) {
			// TODO for now.
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInAnonymousTypes, method,
					iterator, status, pm);
		} else if (declaringType.isEnum()) {
			// TODO for now.
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInEnums, method, iterator, status, pm);
		} else if (declaringType.isLambda()) {
			// TODO for now.
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInLambdas, method, iterator, status, pm);
		} else if (declaringType.isLocal()) {
			// TODO for now.
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInLocals, method, iterator, status, pm);
		} else if (declaringType.isMember()) {
			// TODO for now.
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInMemberTypes, method, iterator, status, pm);
		}

	}

	/**
	 * @param method
	 * @param iterator
	 * @param status
	 * @param pm
	 * @throws JavaModelException
	 */
	protected void checkMethodLevelInitialConditions(IMethod method, Iterator<IMethod> iterator,
			final RefactoringStatus status, IProgressMonitor pm) throws JavaModelException {
		if (!method.exists()) {
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_MethodDoesNotExist, method,
					iterator, status, pm);
		} else if (method.isBinary() || method.isReadOnly()) {
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CantChangeMethod, method,
					iterator, status, pm);
		} else if (!method.isStructureKnown()) {
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CUContainsCompileErrors, method,
					iterator, status, pm);
		} else if (method.isConstructor()) {
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoConstructors, method, iterator,
					status, pm);
		} else if (method.getAnnotations().length > 0) {
			// TODO for now.
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoAnnotations, method, iterator,
					status, pm);
		} else if (Flags.isStatic(method.getFlags())) {
			// TODO for now.
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoStaticMethods, method, iterator,
					status, pm);
		} else if (method.isLambdaMethod()) {
			// TODO for now.
			removeMethod(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoLambdaMethods, method, iterator,
					status, pm);
		}
	}

	protected static void removeMethod(String message, IMethod method, Iterator<IMethod> iterator,
			final RefactoringStatus status, IProgressMonitor pm) {
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
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		try {
			pm.beginTask(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CreatingChange, 1);

			return new NullChange(getName());
		} finally {
			pm.done();
		}
	}
}