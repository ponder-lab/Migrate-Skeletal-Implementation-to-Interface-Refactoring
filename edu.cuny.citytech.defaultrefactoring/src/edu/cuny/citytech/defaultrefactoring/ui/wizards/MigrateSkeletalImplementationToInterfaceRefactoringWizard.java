/**
 * 
 */
package edu.cuny.citytech.defaultrefactoring.ui.wizards;

import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.widgets.Shell;

/**
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi Khatchadourian</a>
 *
 */
public class MigrateSkeletalImplementationToInterfaceRefactoringWizard extends
		RefactoringWizard {

	/**
	 * @param refactoring
	 * @param flags
	 */
	public MigrateSkeletalImplementationToInterfaceRefactoringWizard(
			Refactoring refactoring, int flags) {
		super(refactoring, flags);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	@Override
	protected void addUserInputPages() {
		// TODO Auto-generated method stub

	}

	/**
	 * @param types
	 * @param shell
	 */
	public static void startRefactoring(IType[] types, Shell shell) {
		// TODO Auto-generated method stub
		
	}

}
