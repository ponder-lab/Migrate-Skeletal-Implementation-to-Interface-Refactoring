/**
 * 
 */
package edu.cuny.citytech.defaultrefactoring.ui.wizards;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.widgets.Shell;

import edu.cuny.citytech.defaultrefactoring.ui.refactorings.MigrateSkeletalImplementationToInterfaceRefactoring;

/**
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 *
 */
public class MigrateSkeletalImplementationToInterfaceRefactoringWizard extends
		RefactoringWizard {

	public MigrateSkeletalImplementationToInterfaceRefactoringWizard(
			Refactoring refactoring) {
		super(refactoring, RefactoringWizard.DIALOG_BASED_USER_INTERFACE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	@Override
	protected void addUserInputPages() {
	}

	public static void startRefactoring(IMethod[] methods, Shell shell) {
		Refactoring refactoring = new MigrateSkeletalImplementationToInterfaceRefactoring(
				methods);
		MigrateSkeletalImplementationToInterfaceRefactoringWizard wizard = new MigrateSkeletalImplementationToInterfaceRefactoringWizard(
				refactoring);
		new RefactoringStarter().activate(wizard, shell,
				RefactoringMessages.OpenRefactoringWizardAction_refactoring,
				RefactoringSaveHelper.SAVE_REFACTORING);
	}
}
