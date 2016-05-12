/**
 * 
 */
package edu.cuny.citytech.defaultrefactoring.core.utils;

import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import edu.cuny.citytech.defaultrefactoring.core.refactorings.MigrateSkeletalImplementationToInterfaceRefactoringProcessor;

/**
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 *
 */
@SuppressWarnings("restriction")
public final class Util {
	private Util() {
	}

	public static ProcessorBasedRefactoring createRefactoring(IJavaProject project, IMethod[] methods,
			Optional<IProgressMonitor> monitor) throws JavaModelException {
		MigrateSkeletalImplementationToInterfaceRefactoringProcessor processor = createMigrateSkeletalImplementationToInterfaceRefactoringProcessor(
				project, methods, monitor);
		return new ProcessorBasedRefactoring(processor);
	}

	public static MigrateSkeletalImplementationToInterfaceRefactoringProcessor createMigrateSkeletalImplementationToInterfaceRefactoringProcessor(
			IJavaProject project, IMethod[] methods, Optional<IProgressMonitor> monitor) throws JavaModelException {
		CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(project);
		MigrateSkeletalImplementationToInterfaceRefactoringProcessor processor = new MigrateSkeletalImplementationToInterfaceRefactoringProcessor(
				methods, settings, monitor);
		return processor;
	}

	public static ProcessorBasedRefactoring createRefactoring(IMethod[] methods, Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		IJavaProject project = null;

		if (methods != null && methods.length > 0)
			project = methods[0].getJavaProject();

		return createRefactoring(project, methods, monitor);
	}

	public static ProcessorBasedRefactoring createRefactoring(IMethod[] methods) throws JavaModelException {
		return createRefactoring(methods, Optional.empty());
	}

	public static ProcessorBasedRefactoring createRefactoring() throws JavaModelException {
		RefactoringProcessor processor = new MigrateSkeletalImplementationToInterfaceRefactoringProcessor();
		return new ProcessorBasedRefactoring(processor);
	}

	public static edu.cuny.citytech.refactoring.common.core.Refactoring createRefactoring(
			final Refactoring refactoring) {
		return new edu.cuny.citytech.refactoring.common.core.Refactoring() {

			public String getName() {
				return refactoring.getName();
			}

			public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
				return refactoring.createChange(pm);
			}

			public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
					throws CoreException, OperationCanceledException {
				return refactoring.checkInitialConditions(pm);
			}

			public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
					throws CoreException, OperationCanceledException {
				return refactoring.checkFinalConditions(pm);
			}
		};
	}
}
