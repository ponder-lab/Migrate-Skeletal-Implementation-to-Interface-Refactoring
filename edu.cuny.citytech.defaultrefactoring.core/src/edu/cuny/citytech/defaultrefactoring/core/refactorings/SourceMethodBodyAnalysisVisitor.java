package edu.cuny.citytech.defaultrefactoring.core.refactorings;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;

import edu.cuny.citytech.defaultrefactoring.core.utils.Util;
import edu.cuny.citytech.refactoring.common.core.RefactoringProcessor;

@SuppressWarnings("restriction")
final class SourceMethodBodyAnalysisVisitor extends ASTVisitor {
	/**
	 *
	 */
	private final RefactoringProcessor processor;
	private Set<IMethod> calledProtectedObjectMethodSet = new HashSet<>();
	private boolean methodContainsCallToProtectedObjectMethod;
	private boolean methodContainsQualifiedThisExpression;
	private boolean methodContainsSuperReference;
	private boolean methodContainsTypeIncompatibleThisReference;
	private Optional<IProgressMonitor> monitor;
	private IMethod sourceMethod;

	public SourceMethodBodyAnalysisVisitor(
			RefactoringProcessor migrateSkeletalImplementationToInterfaceRefactoringProcessor,
			IMethod sourceMethod, Optional<IProgressMonitor> monitor) {
		super(false);
		processor = migrateSkeletalImplementationToInterfaceRefactoringProcessor;
		this.sourceMethod = sourceMethod;
		this.monitor = monitor;
	}

	protected boolean doesMethodContainQualifiedThisExpression() {
		return methodContainsQualifiedThisExpression;
	}

	protected boolean doesMethodContainsCallToProtectedObjectMethod() {
		return methodContainsCallToProtectedObjectMethod;
	}

	protected boolean doesMethodContainsSuperReference() {
		return methodContainsSuperReference;
	}

	protected boolean doesMethodContainsTypeIncompatibleThisReference() {
		return methodContainsTypeIncompatibleThisReference;
	}

	protected Set<IMethod> getCalledProtectedObjectMethodSet() {
		return calledProtectedObjectMethodSet;
	}

