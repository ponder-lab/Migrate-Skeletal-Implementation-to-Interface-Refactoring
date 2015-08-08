package edu.cuny.citytech.defaultrefactoring.core.refactorings;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import edu.cuny.citytech.defaultrefactoring.core.descriptors.MigrateSkeletalImplementationToInterfaceRefactoringDescriptor;
import edu.cuny.citytech.defaultrefactoring.core.messages.Messages;
import edu.cuny.citytech.defaultrefactoring.core.utils.RefactoringAvailabilityTester;

// TODO: Are we checking the target interface? I think that the target interface should be completely empty for now.

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 */
@SuppressWarnings({ "restriction" })
public class MigrateSkeletalImplementationToInterfaceRefactoringProcessor extends PullUpRefactoringProcessor {

	/**
	 * Creates a new refactoring with the given methods to refactor.
	 * 
	 * @param methods
	 *            The methods to refactor.
	 */
	public MigrateSkeletalImplementationToInterfaceRefactoringProcessor(final IMethod[] methods,
			final CodeGenerationSettings settings, boolean layer) {
		super(methods, settings, layer);
		this.fCreateMethodStubs = false;
	}

	public MigrateSkeletalImplementationToInterfaceRefactoringProcessor(final IMethod[] methods,
			final CodeGenerationSettings settings) {
		this(methods, settings, false);
	}

	public MigrateSkeletalImplementationToInterfaceRefactoringProcessor() {
		this(null, null, false);
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		try {
			pm.beginTask(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CheckingPreconditions, 1);

			if (this.fMembersToMove.length == 0)
				return RefactoringStatus.createFatalErrorStatus(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_MethodsNotSpecified);
			else if (this.fMembersToMove.length > 1) {
				// TODO: For now.
				return RefactoringStatus.createFatalErrorStatus(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMoreThanOneMethod);
			} else {
				final RefactoringStatus status = new RefactoringStatus();
				status.merge(checkDeclaringType(new SubProgressMonitor(pm, 1)));

				if (status.hasFatalError())
					return status;

				status.merge(checkIfMembersExist());
				return status;
			}

		} catch (Exception e) {
			JavaPlugin.log(e);
			throw e;
		} finally {
			pm.done();
		}
	}

