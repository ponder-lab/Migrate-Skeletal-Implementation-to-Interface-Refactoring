package edu.cuny.citytech.defaultrefactoring.core.refactorings;

import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;

@SuppressWarnings("restriction")
final class FieldAccessAnalysisSearchRequestor extends SearchRequestor {
	/**
	 *
	 */
	private final MigrateSkeletalImplementationToInterfaceRefactoringProcessor processor;
	private boolean accessesFieldsFromImplicitParameter;
	private final Optional<IProgressMonitor> monitor;

	FieldAccessAnalysisSearchRequestor(
			MigrateSkeletalImplementationToInterfaceRefactoringProcessor migrateSkeletalImplementationToInterfaceRefactoringProcessor,
			Optional<IProgressMonitor> monitor) {
		processor = migrateSkeletalImplementationToInterfaceRefactoringProcessor;
		this.monitor = monitor;
	}

	@SuppressWarnings("restriction")
	@Override
	public void acceptSearchMatch(SearchMatch match) throws CoreException {
		if (match.isInsideDocComment())
			return;

		// get the AST node corresponding to the field
		// access. It should be some kind of name
		// (simple of qualified).
		ASTNode node = ASTNodeSearchUtil.getAstNode(match,
				processor.getCompilationUnit(((IMember) match.getElement()).getTypeRoot(),
						new SubProgressMonitor(monitor.orElseGet(NullProgressMonitor::new), IProgressMonitor.UNKNOWN)));

		// examine the node's parent.
		ASTNode parent = node.getParent();

		switch (parent.getNodeType()) {
		case ASTNode.FIELD_ACCESS: {
			FieldAccess fieldAccess = (FieldAccess) parent;

			// the expression is the LHS of the
			// selection operator.
			Expression expression = fieldAccess.getExpression();

			if (expression == null || expression.getNodeType() == ASTNode.THIS_EXPRESSION)
				// either there is nothing on the LHS
				// or it's this, in which case we fail.
				this.accessesFieldsFromImplicitParameter = true;
			break;
		}
		case ASTNode.SUPER_FIELD_ACCESS: {
			// super will also tell us that it's an
			// instance field access of this.
			this.accessesFieldsFromImplicitParameter = true;
			break;
		}
		default: {
			// it must be an unqualified field access,
			// meaning that it's an instance field access of this.
			this.accessesFieldsFromImplicitParameter = true;
		}
		}
	}

	public boolean hasAccessesToFieldsFromImplicitParameter() {
		return accessesFieldsFromImplicitParameter;
	}
}