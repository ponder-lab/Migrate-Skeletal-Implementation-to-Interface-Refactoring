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
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.ui.JavaPlugin;
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
@SuppressWarnings("restriction")
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
		try {
			pm.beginTask(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CheckingPreconditions,
					methods.size());

			if (methods.isEmpty())
				return RefactoringStatus.createFatalErrorStatus(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_MethodsNotSpecified);
			else {
				final RefactoringStatus status = new RefactoringStatus();

				for (Iterator<IMethod> iterator = methods.iterator(); iterator.hasNext();) {
					IMethod method = iterator.next();

					status.merge(checkMethod(method));
					status.merge(checkDeclaringType(method));

					if (!status.isOK())
						iterator.remove();

					pm.worked(1);
				}

				if (methods.isEmpty())
					status.addError(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_PreconditionFailed);

				return status;
			}

		} catch (Exception e) {
			JavaPlugin.log(e);
			throw e;
		} finally {
			pm.done();
		}
	}

	protected RefactoringStatus checkDeclaringType(IMethod method) throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();
		IType declaringType = method.getDeclaringType();

		if (declaringType.isInterface()) {
			// Should not support methods already in interfaces.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInInterfaces,
					method);
		}
		if (declaringType.isAnonymous()) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInAnonymousTypes,
					method);
		}
		if (declaringType.isEnum()) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInEnums, method);
		}
		if (declaringType.isLambda()) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInLambdas, method);
		}
		if (declaringType.isLocal()) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInLocals, method);
		}
		if (declaringType.isMember()) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInMemberTypes,
					method);
		}
		if (!declaringType.isClass()) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_MethodsOnlyInClasses,
					method);
		}
		if (declaringType.getAnnotations().length != 0) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInAnnotatedTypes,
					method);
		}
		if (declaringType.getFields().length != 0) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithFields,
					method);
		}
		if (declaringType.getInitializers().length != 0) {
			// TODO for now.
			addWarning(status,
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithInitializers,
					method);
		}
		if (declaringType.getMethods().length > 1) {
			// TODO for now.
			addWarning(status,
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithMoreThanOneMethod,
					method);
		}
		if (declaringType.getTypeParameters().length != 0) {
			// TODO for now.
			addWarning(status,
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithTypeParameters,
					method);
		}
		if (declaringType.getTypes().length != 0) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithType,
					method);
		}
		if (declaringType.getSuperclassName() != null) {
			// TODO for now.
			addWarning(status,
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithSuperType, method);
		}
		if (declaringType.getSuperInterfaceNames().length == 0) {
			// enclosing type must implement an interface, at least for now,
			// which one of which will become the target interface.
			// it is probably possible to still perform the refactoring without
			// this condition but I believe that this is
			// the particular pattern we are targeting.
			addWarning(status,
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesThatDontImplementInterfaces,
					method);
		}
		if (declaringType.getSuperInterfaceNames().length > 1) {
			// TODO for now. Let's only deal with a single interface as that is
			// part of the targeted pattern.
			addWarning(status,
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesThatExtendMultipleInterfaces,
					method);
		}
		if (!Flags.isAbstract(declaringType.getFlags())) {
			// TODO for now. This follows the target pattern. Maybe we can relax
			// this but that would require checking for instantiations.
			addWarning(status,
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInConcreteTypes,
					method);
		}
		if (Flags.isStatic(declaringType.getFlags())) {
			// TODO no static types for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInStaticTypes, method);
		}

		return status;
	}

	protected RefactoringStatus checkMethod(IMethod method) throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();

		if (!method.exists()) {
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_MethodDoesNotExist, method);
		}
		if (method.isBinary() || method.isReadOnly()) {
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CantChangeMethod, method);
		}
		if (!method.isStructureKnown()) {
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CUContainsCompileErrors,
					method);
		}
		if (method.isConstructor()) {
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoConstructors, method);
		}
		if (method.getAnnotations().length > 0) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoAnnotations, method);
		}
		if (Flags.isStatic(method.getFlags())) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoStaticMethods, method);
		}
		if (method.isLambdaMethod()) {
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoLambdaMethods, method);
		}
		if (method.getExceptionTypes().length != 0) {
			// TODO for now.
			addWarning(status,
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsThatThrowExceptions, method);
		}
		 if (method.getParameters().length != 0) {
			// TODO for now.
			addWarning(status,
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsWithParameters, method);
		}
		if (!method.getReturnType().equals(Signature.SIG_VOID)) { //return type must be void.
			// TODO for now.
			addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsWithReturnTypes,
					method);
		 }

		return status;
	}

	protected static void addWarning(RefactoringStatus status, String message, IMethod method) {
		status.addWarning(MessageFormat.format(message, method.getElementName()), JavaStatusContext.create(method));
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