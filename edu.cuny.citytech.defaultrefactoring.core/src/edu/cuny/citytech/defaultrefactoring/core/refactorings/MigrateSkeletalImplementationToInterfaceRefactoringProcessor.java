package edu.cuny.citytech.defaultrefactoring.core.refactorings;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.HierarchyProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.osgi.framework.FrameworkUtil;

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
public class MigrateSkeletalImplementationToInterfaceRefactoringProcessor extends HierarchyProcessor {

	/**
	 * The destination interface.
	 */
	private IType destinationInterface;

	/**
	 * The map of compilation units to compilation unit rewrites.
	 */
	private Map<ICompilationUnit, CompilationUnitRewrite> compilationUnitRewrites = new HashMap<>();

	@SuppressWarnings("unused")
	private static final GroupCategorySet SET_MIGRATE_METHOD_IMPLEMENTATION_TO_INTERFACE = new GroupCategorySet(
			new GroupCategory("edu.cuny.citytech.defaultrefactoring", //$NON-NLS-1$
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CategoryName,
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CategoryDescription));

	/**
	 * Creates a new refactoring with the given methods to refactor.
	 * 
	 * @param methods
	 *            The methods to refactor.
	 */
	public MigrateSkeletalImplementationToInterfaceRefactoringProcessor(final IMethod[] methods,
			final CodeGenerationSettings settings, boolean layer) {
		super(methods, settings, layer);
	}

	public MigrateSkeletalImplementationToInterfaceRefactoringProcessor(final IMethod[] methods,
			final CodeGenerationSettings settings) {
		this(methods, settings, false);
	}

