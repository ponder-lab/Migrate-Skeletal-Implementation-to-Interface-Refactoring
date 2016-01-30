package edu.cuny.citytech.defaultrefactoring.core.refactorings;

import static org.eclipse.jdt.ui.JavaElementLabels.ALL_DEFAULT;
import static org.eclipse.jdt.ui.JavaElementLabels.ALL_FULLY_QUALIFIED;
import static org.eclipse.jdt.ui.JavaElementLabels.getElementLabel;
import static org.eclipse.jdt.ui.JavaElementLabels.getTextLabel;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ReferenceFinderUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.FrameworkUtil;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

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
public class MigrateSkeletalImplementationToInterfaceRefactoringProcessor extends RefactoringProcessor {

	private Set<IMethod> sourceMethods = new HashSet<>();

	private Set<IMethod> unmigratableMethods = new HashSet<>();

	private static final String FUNCTIONAL_INTERFACE_ANNOTATION_NAME = "FunctionalInterface";

	private Map<CompilationUnit, ASTRewrite> compilationUnitToASTRewriteMap = new HashMap<>();

	private Map<ITypeRoot, CompilationUnit> typeRootToCompilationUnitMap = new HashMap<>();

	@SuppressWarnings("unused")
	private static final GroupCategorySet SET_MIGRATE_METHOD_IMPLEMENTATION_TO_INTERFACE = new GroupCategorySet(
			new GroupCategory("edu.cuny.citytech.defaultrefactoring", //$NON-NLS-1$
					Messages.CategoryName, Messages.CategoryDescription));

	private Map<IMethod, IMethod> sourceMethodToTargetMethodMap = new HashMap<>();

	/** The code generation settings, or <code>null</code> */
	protected CodeGenerationSettings settings;

	/** Does the refactoring use a working copy layer? */
	protected final boolean layer;

	private static Table<IMethod, IType, IMethod> sourceMethodTargetInterfaceTargetMethodTable = HashBasedTable
			.create();

	/**
	 * Creates a new refactoring with the given methods to refactor.
	 * 
	 * @param methods
	 *            The methods to refactor.
	 * @throws JavaModelException
	 */
	public MigrateSkeletalImplementationToInterfaceRefactoringProcessor(final IMethod[] methods,
			final CodeGenerationSettings settings, boolean layer, Optional<IProgressMonitor> monitor)
					throws JavaModelException {
		try {
			this.settings = settings;
			this.layer = layer;
			this.sourceMethods = new HashSet<>(Arrays.asList(methods));

			monitor.ifPresent(m -> m.beginTask("Finding target methods ...", this.sourceMethods.size()));

			for (IMethod method : this.sourceMethods) {
				this.sourceMethodToTargetMethodMap.put(method, getTargetMethod(method, monitor));
				monitor.ifPresent(m -> m.worked(1));
			}

		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	public MigrateSkeletalImplementationToInterfaceRefactoringProcessor(final IMethod[] methods,
			final CodeGenerationSettings settings, Optional<IProgressMonitor> monitor) throws JavaModelException {
		this(methods, settings, false, monitor);
	}

	public MigrateSkeletalImplementationToInterfaceRefactoringProcessor(Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		this(null, null, false, monitor);
	}

	public MigrateSkeletalImplementationToInterfaceRefactoringProcessor() throws JavaModelException {
		this(null, null, false, Optional.empty());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] getElements() {
		Set<IMethod> difference = new HashSet<>(this.getSourceMethods());
		difference.removeAll(this.getUnmigratableMethods());
		return difference.toArray();
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		try {
			if (this.getSourceMethods().isEmpty())
				return RefactoringStatus.createFatalErrorStatus(Messages.MethodsNotSpecified);
			else {
				RefactoringStatus status = new RefactoringStatus();

				pm.beginTask(Messages.CheckingPreconditions, this.getSourceMethods().size());

				for (IMethod method : this.getSourceMethods()) {
					status.merge(checkDeclaringType(method, Optional.of(new SubProgressMonitor(pm, 0))));
					status.merge(
							checkCandidateDestinationInterfaces(method, Optional.of(new SubProgressMonitor(pm, 0))));
					// FIXME: Repeated.
					// TODO: Also, does not remove the method if there is an
					// error.
					status.merge(checkExistence(method, Messages.MethodDoesNotExist));

					pm.worked(1);
				}
				return status;
			}
		} catch (Exception e) {
			JavaPlugin.log(e);
			throw e;
		} finally {
			pm.done();
		}
	}

	protected RefactoringStatus checkDestinationInterfaceTargetMethods(IMethod sourceMethod) throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();

		logInfo("Checking destination interface target methods...");

		// Ensure that target methods are not already default methods.
		// For each method to move, add a warning if the associated target
		// method is already default.
		IMethod targetMethod = this.getSourceMethodToTargetMethodMap().get(sourceMethod);

		if (targetMethod != null) {
			int targetMethodFlags = targetMethod.getFlags();

			if (Flags.isDefaultMethod(targetMethodFlags)) {
				RefactoringStatusEntry entry = addError(status, Messages.TargetMethodIsAlreadyDefault, targetMethod);
				addUnmigratableMethod(sourceMethod, entry);
			}
		}
		return status;
	}

	protected RefactoringStatus checkDestinationInterfaceOnlyDeclaresTargetMethods(IMethod sourceMethod)
			throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();

		// get the destination interface.
		IType destinationInterface = this.getSourceMethodToTargetMethodMap().get(sourceMethod).getDeclaringType();

		// get the methods declared by the destination interface.
		Set<IMethod> destinationInterfaceMethodsSet = new HashSet<>(Arrays.asList(destinationInterface.getMethods()));

		// get the target methods that are declared by the destination
		// interface.
		Set<IMethod> targetMethodDeclaredByDestinationInterfaceSet = this.getSourceMethodToTargetMethodMap().values()
				.parallelStream().filter(Objects::nonNull)
				.filter(m -> m.getDeclaringType().equals(destinationInterface)).collect(Collectors.toSet());

		// TODO: For now, the target interface must only contain the target
		// methods.
		if (!destinationInterfaceMethodsSet.equals(targetMethodDeclaredByDestinationInterfaceSet)) {
			RefactoringStatusEntry error = addError(status,
					Messages.DestinationInterfaceMustOnlyDeclareTheMethodToMigrate, destinationInterface);
			addUnmigratableMethod(sourceMethod, error);
		}

		return status;
	}

	protected RefactoringStatus checkDestinationInterfaces(Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		try {
			RefactoringStatus status = new RefactoringStatus();

			monitor.ifPresent(m -> m.beginTask("Checking destination interfaces ...", this.getSourceMethods().size()));

			for (IMethod sourceMethod : this.getSourceMethods()) {
				// TODO #19
				final Optional<IType> targetInterface = this.getDestinationInterface(sourceMethod);

				// Can't be null.
				if (!targetInterface.isPresent()) {
					RefactoringStatusEntry error = addError(status, Messages.NoDestinationInterface);
					addUnmigratableMethod(sourceMethod, error);
					return status;
				}

				// Must be a pure interface.
				if (!isPureInterface(targetInterface.get())) {
					RefactoringStatusEntry error = addError(status, Messages.DestinationTypeMustBePureInterface,
							targetInterface.get());
					addUnmigratableMethod(sourceMethod, error);
				}

				// Make sure it exists.
				RefactoringStatus existence = checkExistence(targetInterface.get(),
						Messages.DestinationInterfaceDoesNotExist);
				status.merge(existence);
				if (!existence.isOK())
					addUnmigratableMethod(sourceMethod, existence.getEntryWithHighestSeverity());

				// Make sure we can write to it.
				RefactoringStatus writabilitiy = checkWritabilitiy(targetInterface.get(),
						Messages.DestinationInterfaceNotWritable);
				status.merge(writabilitiy);
				if (!writabilitiy.isOK())
					addUnmigratableMethod(sourceMethod, writabilitiy.getEntryWithHighestSeverity());

				// Make sure it doesn't have compilation errors.
				RefactoringStatus structure = checkStructure(targetInterface.get());
				status.merge(structure);
				if (!structure.isOK())
					addUnmigratableMethod(sourceMethod, structure.getEntryWithHighestSeverity());

				// TODO: For now, no annotated target interfaces.
				if (targetInterface.get().getAnnotations().length != 0) {
					RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceHasAnnotations,
							targetInterface.get());
					addUnmigratableMethod(sourceMethod, error);
				}

				// #35: The target interface should not be a
				// @FunctionalInterface.
				if (isInterfaceFunctional(targetInterface.get())) {
					RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceIsFunctional,
							targetInterface.get());
					addUnmigratableMethod(sourceMethod, error);
				}

				// TODO: For now, only top-level types.
				if (targetInterface.get().getDeclaringType() != null) {
					RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceIsNotTopLevel,
							targetInterface.get());
					addUnmigratableMethod(sourceMethod, error);
				}

