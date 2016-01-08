/**
 * 
 */
package edu.cuny.citytech.defaultrefactoring.ui.wizards;

import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.widgets.Shell;

import edu.cuny.citytech.defaultrefactoring.core.messages.Messages;
import edu.cuny.citytech.defaultrefactoring.core.utils.Util;

/**
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 *
 */
public class MigrateSkeletalImplementationToInterfaceRefactoringWizard extends RefactoringWizard {

	public MigrateSkeletalImplementationToInterfaceRefactoringWizard(Refactoring refactoring) {
		super(refactoring, RefactoringWizard.DIALOG_BASED_USER_INTERFACE);
		this.setWindowTitle(Messages.Name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	@Override
	protected void addUserInputPages() {
	}

	public static void startRefactoring(IMethod[] methods, Shell shell, Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		// TODO: Will need to set the target type at some point but see #23.
		Refactoring refactoring = Util.createRefactoring(methods, monitor);
		RefactoringWizard wizard = new MigrateSkeletalImplementationToInterfaceRefactoringWizard(refactoring);

		new RefactoringStarter().activate(wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring,
				RefactoringSaveHelper.SAVE_REFACTORING);
	}
}