	public MigrateSkeletalImplementationToInterfaceRefactoringProcessor() {
		this(null, null, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] getElements() {
		return fMembersToMove;
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

	protected RefactoringStatus checkDestinationInterface(IProgressMonitor monitor) {
		// TODO #15, #19
		return null;
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

	protected RefactoringStatus checkDeclaringSuperTypes(final IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus result = new RefactoringStatus();
		IType[] interfaces = getCandidateTypes(result, monitor);

		if (interfaces.length == 0) {
			IType declaringType = getDeclaringType();

			final String msg = MessageFormat.format(
					Messages.MigrateSkeletalImplementationToInferfaceRefactoring_NoMethodsInTypesWithNoCandidateTargets,
					createLabel(declaringType));

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

	protected IMethod[] getMethodsToMove() {
		return Stream.of(fMembersToMove).parallel().filter(m -> m instanceof IMethod).map(m -> (IMethod) m)
				.toArray(IMethod[]::new);
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

	private static void addWarning(RefactoringStatus status, String message, IMember member) {
		String elementName = JavaElementLabels.getElementLabel(member, JavaElementLabels.ALL_FULLY_QUALIFIED);
		status.addWarning(MessageFormat.format(message, elementName), JavaStatusContext.create(member));
	}

	private static void addError(RefactoringStatus status, String message, IMember member, IMember... more) {
		List<String> elementNames = new ArrayList<>();
		elementNames.add(JavaElementLabels.getElementLabel(member, JavaElementLabels.ALL_FULLY_QUALIFIED));

		Stream<String> stream = Arrays.asList(more).parallelStream()
				.map(MigrateSkeletalImplementationToInterfaceRefactoringProcessor::createLabel);
		Stream<String> concat = Stream.concat(elementNames.stream(), stream);
		List<String> collect = concat.collect(Collectors.toList());

		status.addError(MessageFormat.format(message, collect.toArray()), JavaStatusContext.create(member));
	}

	private static RefactoringStatus createFatalError(String message, IType type) {
		String elementName = createLabel(type);
		return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(message, elementName),
				JavaStatusContext.create(type));
	}

	/**
	 * Creates a working copy layer if necessary.
	 *
	 * @param monitor
	 *            the progress monitor to use
	 * @return a status describing the outcome of the operation
	 */
	protected RefactoringStatus createWorkingCopyLayer(IProgressMonitor monitor) {
		try {
			monitor.beginTask(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CheckingPreconditions, 1);
			ICompilationUnit unit = getDeclaringType().getCompilationUnit();
			if (fLayer)
				unit = unit.findWorkingCopy(fOwner);
			resetWorkingCopies(unit);
			return new RefactoringStatus();
		} finally {
			monitor.done();
		}
	}

	protected CompilationUnitRewrite getCompilationUnitRewrite(
			final Map<ICompilationUnit, CompilationUnitRewrite> rewrites, final ICompilationUnit unit) {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(unit);
		CompilationUnitRewrite rewrite = rewrites.get(unit);
		if (rewrite == null) {
			rewrite = new CompilationUnitRewrite(fOwner, unit);
			rewrites.put(unit, rewrite);
		}
		return rewrite;
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

			// workaround https://bugs.eclipse.org/bugs/show_bug.cgi?id=474524.
			// if (fMembersToMove.length > 0)
			// status.merge(checkProjectCompliance(
			// getCompilationUnitRewrite(compilationUnitRewrites,
			// getDeclaringType().getCompilationUnit()),
			// getDestinationType(), fMembersToMove));

			// TODO: More checks need to be done here #15.
			// TODO: More checks, perhaps resembling those in
			// org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor.checkFinalConditions(IProgressMonitor,
			// CheckConditionsContext).

			return status;
		} catch (Exception e) {
			JavaPlugin.log(e);
			throw e;
		} finally {
			monitor.done();
		}
	}

	protected static RefactoringStatus checkProjectCompliance(CompilationUnitRewrite sourceRewriter, IType destination,
			IMember[] members) {
		RefactoringStatus status = HierarchyProcessor.checkProjectCompliance(sourceRewriter, destination, members);

		if (!JavaModelUtil.is18OrHigher(destination.getJavaProject())) {
			Arrays.asList(members).stream().filter(e -> e instanceof IMethod).map(IMethod.class::cast)
					.filter(IMethod::isLambdaMethod)
					.forEach(m -> addError(status,
							Messages.MigrateSkeletalImplementationToInferfaceRefactoring_IncompatibleLanguageConstruct,
							m, destination));
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {
			pm.beginTask(Messages.MigrateSkeletalImplementationToInferfaceRefactoring_CreatingChange, 1);

			IType targetInterface = getDestinationInterface();

			Iterator<IMethod> methodsToMoveIterator = getMethodsToMoveIterator();
			while (methodsToMoveIterator.hasNext()) {
				IMethod method = methodsToMoveIterator.next();
				logInfo("Migrating method: "
						+ JavaElementLabels.getElementLabel(method, JavaElementLabels.ALL_FULLY_QUALIFIED)
						+ " to interface: " + destinationInterface.getFullyQualifiedName());

				ICompilationUnit sourceUnit = getDeclaringType().getCompilationUnit();
				Map<ICompilationUnit, CompilationUnitRewrite> rewrites = this.getCompilationUnitRewrites();
				CompilationUnitRewrite sourceRewrite = this.getCompilationUnitRewrite(rewrites, sourceUnit);
				CompilationUnit sourceRoot = sourceRewrite.getRoot();

				MethodDeclaration sourceMethodDeclaration = ASTNodeSearchUtil.getMethodDeclarationNode(method, sourceRoot);
				logInfo("Source method declaration: " + sourceMethodDeclaration);
				
				Block sourceMethodBody = sourceMethodDeclaration.getBody();
				Assert.isNotNull(sourceMethodBody, "Source method has a null body.");
				
				// TODO: Take the body onto the target method?
				// TODO: Change the target method to default.
				// TODO: Find the target method?
			}

			final TextEditBasedChangeManager manager = new TextEditBasedChangeManager();

			final Map<String, String> arguments = new HashMap<>();
			int flags = RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;

			MigrateSkeletalImplementationToInterfaceRefactoringDescriptor descriptor = new MigrateSkeletalImplementationToInterfaceRefactoringDescriptor(
					null, "TODO", null, arguments, flags);

			// TODO: Need to add changes.
			return new DynamicValidationRefactoringChange(descriptor, getProcessorName(), new Change[] {});
		} finally {
			pm.done();
		}
	}

	private void logInfo(String message) {
		String name = FrameworkUtil.getBundle(this.getClass()).getSymbolicName();
		IStatus status = new Status(IStatus.INFO, name, message);
		JavaPlugin.log(status);
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
		return RefactoringAvailabilityTester.isInterfaceMigrationAvailable(getMethodsToMove());
	}

	public IMethod[] getMigratableMembersOfDeclaringType() {
		try {
			return RefactoringAvailabilityTester.getMigratableSkeletalImplementations(getDeclaringType());
		} catch (JavaModelException e) {
			return new IMethod[0];
		}
	}

	@Override
	protected RefactoringStatus checkConstructorCalls(IType type, IProgressMonitor monitor) throws JavaModelException {
		// TODO Auto-generated method stub
		return super.checkConstructorCalls(type, monitor);
	}

	/**
	 * @return the destinationType
	 */
	public IType getDestinationInterface() {
		return destinationInterface;
	}

	/**
	 * Sets the destination interface.
	 * 
	 * @param destinationInterface
	 *            The destination interface.
	 * @throws JavaModelException
	 */
	public void setDestinationType(IType destinationInterface) throws JavaModelException {
		Assert.isNotNull(destinationInterface);
		// TODO: Is it possible to have a default method in an annotation?
		// TODO: Test this?
		Assert.isTrue(destinationInterface.isInterface() && !destinationInterface.isAnnotation(),
				"Destination type must be a pure interface.");

		// TODO: Cache type hierarchy?
		this.destinationInterface = destinationInterface;
	}

	@Override
	protected void rewriteTypeOccurrences(TextEditBasedChangeManager manager, ASTRequestor requestor,
			CompilationUnitRewrite rewrite, ICompilationUnit unit, CompilationUnit node, Set<String> replacements,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
	}

	protected Map<ICompilationUnit, CompilationUnitRewrite> getCompilationUnitRewrites() {
		return this.compilationUnitRewrites;
	}
}