				// TODO: For now, no fields.
				if (targetInterface.get().getFields().length != 0) {
					RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceDeclaresFields,
							targetInterface.get());
					addUnmigratableMethod(sourceMethod, error);
				}

				// TODO: For now, no super interfaces.
				if (targetInterface.get().getSuperInterfaceNames().length != 0) {
					RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceExtendsInterface,
							targetInterface.get());
					addUnmigratableMethod(sourceMethod, error);
				}

				// TODO: For now, no type parameters.
				if (targetInterface.get().getTypeParameters().length != 0) {
					RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceDeclaresTypeParameters,
							targetInterface.get());
					addUnmigratableMethod(sourceMethod, error);
				}

				// TODO: For now, no member types.
				if (targetInterface.get().getTypes().length != 0) {
					RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceDeclaresMemberTypes,
							targetInterface.get());
					addUnmigratableMethod(sourceMethod, error);
				}

				// TODO: For now, no member interfaces.
				if (targetInterface.get().isMember()) {
					RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceIsMember,
							targetInterface.get());
					addUnmigratableMethod(sourceMethod, error);
				}

				// Can't be strictfp if all the methods to be migrated aren't
				// also strictfp #42.
				if (Flags.isStrictfp(targetInterface.get().getFlags())
						&& !allMethodsToMoveInTypeAreStrictFP(sourceMethod.getDeclaringType())) {
					RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceIsStrictFP,
							targetInterface.get());
					addUnmigratableMethod(sourceMethod, error);
				}

				status.merge(checkDestinationInterfaceHierarchy(sourceMethod,
						monitor.map(m -> new SubProgressMonitor(m, 1))));
				status.merge(checkDestinationInterfaceTargetMethods(sourceMethod));

				monitor.ifPresent(m -> m.worked(1));
			}

			return status;
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}
	
	protected IType[] getTypesReferencedInMovedMembers(IMethod sourceMethod, final Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		// TODO: Cache this result.
		final IType[] types = ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[] { sourceMethod },
				monitor.orElseGet(NullProgressMonitor::new));
		final List<IType> result = new ArrayList<IType>(types.length);
		final List<IMember> members = Arrays.asList(new IMember[] { sourceMethod });
		for (int index = 0; index < types.length; index++) {
			if (!members.contains(types[index]) && !types[index].equals(sourceMethod.getDeclaringType()))
				result.add(types[index]);
		}
		return result.toArray(new IType[result.size()]);
	}

	protected boolean canBeAccessedFrom(IMethod sourceMethod, final IMember member, final IType target,
			final ITypeHierarchy hierarchy) throws JavaModelException {
		Assert.isTrue(!(member instanceof IInitializer));
		if (member.exists()) {
			if (target.isInterface())
				return true;
			if (target.equals(member.getDeclaringType()))
				return true;
			if (target.equals(member))
				return true;
			if (member instanceof IMethod) {
				final IMethod method = (IMethod) member;
				final IMethod stub = target.getMethod(method.getElementName(), method.getParameterTypes());
				if (stub.exists())
					return true;
			}
			if (member.getDeclaringType() == null) {
				if (!(member instanceof IType))
					return false;
				if (JdtFlags.isPublic(member))
					return true;
				if (!JdtFlags.isPackageVisible(member))
					return false;
				if (JavaModelUtil.isSamePackage(((IType) member).getPackageFragment(), target.getPackageFragment()))
					return true;
				final IType type = member.getDeclaringType();
				if (type != null)
					return hierarchy.contains(type);
				return false;
			}
			final IType declaringType = member.getDeclaringType();
			if (!canBeAccessedFrom(sourceMethod, declaringType, target, hierarchy))
				return false;
			if (declaringType.equals(sourceMethod.getDeclaringType()))
				return false;
			return true;
		}
		return false;
	}

	private RefactoringStatus checkAccessedTypes(IMethod sourceMethod, final Optional<IProgressMonitor> monitor,
			final ITypeHierarchy hierarchy) throws JavaModelException {
		final RefactoringStatus result = new RefactoringStatus();
		final IType[] accessedTypes = getTypesReferencedInMovedMembers(sourceMethod, monitor);
		final IType destination = getDestinationInterface(sourceMethod).get();
		final List<IMember> pulledUpList = Arrays.asList(new IMember[] { sourceMethod });
		for (int index = 0; index < accessedTypes.length; index++) {
			final IType type = accessedTypes[index];
			if (!type.exists())
				continue;

			if (!canBeAccessedFrom(sourceMethod, type, destination, hierarchy) && !pulledUpList.contains(type)) {
				final String message = org.eclipse.jdt.internal.corext.util.Messages
						.format(RefactoringCoreMessages.PullUpRefactoring_type_not_accessible,
								new String[] { JavaElementLabels.getTextLabel(type,
										JavaElementLabels.ALL_FULLY_QUALIFIED),
								JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED) });
				result.addError(message, JavaStatusContext.create(type));
			}
		}
		monitor.ifPresent(IProgressMonitor::done);
		return result;
	}

	// skipped super classes are those declared in the hierarchy between the
	// declaring type of the selected members
	// and the target type
	private Set<IType> getSkippedSuperTypes(IMethod sourceMethod, final Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		// TODO: Cache this?
		Set<IType> skippedSuperTypes = new HashSet<>();
		monitor.ifPresent(m -> m.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1));
		try {
			final ITypeHierarchy hierarchy = getDestinationInterfaceHierarchy(sourceMethod,
					monitor.map(m -> new SubProgressMonitor(m, 1)));
			IType current = hierarchy.getSuperclass(sourceMethod.getDeclaringType());
			while (current != null && !current.equals(getDestinationInterface(sourceMethod).get())) {
				skippedSuperTypes.add(current);
				current = hierarchy.getSuperclass(current);
			}
			return skippedSuperTypes;
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	private RefactoringStatus checkAccessedFields(IMethod sourceMethod, final Optional<IProgressMonitor> monitor,
			final ITypeHierarchy hierarchy) throws JavaModelException {
		monitor.ifPresent(m -> m.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 2));
		final RefactoringStatus result = new RefactoringStatus();

		final List<IMember> pulledUpList = Arrays.asList(new IMember[] { sourceMethod });
		final IField[] accessedFields = ReferenceFinderUtil.getFieldsReferencedIn(new IJavaElement[] { sourceMethod },
				new SubProgressMonitor(monitor.orElseGet(NullProgressMonitor::new), 1));

		final IType destination = getDestinationInterface(sourceMethod).get();
		for (int i = 0; i < accessedFields.length; i++) {
			final IField field = accessedFields[i];
			if (!field.exists())
				continue;

			boolean isAccessible = pulledUpList.contains(field)
					|| canBeAccessedFrom(sourceMethod, field, destination, hierarchy) || Flags.isEnum(field.getFlags());
			if (!isAccessible) {
				final String message = org.eclipse.jdt.internal.corext.util.Messages
						.format(RefactoringCoreMessages.PullUpRefactoring_field_not_accessible,
								new String[] { JavaElementLabels.getTextLabel(field,
										JavaElementLabels.ALL_FULLY_QUALIFIED),
								JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED) });
				result.addError(message, JavaStatusContext.create(field));
			} else if (getSkippedSuperTypes(sourceMethod, monitor.map(m -> new SubProgressMonitor(m, 1)))
					.contains(field.getDeclaringType())) {
				final String message = org.eclipse.jdt.internal.corext.util.Messages
						.format(RefactoringCoreMessages.PullUpRefactoring_field_cannot_be_accessed,
								new String[] { JavaElementLabels.getTextLabel(field,
										JavaElementLabels.ALL_FULLY_QUALIFIED),
								JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED) });
				result.addError(message, JavaStatusContext.create(field));
			}
		}
		monitor.ifPresent(IProgressMonitor::done);
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IMethod sourceMethod, final Optional<IProgressMonitor> monitor,
			final ITypeHierarchy hierarchy) throws JavaModelException {
		monitor.ifPresent(m -> m.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 2));
		final RefactoringStatus result = new RefactoringStatus();

		final List<IMember> pulledUpList = Arrays.asList(new IMember[] { sourceMethod });
		final IMethod[] accessedMethods = ReferenceFinderUtil.getMethodsReferencedIn(
				new IJavaElement[] { sourceMethod },
				new SubProgressMonitor(monitor.orElseGet(NullProgressMonitor::new), 1));

		final IType destination = getDestinationInterface(sourceMethod).get();
		for (int index = 0; index < accessedMethods.length; index++) {
			final IMethod method = accessedMethods[index];
			if (!method.exists())
				continue;
			boolean isAccessible = pulledUpList.contains(method)
					|| canBeAccessedFrom(sourceMethod, method, destination, hierarchy);
			if (!isAccessible) {
				final String message = org.eclipse.jdt.internal.corext.util.Messages
						.format(RefactoringCoreMessages.PullUpRefactoring_method_not_accessible,
								new String[] { JavaElementLabels.getTextLabel(method,
										JavaElementLabels.ALL_FULLY_QUALIFIED),
								JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED) });
				result.addError(message, JavaStatusContext.create(method));
			} else if (getSkippedSuperTypes(sourceMethod, monitor.map(m -> new SubProgressMonitor(m, 1)))
					.contains(method.getDeclaringType())) {
				final String[] keys = { JavaElementLabels.getTextLabel(method, JavaElementLabels.ALL_FULLY_QUALIFIED),
						JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED) };
				final String message = org.eclipse.jdt.internal.corext.util.Messages
						.format(RefactoringCoreMessages.PullUpRefactoring_method_cannot_be_accessed, keys);
				result.addError(message, JavaStatusContext.create(method));
			}
		}
		monitor.ifPresent(IProgressMonitor::done);
		return result;
	}

	private RefactoringStatus checkAccesses(IMethod sourceMethod, final Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		final RefactoringStatus result = new RefactoringStatus();
		try {
			monitor.ifPresent(
					m -> m.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 4));
			final ITypeHierarchy hierarchy = getSuperTypeHierarchy(getDestinationInterface(sourceMethod).get(),
					monitor.map(m -> new SubProgressMonitor(m, 1)));
			result.merge(checkAccessedTypes(sourceMethod, monitor.map(m -> new SubProgressMonitor(m, 1)), hierarchy));
			result.merge(checkAccessedFields(sourceMethod, monitor.map(m -> new SubProgressMonitor(m, 1)), hierarchy));
			result.merge(checkAccessedMethods(sourceMethod, monitor.map(m -> new SubProgressMonitor(m, 1)), hierarchy));
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
		return result;
	}

	private boolean allMethodsToMoveInTypeAreStrictFP(IType type) throws JavaModelException {
		for (Iterator<IMethod> iterator = this.getSourceMethods().iterator(); iterator.hasNext();) {
			IMethod method = iterator.next();
			if (method.getDeclaringType().equals(type) && !Flags.isStrictfp(method.getFlags()))
				return false;
		}
		return true;
	}

	private static boolean isInterfaceFunctional(final IType anInterface) throws JavaModelException {
		// TODO: #37.
		return Stream.of(anInterface.getAnnotations()).parallel().map(IAnnotation::getElementName)
				.anyMatch(s -> s.contains(FUNCTIONAL_INTERFACE_ANNOTATION_NAME));
	}

	protected RefactoringStatus checkDestinationInterfaceHierarchy(IMethod sourceMethod,
			Optional<IProgressMonitor> monitor) throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();
		monitor.ifPresent(m -> m.subTask("Checking destination interface hierarchy..."));

		IType destinationInterface = this.getSourceMethodToTargetMethodMap().get(sourceMethod).getDeclaringType();

		final ITypeHierarchy hierarchy = this.getTypeHierarchy(destinationInterface,
				monitor.map(m -> new SubProgressMonitor(m, 1)));

		status.merge(checkValidClassesInHierarchy(sourceMethod, hierarchy,
				Messages.DestinationInterfaceHierarchyContainsInvalidClass));
		status.merge(checkValidInterfacesInHierarchy(sourceMethod, hierarchy,
				Messages.DestinationInterfaceHierarchyContainsInvalidInterfaces));
		status.merge(checkValidSubtypes(sourceMethod, hierarchy));

		// TODO: For now, no super interfaces.
		if (hierarchy.getAllSuperInterfaces(destinationInterface).length > 0) {
			RefactoringStatusEntry error = addError(status,
					Messages.DestinationInterfaceHierarchyContainsSuperInterface, destinationInterface);
			addUnmigratableMethod(sourceMethod, error);
		}

		// TODO: For now, no extending interfaces.
		if (hierarchy.getExtendingInterfaces(destinationInterface).length > 0) {
			RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceHasExtendingInterface,
					destinationInterface);
			addUnmigratableMethod(sourceMethod, error);
		}

		// TODO: For now, the destination interface can only be implemented by
		// the source class.
		if (!Stream.of(hierarchy.getImplementingClasses(destinationInterface)).parallel().distinct()
				.allMatch(c -> c.equals(sourceMethod.getDeclaringType()))) {
			RefactoringStatusEntry error = addError(status, Messages.DestinationInterfaceHasInvalidImplementingClass,
					destinationInterface);
			addUnmigratableMethod(sourceMethod, error);
		}

		return status;
	}

	private RefactoringStatus checkValidSubtypes(IMethod sourceMethod, final ITypeHierarchy hierarchy)
			throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();

		// TODO: For now, no subtypes except the declaring type.
		// FIXME: Really, it should match the declaring type of the method to be
		// migrated.
		IType destinationInterface = getSourceMethodToTargetMethodMap().get(sourceMethod).getDeclaringType();
		if (!Stream.of(hierarchy.getAllSubtypes(destinationInterface)).distinct()
				.allMatch(s -> s.equals(sourceMethod.getDeclaringType())))
			addError(status, Messages.DestinationInterfaceHierarchyContainsSubtype, destinationInterface);

		return status;
	}

	private RefactoringStatus checkValidInterfacesInHierarchy(IMethod sourceMethod, final ITypeHierarchy hierarchy,
			String errorMessage) throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();

		Optional<IType> destinationInterface = getDestinationInterface(sourceMethod);

		// TODO: For now, there should be only one interface in the hierarchy,
		// and that is the target interface.
		boolean containsOnlyValidInterfaces = Stream.of(hierarchy.getAllInterfaces()).parallel().distinct()
				.allMatch(i -> i.equals(destinationInterface.orElse(null)));

		if (!containsOnlyValidInterfaces)
			addError(status, errorMessage, hierarchy.getType());

		return status;
	}

	private Optional<IType> getDestinationInterface(IMethod sourceMethod) {
		return Optional.ofNullable(this.getSourceMethodToTargetMethodMap().get(sourceMethod))
				.map(IMethod::getDeclaringType);
	}

	private RefactoringStatus checkValidClassesInHierarchy(IMethod sourceMethod, final ITypeHierarchy hierarchy,
			String errorMessage) throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();

		// TODO: For now, the only class in the hierarchy should be the
		// declaring class of the source method and java.lang.Object.
		List<IType> allClassesAsList = Arrays.asList(hierarchy.getAllClasses());

		// TODO: All the methods to move may not be from the same type. This is
		// in regards to getDeclaringType(), which only returns one type.
		boolean containsOnlyValidClasses = allClassesAsList.size() == 2
				&& allClassesAsList.contains(sourceMethod.getDeclaringType())
				&& allClassesAsList.contains(hierarchy.getType().getJavaProject().findType("java.lang.Object"));

		if (!containsOnlyValidClasses) {
			RefactoringStatusEntry error = addError(status, errorMessage, hierarchy.getType());
			addUnmigratableMethod(sourceMethod, error);
		}

		return status;
	}

	@SuppressWarnings("unused")
	private void addWarning(RefactoringStatus status, String message) {
		addWarning(status, message, new IJavaElement[] {});
	}

	protected RefactoringStatus checkDeclaringType(IMethod sourceMethod, Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();
		IType type = sourceMethod.getDeclaringType();

		if (type.isEnum())
			// TODO: For now. It might be okay to have an enum.
			addErrorAndMark(status, Messages.NoMethodsInEnums, sourceMethod, type);
		if (type.isAnnotation())
			addErrorAndMark(status, Messages.NoMethodsInAnnotationTypes, sourceMethod, type);
		if (type.isInterface())
			addErrorAndMark(status, Messages.NoMethodsInInterfaces, sourceMethod, type);
		if (type.isBinary())
			addErrorAndMark(status, Messages.NoMethodsInBinaryTypes, sourceMethod, type);
		if (type.isReadOnly())
			addErrorAndMark(status, Messages.NoMethodsInReadOnlyTypes, sourceMethod, type);
		if (type.isAnonymous())
			addErrorAndMark(status, Messages.NoMethodsInAnonymousTypes, sourceMethod, type);
		if (type.isLambda())
			// TODO for now.
			addErrorAndMark(status, Messages.NoMethodsInLambdas, sourceMethod, type);
		if (type.isLocal())
			// TODO for now.
			addErrorAndMark(status, Messages.NoMethodsInLocals, sourceMethod, type);
		if (type.isMember())
			// TODO for now.
			addErrorAndMark(status, Messages.NoMethodsInMemberTypes, sourceMethod, type);
		if (!type.isClass())
			// TODO for now.
			addErrorAndMark(status, Messages.MethodsOnlyInClasses, sourceMethod, type);
		if (type.getAnnotations().length != 0)
			// TODO for now.
			addErrorAndMark(status, Messages.NoMethodsInAnnotatedTypes, sourceMethod, type);
		if (type.getFields().length != 0)
			// TODO for now.
			addErrorAndMark(status, Messages.NoMethodsInTypesWithFields, sourceMethod, type);
		if (type.getInitializers().length != 0)
			// TODO for now.
			addErrorAndMark(status, Messages.NoMethodsInTypesWithInitializers, sourceMethod, type);
		if (type.getTypeParameters().length != 0)
			// TODO for now.
			addErrorAndMark(status, Messages.NoMethodsInTypesWithTypeParameters, sourceMethod, type);
		if (type.getTypes().length != 0)
			// TODO for now.
			addErrorAndMark(status, Messages.NoMethodsInTypesWithType, sourceMethod, type);
		if (type.getSuperclassName() != null)
			// TODO for now.
			addErrorAndMark(status, Messages.NoMethodsInTypesWithSuperType, sourceMethod, type);
		if (type.getSuperInterfaceNames().length == 0)
			// TODO enclosing type must implement an interface, at least for
			// now,
			// which one of which will become the target interface.
			// it is probably possible to still perform the refactoring
			// without this condition but I believe that this is
			// the particular pattern we are targeting.
			addErrorAndMark(status, Messages.NoMethodsInTypesThatDontImplementInterfaces, sourceMethod, type);
		if (type.getSuperInterfaceNames().length > 1)
			// TODO for now. Let's only deal with a single interface as that
			// is part of the targeted pattern.
			addErrorAndMark(status, Messages.NoMethodsInTypesThatExtendMultipleInterfaces, sourceMethod, type);
		if (!Flags.isAbstract(type.getFlags()))
			// TODO for now. This follows the target pattern. Maybe we can
			// relax this but that would require checking for
			// instantiations.
			addErrorAndMark(status, Messages.NoMethodsInConcreteTypes, sourceMethod, type);
		if (Flags.isStatic(type.getFlags()))
			// TODO no static types for now.
			addErrorAndMark(status, Messages.NoMethodsInStaticTypes, sourceMethod, type);

		status.merge(checkDeclaringTypeHierarchy(sourceMethod, monitor.map(m -> new SubProgressMonitor(m, 1))));

		return status;
	}

	private void addErrorAndMark(RefactoringStatus status, String message, IMethod sourceMethod, IMember... related) {
		RefactoringStatusEntry error = addError(status, message, sourceMethod, related);
		addUnmigratableMethod(sourceMethod, error);
	}

	protected RefactoringStatus checkDeclaringTypeHierarchy(IMethod sourceMethod, Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		try {
			RefactoringStatus status = new RefactoringStatus();
			monitor.ifPresent(m -> m.subTask("Checking declaring type hierarchy..."));

			final ITypeHierarchy hierarchy = this.getDeclaringTypeHierarchy(sourceMethod, monitor);

			status.merge(checkValidClassesInHierarchy(sourceMethod, hierarchy,
					Messages.DeclaringTypeHierarchyContainsInvalidClass));
			status.merge(checkValidInterfacesInHierarchy(sourceMethod, hierarchy,
					Messages.DeclaringTypeHierarchyContainsInvalidInterface));

			// TODO: For now, the declaring type should have no subtypes.
			if (hierarchy.getAllSubtypes(sourceMethod.getDeclaringType()).length != 0) {
				RefactoringStatusEntry error = addError(status, Messages.DeclaringTypeContainsSubtype,
						sourceMethod.getDeclaringType());
				addUnmigratableMethod(sourceMethod, error);
			}

			// TODO: For now, only java.lang.Object as the super class.
			final IType object = hierarchy.getType().getJavaProject().findType("java.lang.Object");
			if (!Stream.of(hierarchy.getAllSuperclasses(sourceMethod.getDeclaringType())).parallel().distinct()
					.allMatch(t -> t.equals(object))) {
				RefactoringStatusEntry error = addError(status, Messages.DeclaringTypeContainsInvalidSupertype,
						sourceMethod.getDeclaringType());
				addUnmigratableMethod(sourceMethod, error);
			}

			return status;
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	protected RefactoringStatus checkCandidateDestinationInterfaces(IMethod sourceMethod,
			final Optional<IProgressMonitor> monitor) throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();

		IType[] interfaces = getCandidateDestinationInterfaces(sourceMethod,
				monitor.map(m -> new SubProgressMonitor(m, 1)));

		if (interfaces.length == 0)
			addErrorAndMark(status, Messages.NoMethodsInTypesWithNoCandidateTargetTypes, sourceMethod,
					sourceMethod.getDeclaringType());
		else if (interfaces.length > 1)
			// TODO For now, let's make sure there's only one candidate type.
			addErrorAndMark(status, Messages.NoMethodsInTypesWithMultipleCandidateTargetTypes, sourceMethod,
					sourceMethod.getDeclaringType());

		return status;
	}

	/**
	 * Returns the possible target interfaces for the migration. NOTE: One
	 * difference here between this refactoring and pull up is that we can have
	 * a much more complex type hierarchy due to multiple interface inheritance
	 * in Java.
	 * <p>
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
	public static IType[] getCandidateDestinationInterfaces(IMethod method, final Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		try {
			monitor.ifPresent(m -> m.beginTask("Retrieving candidate types...", IProgressMonitor.UNKNOWN));

			IType[] superInterfaces = getSuperInterfaces(method.getDeclaringType(),
					monitor.map(m -> new SubProgressMonitor(m, 1)));

			return Stream.of(superInterfaces).parallel().filter(Objects::nonNull).filter(IJavaElement::exists)
					.filter(t -> !t.isReadOnly()).filter(t -> !t.isBinary()).filter(t -> {
						IMethod[] methods = t.findMethods(method);
						return methods != null && methods.length > 0;
					}).toArray(IType[]::new);
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	private static IType[] getSuperInterfaces(IType type, final Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		try {
			monitor.ifPresent(m -> m.beginTask("Retrieving type super interfaces...", IProgressMonitor.UNKNOWN));
			return getSuperTypeHierarchy(type, monitor.map(m -> new SubProgressMonitor(m, 1)))
					.getAllSuperInterfaces(type);
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	private ITypeHierarchy getDeclaringTypeHierarchy(IMethod sourceMethod, final Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		try {
			monitor.ifPresent(m -> m.subTask("Retrieving declaring type hierarchy..."));
			IType declaringType = sourceMethod.getDeclaringType();
			return this.getTypeHierarchy(declaringType, monitor);
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	private ITypeHierarchy getDestinationInterfaceHierarchy(IMethod sourceMethod,
			final Optional<IProgressMonitor> monitor) throws JavaModelException {
		try {
			monitor.ifPresent(m -> m.subTask("Retrieving destination type hierarchy..."));
			IType destinationInterface = getDestinationInterface(sourceMethod).get();
			return this.getTypeHierarchy(destinationInterface, monitor);
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	private static Map<IType, ITypeHierarchy> typeToSuperTypeHierarchyMap = new HashMap<>();

	private static Map<IType, ITypeHierarchy> getTypeToSuperTypeHierarchyMap() {
		return typeToSuperTypeHierarchyMap;
	}

	private static ITypeHierarchy getSuperTypeHierarchy(IType type, final Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		try {
			monitor.ifPresent(m -> m.subTask("Retrieving declaring super type hierarchy..."));

			if (getTypeToSuperTypeHierarchyMap().containsKey(type))
				return getTypeToSuperTypeHierarchyMap().get(type);
			else {
				ITypeHierarchy newSupertypeHierarchy = type
						.newSupertypeHierarchy(monitor.orElseGet(NullProgressMonitor::new));
				getTypeToSuperTypeHierarchyMap().put(type, newSupertypeHierarchy);
				return newSupertypeHierarchy;
			}
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	protected RefactoringStatus checkMethodsToMigrate(Optional<IProgressMonitor> pm) throws JavaModelException {
		try {
			RefactoringStatus status = new RefactoringStatus();
			pm.ifPresent(m -> m.beginTask(Messages.CheckingPreconditions, this.getSourceMethods().size()));

			for (IMethod sourceMethod : this.getSourceMethods()) {

				// FIXME: Repeated.
				RefactoringStatus existenceStatus = checkExistence(sourceMethod, Messages.MethodDoesNotExist);
				if (!existenceStatus.isOK()) {
					status.merge(existenceStatus);
					addUnmigratableMethod(sourceMethod, existenceStatus.getEntryWithHighestSeverity());
				}

				RefactoringStatus writabilityStatus = checkWritabilitiy(sourceMethod, Messages.CantChangeMethod);
				if (!writabilityStatus.isOK()) {
					status.merge(writabilityStatus);
					addUnmigratableMethod(sourceMethod, writabilityStatus.getEntryWithHighestSeverity());
				}

				RefactoringStatus structureStatus = checkStructure(sourceMethod);
				if (!structureStatus.isOK()) {
					status.merge(structureStatus);
					addUnmigratableMethod(sourceMethod, structureStatus.getEntryWithHighestSeverity());
				}

				if (sourceMethod.isConstructor()) {
					RefactoringStatusEntry entry = addError(status, Messages.NoConstructors, sourceMethod);
					addUnmigratableMethod(sourceMethod, entry);
				}

				status.merge(checkAnnotations(sourceMethod));

				// synchronized methods aren't allowed in interfaces (even
				// if they're default).
				if (Flags.isSynchronized(sourceMethod.getFlags())) {
					RefactoringStatusEntry entry = addError(status, Messages.NoSynchronizedMethods, sourceMethod);
					addUnmigratableMethod(sourceMethod, entry);
				}
				if (Flags.isStatic(sourceMethod.getFlags())) {
					RefactoringStatusEntry entry = addError(status, Messages.NoStaticMethods, sourceMethod);
					addUnmigratableMethod(sourceMethod, entry);
				}
				if (Flags.isAbstract(sourceMethod.getFlags())) {
					RefactoringStatusEntry entry = addError(status, Messages.NoAbstractMethods, sourceMethod);
					addUnmigratableMethod(sourceMethod, entry);
				}
				// final methods aren't allowed in interfaces.
				if (Flags.isFinal(sourceMethod.getFlags())) {
					RefactoringStatusEntry entry = addError(status, Messages.NoFinalMethods, sourceMethod);
					addUnmigratableMethod(sourceMethod, entry);
				}
				// native methods don't have bodies. As such, they can't
				// be skeletal implementors.
				if (JdtFlags.isNative(sourceMethod)) {
					RefactoringStatusEntry entry = addError(status, Messages.NoNativeMethods, sourceMethod);
					addUnmigratableMethod(sourceMethod, entry);
				}
				if (sourceMethod.isLambdaMethod()) {
					RefactoringStatusEntry entry = addError(status, Messages.NoLambdaMethods, sourceMethod);
					addUnmigratableMethod(sourceMethod, entry);
				}

				status.merge(checkExceptions(sourceMethod));
				status.merge(checkParameters(sourceMethod));

				if (!sourceMethod.getReturnType().equals(Signature.SIG_VOID)) {
					// return type must be void.
					// TODO for now. Can't remove this until we allow at
					// least
					// one statement.
					RefactoringStatusEntry entry = addError(status, Messages.NoMethodsWithReturnTypes, sourceMethod);
					addUnmigratableMethod(sourceMethod, entry);
				}

				// ensure that the method has a target.
				if (this.getSourceMethodToTargetMethodMap().get(sourceMethod) == null)
					addErrorAndMark(status, Messages.SourceMethodHasNoTargetMethod, sourceMethod);
				else // otherwise, check accesses in the source method.
					status.merge(checkAccesses(sourceMethod, pm.map(m -> new SubProgressMonitor(m, 1))));

				pm.ifPresent(m -> m.worked(1));
			}

			if (!status.hasFatalError())
				status.merge(checkMethodsToMigrateBodies(new SubProgressMonitor(pm.orElseGet(NullProgressMonitor::new),
						this.getSourceMethods().size())));

			return status;
		} finally {
			pm.ifPresent(IProgressMonitor::done);
		}
	}

	/**
	 * Annotations between source and target methods must be consistent. Related
	 * to #45.
	 * 
	 * @param sourceMethod
	 *            The method to check annotations.
	 * @return The resulting {@link RefactoringStatus}.
	 * @throws JavaModelException
	 *             If the {@link IAnnotation}s cannot be retrieved.
	 */
	private RefactoringStatus checkAnnotations(IMethod sourceMethod) throws JavaModelException {
		IMethod targetMethod = this.getSourceMethodToTargetMethodMap().get(sourceMethod);

		if (targetMethod != null && !checkAnnotations(sourceMethod, targetMethod).isOK()) {
			RefactoringStatus status = RefactoringStatus.createErrorStatus(
					formatMessage(Messages.AnnotationMismatch, sourceMethod, targetMethod),
					JavaStatusContext.create(sourceMethod));
			addUnmigratableMethod(sourceMethod, status.getEntryWithHighestSeverity());
			return status;
		}

		return new RefactoringStatus(); // OK.
	}

	private void addUnmigratableMethod(IMethod method, Object reason) {
		this.getUnmigratableMethods().add(method);
		this.logInfo(
				"Method " + getElementLabel(method, ALL_FULLY_QUALIFIED) + " is not migratable because: " + reason);
	}

	private RefactoringStatus checkAnnotations(IAnnotatable source, IAnnotatable target) throws JavaModelException {
		// a set of annotations from the source method.
		Set<IAnnotation> sourceAnnotationSet = new HashSet<>(Arrays.asList(source.getAnnotations()));

		// remove any annotations to not consider.
		removeSpecialAnnotations(sourceAnnotationSet);

		// a set of source method annotation names.
		Set<String> sourceMethodAnnotationElementNames = sourceAnnotationSet.parallelStream()
				.map(IAnnotation::getElementName).collect(Collectors.toSet());

		// a set of target method annotation names.
		Set<String> targetAnnotationElementNames = getAnnotationElementNames(target);

		// if the source method annotation names don't match the target method
		// annotation names.
		if (!sourceMethodAnnotationElementNames.equals(targetAnnotationElementNames))
			return RefactoringStatus.createErrorStatus(Messages.AnnotationNameMismatch, new RefactoringStatusContext() {

				@Override
				public Object getCorrespondingElement() {
					return source;
				}
			});
		else { // otherwise, we have the same annotations names. Check the
				// values.
			for (IAnnotation sourceAnnotation : sourceAnnotationSet) {
				IMemberValuePair[] sourcePairs = sourceAnnotation.getMemberValuePairs();

				IAnnotation targetAnnotation = target.getAnnotation(sourceAnnotation.getElementName());
				IMemberValuePair[] targetPairs = targetAnnotation.getMemberValuePairs();

				// sanity check.
				Assert.isTrue(sourcePairs.length == targetPairs.length, "Source and target pairs differ.");

				Arrays.parallelSort(sourcePairs, Comparator.comparing(IMemberValuePair::getMemberName));
				Arrays.parallelSort(targetPairs, Comparator.comparing(IMemberValuePair::getMemberName));

				for (int i = 0; i < sourcePairs.length; i++)
					if (!sourcePairs[i].getMemberName().equals(targetPairs[i].getMemberName())
							|| sourcePairs[i].getValueKind() != targetPairs[i].getValueKind()
							|| !(sourcePairs[i].getValue().equals(targetPairs[i].getValue())))
						return RefactoringStatus.createErrorStatus(
								formatMessage(Messages.AnnotationValueMismatch, sourceAnnotation, targetAnnotation),
								JavaStatusContext.create(findEnclosingMember(sourceAnnotation)));
			}
		}
		return new RefactoringStatus(); // OK.
	}

	/**
	 * Remove any annotations that we don't want considered.
	 * 
	 * @param annotationSet
	 *            The set of annotations to work with.
	 */
	protected void removeSpecialAnnotations(Set<IAnnotation> annotationSet) {
		// Special case: don't consider the @Override annotation in the source
		// (the target will never have this) #67.
		annotationSet.removeIf(a -> a.getElementName().equals(Override.class.getName()));
		annotationSet.removeIf(a -> a.getElementName().equals(Override.class.getSimpleName()));
	}

	private static IMember findEnclosingMember(IJavaElement element) {
		if (element == null)
			return null;
		else if (element instanceof IMember)
			return (IMember) element;
		else
			return findEnclosingMember(element.getParent());
	}

	private Set<String> getAnnotationElementNames(IAnnotatable annotatable) throws JavaModelException {
		return Arrays.stream(annotatable.getAnnotations()).parallel().map(IAnnotation::getElementName)
				.collect(Collectors.toSet());
	}

	/**
	 * #44: Ensure that exception types between the source and target methods
	 * match.
	 * 
	 * @param sourceMethod
	 *            The source method.
	 * @return The corresponding {@link RefactoringStatus}.
	 * @throws JavaModelException
	 *             If there is trouble retrieving exception types from
	 *             sourceMethod.
	 */
	private RefactoringStatus checkExceptions(IMethod sourceMethod) throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();

		IMethod targetMethod = this.getSourceMethodToTargetMethodMap().get(sourceMethod);

		if (targetMethod != null) {
			Set<String> sourceMethodExceptionTypeSet = getExceptionTypeSet(sourceMethod);
			Set<String> targetMethodExceptionTypeSet = getExceptionTypeSet(targetMethod);

			if (!sourceMethodExceptionTypeSet.equals(targetMethodExceptionTypeSet)) {
				RefactoringStatusEntry entry = addError(status, Messages.ExceptionTypeMismatch, sourceMethod,
						targetMethod);
				addUnmigratableMethod(sourceMethod, entry);
			}
		}

		return status;
	}

	private static Set<String> getExceptionTypeSet(IMethod method) throws JavaModelException {
		return Stream.of(method.getExceptionTypes()).parallel().collect(Collectors.toSet());
	}

	/**
	 * Check that the annotations in the parameters are consistent between the
	 * source and target.
	 * 
	 * FIXME: What if the annotation type is not available in the target?
	 * 
	 * @param sourceMethod
	 *            The method to check.
	 * @return {@link RefactoringStatus} indicating the result of the check.
	 * @throws JavaModelException
	 */
	private RefactoringStatus checkParameters(IMethod sourceMethod) throws JavaModelException {
		IMethod targetMethod = this.getSourceMethodToTargetMethodMap().get(sourceMethod);

		// for each parameter.
		for (int i = 0; i < sourceMethod.getParameters().length; i++) {
			ILocalVariable sourceParameter = sourceMethod.getParameters()[i];

			// get the corresponding target parameter.
			ILocalVariable targetParameter = targetMethod.getParameters()[i];

			if (!checkAnnotations(sourceParameter, targetParameter).isOK()) {
				RefactoringStatus status = RefactoringStatus
						.createErrorStatus(formatMessage(Messages.MethodContainsInconsistentParameterAnnotations,
								sourceMethod, targetMethod), JavaStatusContext.create(sourceMethod));
				addUnmigratableMethod(sourceMethod, status.getEntryWithHighestSeverity());
			}
		}

		return new RefactoringStatus(); // OK.
	}

	private RefactoringStatus checkStructure(IMember member) throws JavaModelException {
		if (!member.isStructureKnown()) {
			return RefactoringStatus.createErrorStatus(
					MessageFormat.format(Messages.CUContainsCompileErrors, getElementLabel(member, ALL_FULLY_QUALIFIED),
							getElementLabel(member.getCompilationUnit(), ALL_FULLY_QUALIFIED)),
					JavaStatusContext.create(member.getCompilationUnit()));
		}
		return new RefactoringStatus();
	}

	private static RefactoringStatusEntry getLastRefactoringStatusEntry(RefactoringStatus status) {
		return status.getEntryAt(status.getEntries().length - 1);
	}

	private RefactoringStatus checkWritabilitiy(IMember member, String message) {
		if (member.isBinary() || member.isReadOnly()) {
			return createError(message, member);
		}
		return new RefactoringStatus();
	}

	private RefactoringStatus checkExistence(IMember member, String message) {
		if (member == null || !member.exists()) {
			return createError(message, member);
		}
		return new RefactoringStatus();
	}

	protected Set<IMethod> getSourceMethods() {
		return this.sourceMethods;
	}

	protected Set<IMethod> getUnmigratableMethods() {
		return this.unmigratableMethods;
	}

	protected RefactoringStatus checkMethodsToMigrateBodies(IProgressMonitor pm) throws JavaModelException {
		try {
			RefactoringStatus status = new RefactoringStatus();

			Iterator<IMethod> it = this.getSourceMethods().iterator();
			while (it.hasNext()) {
				IMethod method = it.next();
				ITypeRoot root = method.getCompilationUnit();
				CompilationUnit unit = this.getCompilationUnit(root, new SubProgressMonitor(pm, 1));

				MethodDeclaration declaration = ASTNodeSearchUtil.getMethodDeclarationNode(method, unit);

				if (declaration != null) {
					Block body = declaration.getBody();

					if (body != null) {
						@SuppressWarnings("rawtypes")
						List statements = body.statements();

						if (!statements.isEmpty()) {
							// TODO for now.
							RefactoringStatusEntry entry = addError(status, Messages.NoMethodsWithStatements, method);
							addUnmigratableMethod(method, entry);
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

	private static void addWarning(RefactoringStatus status, String message, IJavaElement... relatedElementCollection) {
		addEntry(status, RefactoringStatus.WARNING, message, relatedElementCollection);
	}

	private static RefactoringStatusEntry addError(RefactoringStatus status, String message,
			IJavaElement... relatedElementCollection) {
		addEntry(status, RefactoringStatus.ERROR, message, relatedElementCollection);
		return getLastRefactoringStatusEntry(status);
	}

	private static void addEntry(RefactoringStatus status, int severity, String message,
			IJavaElement... relatedElementCollection) {
		message = formatMessage(message, relatedElementCollection);

		// add the first element as the context if appropriate.
		if (relatedElementCollection.length > 0 && relatedElementCollection[0] instanceof IMember) {
			IMember member = (IMember) relatedElementCollection[0];
			RefactoringStatusContext context = JavaStatusContext.create(member);
			status.addEntry(new RefactoringStatusEntry(severity, message, context));
		} else // otherwise, just add the message.
			status.addEntry(new RefactoringStatusEntry(severity, message));
	}

	private static String formatMessage(String message, IJavaElement... relatedElementCollection) {
		Object[] elementNames = Arrays.stream(relatedElementCollection).parallel().filter(Objects::nonNull)
				.map(re -> getElementLabel(re, ALL_FULLY_QUALIFIED)).toArray();
		message = MessageFormat.format(message, elementNames);
		return message;
	}

	private static RefactoringStatusEntry addError(RefactoringStatus status, String message, IMember member,
			IMember... more) {
		List<String> elementNames = new ArrayList<>();
		elementNames.add(getElementLabel(member, ALL_FULLY_QUALIFIED));

		Stream<String> stream = Arrays.asList(more).parallelStream().map(m -> getElementLabel(m, ALL_FULLY_QUALIFIED));
		Stream<String> concat = Stream.concat(elementNames.stream(), stream);
		List<String> collect = concat.collect(Collectors.toList());

		status.addError(MessageFormat.format(message, collect.toArray()), JavaStatusContext.create(member));
		return getLastRefactoringStatusEntry(status);
	}

	protected static RefactoringStatus createWarning(String message, IMember member) {
		return createRefactoringStatus(message, member, RefactoringStatus::createWarningStatus);
	}

	private RefactoringStatus createError(String message, IMember member) {
		return createRefactoringStatus(message, member, RefactoringStatus::createErrorStatus);
	}

	protected static RefactoringStatus createFatalError(String message, IMember member) {
		return createRefactoringStatus(message, member, RefactoringStatus::createFatalErrorStatus);
	}

	private static RefactoringStatus createRefactoringStatus(String message, IMember member,
			BiFunction<String, RefactoringStatusContext, RefactoringStatus> function) {
		String elementName = getElementLabel(member, ALL_FULLY_QUALIFIED);
		return function.apply(MessageFormat.format(message, elementName), JavaStatusContext.create(member));
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
			monitor.beginTask(Messages.CheckingPreconditions, 1);
			// TODO ICompilationUnit unit =
			// getDeclaringType().getCompilationUnit();
			// if (fLayer)
			// unit = unit.findWorkingCopy(fOwner);
			// resetWorkingCopies(unit);
			return new RefactoringStatus();
		} finally {
			monitor.done();
		}
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(Messages.CheckingPreconditions, 12);
			// TODO: clearCaches();

			final RefactoringStatus status = new RefactoringStatus();

			// workaround https://bugs.eclipse.org/bugs/show_bug.cgi?id=474524.
			if (!this.getSourceMethods().isEmpty())
				status.merge(createWorkingCopyLayer(new SubProgressMonitor(monitor, 4)));
			if (status.hasFatalError())
				return status;
			if (monitor.isCanceled())
				throw new OperationCanceledException();

			status.merge(checkMethodsToMigrate(Optional.of(new SubProgressMonitor(monitor, 1))));
			if (status.hasFatalError())
				return status;
			if (monitor.isCanceled())
				throw new OperationCanceledException();

			// TODO: Should this be a separate method?
			status.merge(checkDestinationInterfaces(Optional.of(new SubProgressMonitor(monitor, 1))));
			if (status.hasFatalError())
				return status;

			// workaround https://bugs.eclipse.org/bugs/show_bug.cgi?id=474524.
			// if (fMembersToMove.length > 0)
			// TODO: Check project compliance.
			// status.merge(checkProjectCompliance(
			// getCompilationUnitRewrite(compilationUnitRewrites,
			// getDeclaringType().getCompilationUnit()),
			// getDestinationType(), fMembersToMove));

			// TODO: More checks, perhaps resembling those in
			// org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor.checkFinalConditions(IProgressMonitor,
			// CheckConditionsContext).

			// check if there are any methods left to migrate.
			if (this.getUnmigratableMethods().containsAll(this.getSourceMethods()))
				// if not, we have a fatal error.
				status.addFatalError(Messages.NoMethodsHavePassedThePreconditions);

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
		RefactoringStatus status = new RefactoringStatus();
		if (!JavaModelUtil.is50OrHigher(destination.getJavaProject())) {
			for (int index = 0; index < members.length; index++) {
				try {
					BodyDeclaration decl = ASTNodeSearchUtil.getBodyDeclarationNode(members[index],
							sourceRewriter.getRoot());
					if (decl != null) {
						for (@SuppressWarnings("unchecked")
						final Iterator<IExtendedModifier> iterator = decl.modifiers().iterator(); iterator.hasNext();) {
							boolean reported = false;
							final IExtendedModifier modifier = iterator.next();
							if (!reported && modifier.isAnnotation()) {
								status.merge(
										RefactoringStatus
												.createErrorStatus(
														MessageFormat
																.format(RefactoringCoreMessages.PullUpRefactoring_incompatible_langauge_constructs,
																		getTextLabel(members[index],
																				ALL_FULLY_QUALIFIED),
																		getTextLabel(destination, ALL_DEFAULT)),
										JavaStatusContext.create(members[index])));
								reported = true;
							}
						}
					}
				} catch (JavaModelException exception) {
					JavaPlugin.log(exception);
				}
				if (members[index] instanceof IMethod) {
					final IMethod method = (IMethod) members[index];
					try {
						if (Flags.isVarargs(method.getFlags()))
							status.merge(RefactoringStatus.createErrorStatus(
									MessageFormat.format(
											RefactoringCoreMessages.PullUpRefactoring_incompatible_language_constructs1,
											getTextLabel(members[index], ALL_FULLY_QUALIFIED),
											getTextLabel(destination, ALL_DEFAULT)),
									JavaStatusContext.create(members[index])));
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					}
				}
			}
		}

		if (!JavaModelUtil.is18OrHigher(destination.getJavaProject())) {
			Arrays.asList(members).stream().filter(e -> e instanceof IMethod).map(IMethod.class::cast)
					.filter(IMethod::isLambdaMethod)
					.forEach(m -> addError(status, Messages.IncompatibleLanguageConstruct, m, destination));
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {
			pm.beginTask(Messages.CreatingChange, 1);

			final TextEditBasedChangeManager manager = new TextEditBasedChangeManager();

			// the input methods as a set.
			Set<IMethod> methods = new HashSet<>(this.getSourceMethods().parallelStream()
					.filter(m -> m instanceof IMethod).map(m -> m).collect(Collectors.toSet()));

			// remove all the unmigratable methods.
			methods.removeAll(this.unmigratableMethods);

			if (methods.isEmpty())
				return new NullChange(Messages.NoMethodsToMigrate);

			for (IMethod sourceMethod : methods) {
				IType destinationInterface = getSourceMethodToTargetMethodMap().get(sourceMethod).getDeclaringType();

				logInfo("Migrating method: " + getElementLabel(sourceMethod, ALL_FULLY_QUALIFIED) + " to interface: "
						+ destinationInterface.getFullyQualifiedName());

				CompilationUnit destinationCompilationUnit = this.getCompilationUnit(destinationInterface.getTypeRoot(),
						pm);
				ASTRewrite destinationRewrite = getASTRewrite(destinationCompilationUnit);

				CompilationUnit sourceCompilationUnit = getCompilationUnit(sourceMethod.getTypeRoot(), pm);

				MethodDeclaration sourceMethodDeclaration = ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod,
						sourceCompilationUnit);
				logInfo("Source method declaration: " + sourceMethodDeclaration);

				// Find the target method.
				IMethod targetMethod = getSourceMethodToTargetMethodMap().get(sourceMethod);
				MethodDeclaration targetMethodDeclaration = ASTNodeSearchUtil.getMethodDeclarationNode(targetMethod,
						destinationCompilationUnit);

				// tack on the source method body to the target method.
				copyMethodBody(sourceMethodDeclaration, targetMethodDeclaration, destinationRewrite);

				// Change the target method to default.
				convertToDefault(targetMethodDeclaration, destinationRewrite);

				// TODO: Do we need to worry about preserving ordering of the
				// modifiers?

				// TODO: Should I be using JdtFlags instead of Flags?

				// if the source method is strictfp.
				// FIXME: Actually, I think we need to check that, in the
				// case the target method isn't already strictfp, that the other
				// methods in the hierarchy are.
				if ((Flags.isStrictfp(sourceMethod.getFlags())
						|| Flags.isStrictfp(sourceMethod.getDeclaringType().getFlags()))
						&& !Flags.isStrictfp(targetMethod.getFlags()))
					// change the target method to strictfp.
					convertToStrictFP(targetMethodDeclaration, destinationRewrite);

				// Remove the source method.
				ASTRewrite sourceRewrite = getASTRewrite(sourceCompilationUnit);
				removeMethod(sourceMethodDeclaration, sourceRewrite);
			}

			// TODO: Need to deal with imports #22.

			// save the source changes.
			ICompilationUnit[] units = this.typeRootToCompilationUnitMap.keySet().parallelStream()
					.filter(t -> t instanceof ICompilationUnit).map(t -> (ICompilationUnit) t)
					.filter(cu -> !manager.containsChangesIn(cu)).toArray(ICompilationUnit[]::new);

			for (ICompilationUnit cu : units)
				manageCompilationUnit(manager, cu, getASTRewrite(getCompilationUnit(cu, pm)));

			final Map<String, String> arguments = new HashMap<>();
			int flags = RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;

			// TODO: Fill in description.

			MigrateSkeletalImplementationToInterfaceRefactoringDescriptor descriptor = new MigrateSkeletalImplementationToInterfaceRefactoringDescriptor(
					null, "TODO", null, arguments, flags);

			return new DynamicValidationRefactoringChange(descriptor, getProcessorName(), manager.getAllChanges());
		} finally {
			pm.done();
		}
	}

	private CompilationUnit getCompilationUnit(ITypeRoot root, IProgressMonitor pm) {
		CompilationUnit compilationUnit = this.typeRootToCompilationUnitMap.get(root);
		if (compilationUnit == null) {
			compilationUnit = RefactoringASTParser.parseWithASTProvider(root, false, pm);
			this.typeRootToCompilationUnitMap.put(root, compilationUnit);
		}
		return compilationUnit;
	}

	private ASTRewrite getASTRewrite(CompilationUnit compilationUnit) {
		ASTRewrite rewrite = this.compilationUnitToASTRewriteMap.get(compilationUnit);
		if (rewrite == null) {
			rewrite = ASTRewrite.create(compilationUnit.getAST());
			this.compilationUnitToASTRewriteMap.put(compilationUnit, rewrite);
		}
		return rewrite;
	}

	private void manageCompilationUnit(final TextEditBasedChangeManager manager, ICompilationUnit compilationUnit,
			ASTRewrite rewrite) throws JavaModelException {
		TextEdit edit = rewrite.rewriteAST();

		TextChange change = (TextChange) manager.get(compilationUnit);
		change.setTextType("java");

		if (change.getEdit() == null)
			change.setEdit(edit);
		else
			change.addEdit(edit);

		manager.manage(compilationUnit, change);
	}

	private void copyMethodBody(MethodDeclaration sourceMethodDeclaration, MethodDeclaration targetMethodDeclaration,
			ASTRewrite destinationRewrite) {
		Block sourceMethodBody = sourceMethodDeclaration.getBody();
		Assert.isNotNull(sourceMethodBody, "Source method has a null body.");

		ASTNode sourceMethodBodyCopy = ASTNode.copySubtree(destinationRewrite.getAST(), sourceMethodBody);
		destinationRewrite.set(targetMethodDeclaration, MethodDeclaration.BODY_PROPERTY, sourceMethodBodyCopy, null);
	}

	private void removeMethod(MethodDeclaration methodDeclaration, ASTRewrite rewrite) {
		// TODO: Do I need an edit group??
		rewrite.remove(methodDeclaration, null);
	}

	private void convertToDefault(MethodDeclaration methodDeclaration, ASTRewrite rewrite) {
		addModifierKeyword(methodDeclaration, ModifierKeyword.DEFAULT_KEYWORD, rewrite);
	}

	private void convertToStrictFP(MethodDeclaration methodDeclaration, ASTRewrite rewrite) {
		addModifierKeyword(methodDeclaration, ModifierKeyword.STRICTFP_KEYWORD, rewrite);
	}

	private void addModifierKeyword(MethodDeclaration methodDeclaration, ModifierKeyword modifierKeyword,
			ASTRewrite rewrite) {
		Modifier modifier = rewrite.getAST().newModifier(modifierKeyword);
		ListRewrite listRewrite = rewrite.getListRewrite(methodDeclaration, methodDeclaration.getModifiersProperty());
		listRewrite.insertLast(modifier, null);
	}

	protected Map<IMethod, IMethod> getSourceMethodToTargetMethodMap() {
		return sourceMethodToTargetMethodMap;
	}

	/**
	 * Finds the target (interface) method declaration in the destination
	 * interface for the given source method.
	 * 
	 * TODO: Something is very wrong here. There can be multiple targets for a
	 * given source method because it can be declared in multiple interfaces up
	 * and down the hierarchy. What this method right now is really doing is
	 * finding the target method for the given source method in the destination
	 * interface. As such, we should be sure what the destination is prior to
	 * this call.
	 * 
	 * @param sourceMethod
	 *            The method that will be migrated to the target interface.
	 * @return The target method that will be manipulated or null if not found.
	 * @throws JavaModelException
	 */
	public static IMethod getTargetMethod(IMethod sourceMethod, Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		IType destinationInterface = getDestinationInterface(sourceMethod, monitor);

		if (sourceMethodTargetInterfaceTargetMethodTable.contains(sourceMethod, destinationInterface))
			return sourceMethodTargetInterfaceTargetMethodTable.get(sourceMethod, destinationInterface);
		else {
			if (destinationInterface == null)
				return null; // no target method in null destination interfaces.
			else {
				IMethod targetMethod = getTargetMethod(sourceMethod, destinationInterface);
				sourceMethodTargetInterfaceTargetMethodTable.put(sourceMethod, destinationInterface, targetMethod);
				return targetMethod;
			}
		}
	}

	private static IType getDestinationInterface(IMethod sourceMethod, Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		try {
			IType[] candidateDestinationInterfaces = getCandidateDestinationInterfaces(sourceMethod,
					monitor.map(m -> new SubProgressMonitor(m, 1)));

			// FIXME: Really just returning the first match here #23.
			return Arrays.stream(candidateDestinationInterfaces).findFirst().orElse(null);
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	/**
	 * Finds the target (interface) method declaration in the given type for the
	 * given source method.
	 * 
	 * @param sourceMethod
	 *            The method that will be migrated to the target interface.
	 * @param
	 * @return The target method that will be manipulated or null if not found.
	 */
	private static IMethod getTargetMethod(IMethod sourceMethod, IType targetInterface) {
		if (targetInterface == null)
			return null; // not found.

		IMethod[] methods = targetInterface.findMethods(sourceMethod);

		if (methods == null)
			return null; // not found.

		Assert.isTrue(methods.length <= 1,
				"Found multiple target methods for method: " + sourceMethod.getElementName());

		if (methods.length == 1)
			return methods[0];
		else
			return null; // not found.
	}

	private void log(int severity, String message) {
		String name = FrameworkUtil.getBundle(this.getClass()).getSymbolicName();
		IStatus status = new Status(severity, name, message);
		JavaPlugin.log(status);
	}

	private void logInfo(String message) {
		log(IStatus.INFO, message);
	}

	@SuppressWarnings("unused")
	private void logWarning(String message) {
		log(IStatus.WARNING, message);
	}

	@Override
	public String getIdentifier() {
		return MigrateSkeletalImplementationToInterfaceRefactoringDescriptor.REFACTORING_ID;
	}

	@Override
	public String getProcessorName() {
		return Messages.Name;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isInterfaceMigrationAvailable(getSourceMethods().parallelStream()
				.filter(m -> !this.unmigratableMethods.contains(m)).toArray(IMethod[]::new), Optional.empty());
	}

	/**
	 * Returns true if the given type is a pure interface, i.e., it is an
	 * interface but not an annotation.
	 * 
	 * @param type
	 *            The type to check.
	 * @return True if the given type is a pure interface and false otherwise.
	 * @throws JavaModelException
	 */
	private static boolean isPureInterface(IType type) throws JavaModelException {
		return type != null && type.isInterface() && !type.isAnnotation();
	}

	private Map<IType, ITypeHierarchy> typeToTypeHierarchyMap = new HashMap<>();

	protected Map<IType, ITypeHierarchy> getTypeToTypeHierarchyMap() {
		return typeToTypeHierarchyMap;
	}

	protected ITypeHierarchy getTypeHierarchy(IType type, Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		try {
			ITypeHierarchy ret = this.getTypeToTypeHierarchyMap().get(type);

			if (ret == null) {
				ret = type.newTypeHierarchy(monitor.orElseGet(NullProgressMonitor::new));
				this.getTypeToTypeHierarchyMap().put(type, ret);
			}

			return ret;
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants)
			throws CoreException {
		return new RefactoringParticipant[0];
	}
}