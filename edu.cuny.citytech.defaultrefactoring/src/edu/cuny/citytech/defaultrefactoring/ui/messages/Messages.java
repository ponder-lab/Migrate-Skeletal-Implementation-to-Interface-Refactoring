/**
 * 
 */
package edu.cuny.citytech.defaultrefactoring.ui.messages;

import org.eclipse.osgi.util.NLS;

/**
 * @author raffi
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "edu.cuny.citytech.defaultrefactoring.ui.messages.messages"; //$NON-NLS-1$

	public static String MigrateSkeletalImplementationToInferfaceRefactoring_MethodsNotSpecified;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_Name;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_CheckingPreconditions;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_CompilingSource;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_CreatingChange;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_CUContainsCompileErrors;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_MethodDoesNotExist;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_PreconditionFailed;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_RefactoringNotPossible;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_WrongType;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_CantChangeMethod;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_NoConstructors;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_NoAnnotations;
	public static String MigrateSkeletalImplementationToInferfaceRefactoring_NoStaticMethods;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		super();
	}
}
