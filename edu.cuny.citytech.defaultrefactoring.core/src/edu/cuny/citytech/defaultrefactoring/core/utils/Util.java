/**
 * 
 */
package edu.cuny.citytech.defaultrefactoring.core.utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import edu.cuny.citytech.defaultrefactoring.core.refactorings.MigrateSkeletalImplementationToInterfaceRefactoring;

/**
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi Khatchadourian</a>
 *
 */
@SuppressWarnings("restriction")
public final class Util {
	private Util() {
	}
	
	public static Refactoring createRefactoring(IJavaProject project, IMethod[] methods) {
		CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(project);
		RefactoringProcessor processor = new MigrateSkeletalImplementationToInterfaceRefactoring(methods, settings);
		return new ProcessorBasedRefactoring(processor);
	}

	public static Refactoring createRefactoring(IMethod[] methods) {
		IJavaProject project = null;
		
		if (methods != null && methods.length > 0)
			project = methods[0].getJavaProject();
		
		return createRefactoring(project, methods);
	}
	
	public static Refactoring createRefactoring() {
		RefactoringProcessor processor = new MigrateSkeletalImplementationToInterfaceRefactoring();
		return new ProcessorBasedRefactoring(processor);
	}

	public static edu.cuny.citytech.refactoring.common.core.Refactoring createRefactoring(final Refactoring refactoring) {
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
