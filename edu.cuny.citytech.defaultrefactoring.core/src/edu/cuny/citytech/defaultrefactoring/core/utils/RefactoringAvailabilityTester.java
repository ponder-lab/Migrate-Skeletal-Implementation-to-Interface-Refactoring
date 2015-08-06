package edu.cuny.citytech.defaultrefactoring.core.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.Checks;

/**
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 * @see org.eclipse.jdt.internal.corext.refactoring.
 *      RefactoringAvailabilityTester
 */
@SuppressWarnings("restriction")
public final class RefactoringAvailabilityTester {

	private RefactoringAvailabilityTester() {
	}

	public static boolean isInterfaceMigrationAvailable(IMethod method) throws JavaModelException {
		if (!Checks.isAvailable(method))
			return false;
		if (method.isConstructor())
			return false;
		
        final IType declaring= method.getDeclaringType();
        if (declaring != null && declaring.isInterface())
                return false; // Method is already in an interface.

		return true;
	}

	public static IMethod[] getMigratableSkeletalImplementations(final IType type) throws JavaModelException {
		List<IMethod> ret = new ArrayList<>();

		if (type.exists()) {
			IMethod[] methodsOfType = type.getMethods();
			for (int i = 0; i < methodsOfType.length; i++) {
				IMethod method = methodsOfType[i];
				if (RefactoringAvailabilityTester.isInterfaceMigrationAvailable(method))
					ret.add(method);
			}
		}
		
		return ret.toArray(new IMethod[ret.size()]);
	}

}