	@SuppressWarnings("restriction")
	private void process(ASTNode node, ThisExpression thisExpression) {
		switch (node.getNodeType()) {
		case ASTNode.METHOD_INVOCATION:
		case ASTNode.CLASS_INSTANCE_CREATION:
		case ASTNode.CONSTRUCTOR_INVOCATION:
			// NOTE: super isn't allowed inside the source method body.
		case ASTNode.ASSIGNMENT:
		case ASTNode.RETURN_STATEMENT:
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT: {
			// get the target method.
			IMethod targetMethod = null;
			try {
				targetMethod = MigrateSkeletalImplementationToInterfaceRefactoringProcessor.getTargetMethod(
						this.sourceMethod, this.monitor.map(m -> new SubProgressMonitor(m, IProgressMonitor.UNKNOWN)));
			} catch (JavaModelException e) {
				throw new RuntimeException(e);
			}
			IType destinationInterface = targetMethod.getDeclaringType();

			// get the destination interface.
			ITypeBinding destinationInterfaceTypeBinding = null;
			try {
				destinationInterfaceTypeBinding = ASTNodeSearchUtil
						.getTypeDeclarationNode(destinationInterface,
								processor.getCompilationUnit(destinationInterface.getTypeRoot(), new SubProgressMonitor(
										this.monitor.orElseGet(NullProgressMonitor::new), IProgressMonitor.UNKNOWN)))
						.resolveBinding();
			} catch (JavaModelException e) {
				throw new RuntimeException(e);
			}
			if (node.getNodeType() == ASTNode.CONSTRUCTOR_INVOCATION) {
				ConstructorInvocation constructorInvocation = (ConstructorInvocation) node;
				List<?> arguments = constructorInvocation.arguments();
				IMethodBinding methodBinding = constructorInvocation.resolveConstructorBinding();
				processArguments(arguments, methodBinding, thisExpression, destinationInterfaceTypeBinding);
			} else if (node.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
				ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) node;
				List<?> arguments = classInstanceCreation.arguments();
				IMethodBinding methodBinding = classInstanceCreation.resolveConstructorBinding();
				processArguments(arguments, methodBinding, thisExpression, destinationInterfaceTypeBinding);
			} else if (node.getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodInvocation = (MethodInvocation) node;
				List<?> arguments = methodInvocation.arguments();
				IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
				processArguments(arguments, methodBinding, thisExpression, destinationInterfaceTypeBinding);
			} else if (node.getNodeType() == ASTNode.ASSIGNMENT) {
				Assignment assignment = (Assignment) node;
				Expression leftHandSide = assignment.getLeftHandSide();
				Expression rightHandSide = assignment.getRightHandSide();
				processAssignment(assignment, thisExpression, destinationInterfaceTypeBinding, leftHandSide,
						rightHandSide);
			} else if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) node;
				Expression initializer = vdf.getInitializer();
				SimpleName name = vdf.getName();
				processAssignment(vdf, thisExpression, destinationInterfaceTypeBinding, name, initializer);
			} else if (node.getNodeType() == ASTNode.RETURN_STATEMENT) {
				ReturnStatement returnStatement = (ReturnStatement) node;

				// sanity check.
				Expression expression = returnStatement.getExpression();
				expression = (Expression) Util.stripParenthesizedExpressions(expression);
				Assert.isTrue(expression == thisExpression, "The return expression should be this.");

				MethodDeclaration targetMethodDeclaration = null;
				try {
					targetMethodDeclaration = ASTNodeSearchUtil.getMethodDeclarationNode(targetMethod,
							processor.getCompilationUnit(targetMethod.getTypeRoot(), new SubProgressMonitor(
									this.monitor.orElseGet(NullProgressMonitor::new), IProgressMonitor.UNKNOWN)));
				} catch (JavaModelException e) {
					throw new RuntimeException(e);
				}
				ITypeBinding returnType = targetMethodDeclaration.resolveBinding().getReturnType();

				// ensure that the destination type is assignment compatible
				// with the return type.
				if (!MigrateSkeletalImplementationToInterfaceRefactoringProcessor
						.isAssignmentCompatible(destinationInterfaceTypeBinding, returnType))
					this.methodContainsTypeIncompatibleThisReference = true;
			} else
				throw new IllegalStateException("Unexpected node type: " + node.getNodeType());
			break;
		}
		case ASTNode.PARENTHESIZED_EXPRESSION: {
			process(node.getParent(), thisExpression);
		}
		}
	}

	/**
	 * Process a list of arguments stemming from a method-like call.
	 *
	 * @param arguments
	 *            The list of arguments to process.
	 * @param methodBinding
	 *            The binding of the corresponding method call.
	 * @param thisExpression
	 *            The this expression we are looking for.
	 * @param destinationInterfaceTypeBinding
	 *            The binding of the destination interface.
	 */
	private void processArguments(List<?> arguments, IMethodBinding methodBinding, ThisExpression thisExpression,
			ITypeBinding destinationInterfaceTypeBinding) {
		// find where (or if) the this expression occurs in the
		// method
		// invocation arguments.
		for (int i = 0; i < arguments.size(); i++) {
			Object object = arguments.get(i);
			// if we are at the argument where this appears.
			if (object == thisExpression) {
				// get the type binding from the corresponding
				// parameter.
				ITypeBinding parameterTypeBinding;

				// varargs case.
				// if we are at or past the last parameter and the method is
				// vararg.
				if (i >= methodBinding.getParameterTypes().length - 1 && methodBinding.isVarargs()) {
					// assign the parameter type binding to be the scalar
					// type of the last parameter type.
					parameterTypeBinding = methodBinding.getParameterTypes()[methodBinding.getParameterTypes().length
							- 1].getElementType();
					Assert.isNotNull(parameterTypeBinding, "The last parameter of a vararg method should be an array.");
				} else
					parameterTypeBinding = methodBinding.getParameterTypes()[i];

				// the type of this will change to the destination
				// interface. Let's check whether an expression of
				// the destination type can be assigned to a
				// variable of
				// the parameter type.
				// TODO: Does `isAssignmentCompatible()` also work
				// with
				// comparison?
				if (!MigrateSkeletalImplementationToInterfaceRefactoringProcessor
						.isAssignmentCompatible(destinationInterfaceTypeBinding, parameterTypeBinding)) {
					this.methodContainsTypeIncompatibleThisReference = true;
					break;
				}
			}
		}
	}

	private void processAssignment(ASTNode node, ThisExpression thisExpression,
			ITypeBinding destinationInterfaceTypeBinding, Expression leftHandSide, Expression rightHandSide) {
		// if `this` appears on the LHS.
		if (leftHandSide == thisExpression) {
			// in this case, we need to check that the RHS can be
			// assigned to a variable of the destination type.
			if (!MigrateSkeletalImplementationToInterfaceRefactoringProcessor
					.isAssignmentCompatible(rightHandSide.resolveTypeBinding(), destinationInterfaceTypeBinding))
				this.methodContainsTypeIncompatibleThisReference = true;
		} else if (rightHandSide == thisExpression) {
			// otherwise, if `this` appears on the RHS. Then, we
			// need to check that the LHS can receive a variable of
			// the destination type.
			if (!MigrateSkeletalImplementationToInterfaceRefactoringProcessor
					.isAssignmentCompatible(destinationInterfaceTypeBinding, leftHandSide.resolveTypeBinding()))
				this.methodContainsTypeIncompatibleThisReference = true;
		} else
			throw new IllegalStateException(
					"this: " + thisExpression + " must appear either on the LHS or RHS of the assignment: " + node);
	}

	@Override
	public boolean visit(MethodInvocation node) {
		// check for calls to particular java.lang.Object
		// methods #144.
		IMethodBinding methodBinding = node.resolveMethodBinding();

		if (methodBinding != null && methodBinding.getDeclaringClass().getQualifiedName().equals("java.lang.Object")) {
			IMethod calledObjectMethod = (IMethod) methodBinding.getJavaElement();

			try {
				if (Flags.isProtected(calledObjectMethod.getFlags())) {
					this.methodContainsCallToProtectedObjectMethod = true;
					this.calledProtectedObjectMethodSet.add(calledObjectMethod);
				}
			} catch (JavaModelException e) {
				throw new RuntimeException(e);
			}
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperConstructorInvocation node) {
		this.methodContainsSuperReference = true;
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperFieldAccess node) {
		this.methodContainsSuperReference = true;
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		this.methodContainsSuperReference = true;
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperMethodReference node) {
		this.methodContainsSuperReference = true;
		return super.visit(node);
	}

	@Override
	public boolean visit(ThisExpression node) {
		// #153: Precondition missing for compile-time type of this
		// TODO: #153 There is actually a lot more checks we should add
		// here.
		/*
		 * TODO: Actually need to examine every kind of expression where `this`
		 * may appear. #149. Really, type constraints can (or should) be used
		 * for this. Actually, similar to enum problem, especially with finding
		 * the parameter from where the `this` expression came. Assignment is
		 * only one kind of expression, we need to also look at comparison and
		 * switches.
		 */
		if (node.getQualifier() != null)
			this.methodContainsQualifiedThisExpression = true;

		ASTNode parent = node.getParent();
		process(parent, node);
		return super.visit(node);
	}
}