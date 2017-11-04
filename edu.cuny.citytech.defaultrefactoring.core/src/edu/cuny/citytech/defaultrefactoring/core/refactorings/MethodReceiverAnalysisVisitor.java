package edu.cuny.citytech.defaultrefactoring.core.refactorings;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import edu.cuny.citytech.defaultrefactoring.core.utils.Util;

final class MethodReceiverAnalysisVisitor extends ASTVisitor {
	private IMethod accessedMethod;
	private boolean encounteredThisReceiver;

	MethodReceiverAnalysisVisitor(IMethod accessedMethod) {
		this.accessedMethod = accessedMethod;
	}

	public boolean hasEncounteredThisReceiver() {
		return encounteredThisReceiver;
	}

	@Override
	public boolean visit(MethodInvocation methodInvocation) {
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();

		if (methodBinding != null) {
			IJavaElement javaElement = methodBinding.getJavaElement();

			if (javaElement == null)
				MigrateSkeletalImplementationToInterfaceRefactoringProcessor
						.logWarning("Could not get Java element from binding: " + methodBinding + " while processing: "
								+ methodInvocation);
			else if (javaElement.equals(accessedMethod)) {
				Expression expression = methodInvocation.getExpression();
				expression = (Expression) Util.stripParenthesizedExpressions(expression);

				// FIXME: It's not really that the expression is a `this`
				// expression but that the type of the expression comes from
				// a
				// `this` expression. In other words, we may need to climb
				// the
				// AST.
				if (expression == null || expression.getNodeType() == ASTNode.THIS_EXPRESSION)
					this.encounteredThisReceiver = true;
			}
		}
		return super.visit(methodInvocation);
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		IMethodBinding methodBinding = node.resolveMethodBinding();
		if (methodBinding != null)
			if (methodBinding.getJavaElement().equals(accessedMethod))
				this.encounteredThisReceiver = true;
		return super.visit(node);
	}
}