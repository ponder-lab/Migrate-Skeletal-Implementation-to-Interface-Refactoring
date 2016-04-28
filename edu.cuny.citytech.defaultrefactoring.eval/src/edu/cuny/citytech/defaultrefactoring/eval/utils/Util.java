package edu.cuny.citytech.defaultrefactoring.eval.utils;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class Util {
	private Util() {
	}

	public static IJavaProject[] getSelectedJavaProjectsFromEvent(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelectionChecked(event);
	
		List<?> list = SelectionUtil.toList(currentSelection);
		IJavaProject[] javaProjects = list.stream().filter(e -> e instanceof IJavaProject)
				.toArray(length -> new IJavaProject[length]);
		return javaProjects;
	}

	public static String getMethodIdentifier(IMethod method) throws JavaModelException {
		StringBuilder sb = new StringBuilder();
		sb.append((method.getElementName()) + "(");
		ILocalVariable[] parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			sb.append(getParamString(parameters[i], method));
			if (i != (parameters.length - 1)) {
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	private static String getParamString(ILocalVariable parameterVariable, IMethod method) throws JavaModelException {
		IType declaringType = method.getDeclaringType();
		String name = parameterVariable.getTypeSignature();
		String simpleName = Signature.getSignatureSimpleName(name);
		String[][] allResults = declaringType.resolveType(simpleName);
		String fullName = null;
		if (allResults != null) {
			String[] nameParts = allResults[0];
			if (nameParts != null) {
				fullName = new String();
				for (int i = 0; i < nameParts.length; i++) {
					if (fullName.length() > 0) {
						fullName += '.';
					}
					String part = nameParts[i];
					if (part != null) {
						fullName += part;
					}
				}
			}
		} else
			fullName = simpleName;
		return fullName;
	}
}