	@Override
	protected RefactoringStatus checkDeclaringType(IProgressMonitor monitor) throws JavaModelException {
		RefactoringStatus status = super.checkDeclaringType(monitor);

		if (!status.hasFatalError()) {
			final IType type = getDeclaringType();

			if (type.isAnonymous()) {
				// TODO for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInAnonymousTypes, type);
			}
			// TODO: This is being checked by the super implementation but need
			// to revisit. It might be okay to have an enum. In that case, we
			// can't call the super method.
			// if (type.isEnum()) {
			// // TODO for now.
			// addWarning(status,
			// Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInEnums,
			// method);
			// }
			if (type.isLambda()) {
				// TODO for now.
				return createFatalError(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInLambdas,
						type);
			}
			if (type.isLocal()) {
				// TODO for now.
				return createFatalError(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInLocals,
						type);
			}
			if (type.isMember()) {
				// TODO for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInMemberTypes, type);
			}
			if (!type.isClass()) {
				// TODO for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_MethodsOnlyInClasses, type);
			}
			if (type.getAnnotations().length != 0) {
				// TODO for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInAnnotatedTypes, type);
			}
			if (type.getFields().length != 0) {
				// TODO for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithFields, type);
			}
			if (type.getInitializers().length != 0) {
				// TODO for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithInitializers,
						type);
			}
			if (type.getMethods().length > 1) {
				// TODO for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithMoreThanOneMethod,
						type);
			}
			if (type.getTypeParameters().length != 0) {
				// TODO for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithTypeParameters,
						type);
			}
			if (type.getTypes().length != 0) {
				// TODO for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithType, type);
			}
			if (type.getSuperclassName() != null) {
				// TODO for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithSuperType,
						type);
			}
			if (type.getSuperInterfaceNames().length == 0) {
				// enclosing type must implement an interface, at least for now,
				// which one of which will become the target interface.
				// it is probably possible to still perform the refactoring
				// without this condition but I believe that this is
				// the particular pattern we are targeting.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesThatDontImplementInterfaces,
						type);
			}
			if (type.getSuperInterfaceNames().length > 1) {
				// TODO for now. Let's only deal with a single interface as that
				// is part of the targeted pattern.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesThatExtendMultipleInterfaces,
						type);
			}
			if (!Flags.isAbstract(type.getFlags())) {
				// TODO for now. This follows the target pattern. Maybe we can
				// relax this but that would require checking for
				// instantiations.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInConcreteTypes, type);
			}
			if (Flags.isStatic(type.getFlags())) {
				// TODO no static types for now.
				return createFatalError(
						Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInStaticTypes, type);
			}

			status.merge(checkDeclaringSuperTypes(monitor));
		}

		return status;
	}

	@Override
	protected RefactoringStatus checkDeclaringSuperTypes(final IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus result = new RefactoringStatus();
		IType[] interfaces = getCandidateTypes(result, monitor);

		if (interfaces.length == 0) {
			IType declaringType = getDeclaringType();

			final String msg = MessageFormat.format(
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithNoCandidateTargets,
					JavaElementLabels.getTextLabel(declaringType, JavaElementLabels.ALL_FULLY_QUALIFIED));

			return RefactoringStatus.createWarningStatus(msg);
		} else if (interfaces.length > 1) {
			// TODO For now, let's make sure there's only one candidate type.
			IType declaringType = getDeclaringType();

			final String msg = MessageFormat.format(
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithMultipleCandidateTargets,
					JavaElementLabels.getTextLabel(declaringType, JavaElementLabels.ALL_FULLY_QUALIFIED));

			return RefactoringStatus.createWarningStatus(msg);
		}

		return result;
	}

	/**
	 * Returns the possible target interfaces for the migration. NOTE: One
	 * difference here between this refactoring and pull up is that we can have
	 * a much more complex type hierarchy due to multiple interface inheritance
	 * in Java.
	 * 
	 * TODO: It should be possible to pull up a method into an interface (i.e.,
	 * "Pull Up Method To Interface") that is not implemented explicitly. For
	 * example, there may be a skeletal implementation class that implements all
	 * the target interface's methods without explicitly declaring so.
	 * 
	 * @param monitor
	 *            A progress monitor.
	 * @return The possible target interfaces for the migration.
	 * @throws JavaModelException
	 *             upon Java model problems.
	 */
	@Override
	public IType[] getCandidateTypes(final RefactoringStatus status, final IProgressMonitor monitor)
			throws JavaModelException {
		IType declaringType = getDeclaringType();
		IType[] superInterfaces = declaringType.newSupertypeHierarchy(monitor).getAllSuperInterfaces(declaringType);

		return Stream.of(superInterfaces).parallel()
				.filter(t -> t != null && t.exists() && !t.isReadOnly() && !t.isBinary()).toArray(IType[]::new);
	}

	protected RefactoringStatus checkMethods(IProgressMonitor pm) throws JavaModelException {
		try {
			RefactoringStatus status = new RefactoringStatus();
			Iterator<IMethod> it = getMethodsToMoveIterator();

			while (it.hasNext()) {
				IMethod method = it.next();

				if (!method.exists()) {
					addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_MethodDoesNotExist,
							method);
				}
				if (method.isBinary() || method.isReadOnly()) {
					addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CantChangeMethod,
							method);
				}
				if (!method.isStructureKnown()) {
					addWarning(status,
							Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CUContainsCompileErrors,
							method);
				}
				if (method.isConstructor()) {
					addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoConstructors,
							method);
				}
				if (method.getAnnotations().length > 0) {
					// TODO for now.
					addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoAnnotations,
							method);
				}
				if (Flags.isStatic(method.getFlags())) {
					// TODO for now.
					addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoStaticMethods,
							method);
				}
				if (JdtFlags.isNative(method)) {
					addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoNativeMethods,
							method);
				}
				if (method.isLambdaMethod()) {
					// TODO for now.
					addWarning(status, Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoLambdaMethods,
							method);
				}
				if (method.getExceptionTypes().length != 0) {
					// TODO for now.
					addWarning(status,
							Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsThatThrowExceptions,
							method);
				}
				if (method.getParameters().length != 0) {
					// TODO for now.
					addWarning(status,
							Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsWithParameters,
							method);
				}
				if (!method.getReturnType().equals(Signature.SIG_VOID)) {
					// return type must be void.
					// TODO for now.
					addWarning(status,
							Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsWithReturnTypes,
							method);
				}
				if (method.getTypeParameters().length != 0) {
					// TODO for now but this will be an important one.
					addWarning(status,
							Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsWithTypeParameters,
							method);
				}
				pm.worked(1);
			}

			if (!status.hasFatalError()) {
				status.merge(checkMethodBodies(new SubProgressMonitor(pm, fMembersToMove.length)));
			}

			return status;
		} finally {
			pm.done();
		}
	}

	protected Iterator<IMethod> getMethodsToMoveIterator() {
		return Stream.of(fMembersToMove).parallel().filter(m -> m instanceof IMethod).map(m -> (IMethod) m).iterator();
	}

	protected RefactoringStatus checkMethodBodies(IProgressMonitor pm) throws JavaModelException {
		try {
			RefactoringStatus status = new RefactoringStatus();
			Iterator<IMethod> it = this.getMethodsToMoveIterator();

			while (it.hasNext()) {
				IMethod method = it.next();
				ITypeRoot root = method.getCompilationUnit();
				CompilationUnit unit = RefactoringASTParser.parseWithASTProvider(root, false,
						new SubProgressMonitor(pm, 1));

				MethodDeclaration declaration = ASTNodeSearchUtil.getMethodDeclarationNode(method, unit);

				if (declaration != null) {
					Block body = declaration.getBody();

					if (body != null) {
						@SuppressWarnings("rawtypes")
						List statements = body.statements();

						if (!statements.isEmpty()) {
							// TODO for now.
							addWarning(status,
									Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsWithStatements,
									method);
						}
					}
				}
				pm.worked(1);
			}

			return status;
		} finally {
			pm.done();
		}
	}

	protected static void addWarning(RefactoringStatus status, String message, IMethod method) {
		status.addWarning(MessageFormat.format(message, method.getElementName()), JavaStatusContext.create(method));
	}

	protected static RefactoringStatus createFatalError(String message, IType type) {
		return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(message, type.getElementName()),
				JavaStatusContext.create(type));
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CheckingPreconditions, 12);
			clearCaches();

			final RefactoringStatus status = new RefactoringStatus();

			// workaround https://bugs.eclipse.org/bugs/show_bug.cgi?id=474524.
			if (fMembersToMove.length > 0)
			status.merge(createWorkingCopyLayer(new SubProgressMonitor(monitor, 4)));
			if (status.hasFatalError())
				return status;
			if (monitor.isCanceled())
				throw new OperationCanceledException();

			status.merge(checkMethods(new SubProgressMonitor(monitor, 1)));
			if (status.hasFatalError())
				return status;

			// TODO: Is this needed?
			// status.merge(checkProjectCompliance(getCompilationUnitRewrite(compilationUnitRewrites,
			// getDeclaringType().getCompilationUnit()), getDestinationType(),
			// fMembersToMove));

			// TODO: More checks need to be done here #15.

			return status;
		} catch (Exception e) {
			JavaPlugin.log(e);
			throw e;
		} finally {
			monitor.done();
		}
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {
			pm.beginTask(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CreatingChange, 1);

			return new NullChange(getProcessorName());
		} finally {
			pm.done();
		}
	}

	@Override
	protected void rewriteTypeOccurrences(TextEditBasedChangeManager manager, ASTRequestor requestor,
			CompilationUnitRewrite rewrite, ICompilationUnit unit, CompilationUnit node, Set<String> replacements,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
	}

	@Override
	public String getIdentifier() {
		return MigrateSkeletalImplementationToInterfaceRefactoringDescriptor.REFACTORING_ID;
	}

	@Override
	public String getProcessorName() {
		return Messages.MigrateSkeletalImplementationToInferfaceRefactoring_Name;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Will always return false as this refactoring will never create method
	 * stubs.
	 */
	@Override
	public boolean getCreateMethodStubs() {
		return false;
	}

	@Override
	public IMethod[] getPullableMembersOfDeclaringType() {
		try {
			return RefactoringAvailabilityTester.getMigratableSkeletalImplementations(getDeclaringType());
		} catch (JavaModelException e) {
			return new IMethod[0];
		}
	}

	@Override
	protected void registerChanges(TextEditBasedChangeManager manager) throws CoreException {
		// TODO Auto-generated method stub
		super.registerChanges(manager);
	}

	@Override
	public void resetEnvironment() {
		// TODO Auto-generated method stub
		super.resetEnvironment();
	}

	@Override
	public void setAbstractMethods(IMethod[] methods) {
		// TODO Auto-generated method stub
		// I don't think this applicable for us.
		super.setAbstractMethods(methods);
	}

	@Override
	public void setCreateMethodStubs(boolean create) {
		// TODO Auto-generated method stub
		// I don't think this applicable for us.
		super.setCreateMethodStubs(create);
	}

	@Override
	public void setDeletedMethods(IMethod[] methods) {
		// TODO Auto-generated method stub
		super.setDeletedMethods(methods);
	}

	@Override
	public void setDestinationType(IType type) {
		// TODO Auto-generated method stub
		super.setDestinationType(type);
	}

	@Override
	public void setMembersToMove(IMember[] members) {
		// TODO Auto-generated method stub
		super.setMembersToMove(members);
	}

	@Override
	protected RefactoringStatus checkConstructorCalls(IType type, IProgressMonitor monitor) throws JavaModelException {
		// TODO Auto-generated method stub
		return super.checkConstructorCalls(type, monitor);
	}

	@Override
	public ProcessorBasedRefactoring getRefactoring() {
		// TODO Auto-generated method stub
		return super.getRefactoring();
	}
}