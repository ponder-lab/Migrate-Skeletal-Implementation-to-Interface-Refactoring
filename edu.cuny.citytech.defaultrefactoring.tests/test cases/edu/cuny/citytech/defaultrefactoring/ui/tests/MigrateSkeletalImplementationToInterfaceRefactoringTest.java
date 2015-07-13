/**
 * 
 */
package edu.cuny.citytech.defaultrefactoring.ui.tests;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.tests.refactoring.Java18Setup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.citytech.defaultrefactoring.core.refactorings.MigrateSkeletalImplementationToInterfaceRefactoring;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 *
 */
@SuppressWarnings("restriction")
public class MigrateSkeletalImplementationToInterfaceRefactoringTest extends RefactoringTest {

	private static final Class<MigrateSkeletalImplementationToInterfaceRefactoringTest> clazz = MigrateSkeletalImplementationToInterfaceRefactoringTest.class;

	private static final Logger logger = Logger.getLogger(clazz.getName());

	private static final String REFACTORING_PATH = "MigrateSkeletalImplementationToInterface/";

	/**
	 * The name of the directory containing resources under the project
	 * directory.
	 */
	private static final String RESOURCE_PATH = "resources";

	/**
	 * @param testSuite
	 * @return
	 */
	public static Test setUpTest(Test test) {
		return new Java18Setup(test);
	}

	/**
	 * @return
	 */
	public static Test suite() {
		return setUpTest(new TestSuite(clazz));
	}

	public MigrateSkeletalImplementationToInterfaceRefactoringTest(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.ui.tests.refactoring.RefactoringTest#getFileContents(java
	 * .lang.String) Had to override this method because, since this plug-in is
	 * a fragment (at least I think that this is the reason), it doesn't have an
	 * activator and the bundle is resolving to the eclipse refactoring test
	 * bundle.
	 */
	@Override
	public String getFileContents(String fileName) throws IOException {
		Path path = Paths.get(RESOURCE_PATH, fileName);
		Path absolutePath = path.toAbsolutePath();
		byte[] encoded = Files.readAllBytes(absolutePath);
		return new String(encoded, Charset.defaultCharset());
	}

	/**
	 * @return the refactoringPath
	 */
	@Override
	public String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void helperPass(String[] methodNames, String[][] signatures) throws Exception {
		ICompilationUnit cu = createCUfromTestFile(getPackageP(), "A");
		IType type = getType(cu, "A");
		IMethod[] methods = getMethods(type, methodNames, signatures);

		MigrateSkeletalImplementationToInterfaceRefactoring refactoring = new MigrateSkeletalImplementationToInterfaceRefactoring(
				methods);

		RefactoringStatus initialStatus = refactoring.checkInitialConditions(new NullProgressMonitor());
		logger.info("Initial status: " + initialStatus);

		RefactoringStatus finalStatus = refactoring.checkFinalConditions(new NullProgressMonitor());
		logger.info("Final status: " + finalStatus);

		assertTrue("Precondition was supposed to pass.", initialStatus.isOK() && finalStatus.isOK());
		performChange(refactoring, false);

		String expected = getFileContents(getOutputTestFileName("A"));
		String actual = cu.getSource();
		assertEqualLines(expected, actual);
	}

	private void helperFail(String[] methodNames, String[][] signatures) throws Exception {
		helperFail("A", methodNames, signatures);
	}

	private void helperFail(String typeName, String[] methodNames, String[][] signatures) throws Exception {
		ICompilationUnit cu = createCUfromTestFile(getPackageP(), typeName);
		IType type = getType(cu, typeName);
		IMethod[] methods = getMethods(type, methodNames, signatures);

		MigrateSkeletalImplementationToInterfaceRefactoring refactoring = new MigrateSkeletalImplementationToInterfaceRefactoring(
				methods);

		RefactoringStatus initialStatus = refactoring.checkInitialConditions(new NullProgressMonitor());
		logger.info("Initial status: " + initialStatus);

		RefactoringStatus finalStatus = refactoring.checkFinalConditions(new NullProgressMonitor());
		logger.info("Final status: " + finalStatus);

		assertTrue("Precondition was supposed to fail.", !initialStatus.isOK() || !finalStatus.isOK());
	}

	public void testConstructor() throws Exception {
		helperFail(new String[] { "A" }, new String[][] { new String[0] });
	}

	public void testAnnotatedMethod() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testStaticMethod() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	// TODO I have no idea how to create a test file for this.
	// public void testLambdaMethod() throws Exception {
	// helperFail(new String[] { "m" }, new String[][] { new String[0] });
	// }

	public void testPlainMethod() throws Exception {
		helperPass(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodContainedInInterface() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	// TODO This helper only tests top-level types.
	// public void testMethodContainedInAnonymousType() throws Exception {
	// helperFail("A.1", new String[] { "n" }, new String[][] { new String[0]
	// });
	// }

	public void testMethodContainedInEnum() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	// TODO No idea how to create a case for this.
	// public void testMethodContainedInLambdas() throws Exception {
	// helperFail(new String[] { "m" }, new String[][] { new String[0] });
	// }

	// TODO This helper only tests top-level types.
	// public void testMethodContainedInLocalType() throws Exception {
	// helperFail(new String[] { "m" }, new String[][] { new String[0] });
	// }

	// TODO This helper only tests top-level types.
	// public void testMethodContainedInMemberType() throws Exception {
	// helperFail(new String[] { "m" }, new String[][] { new String[0] });
	// }

	public void testMethodContainedInAnnotation() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodContainedInAnnotatedType() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodContainedInTypeWithField() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodContainedInTypeWithInitializer() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodContainedInTypeWithMoreThanOneMethod() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodContainedInTypeWithTypeParameters() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodContainedInTypeWithSuperTypes() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodContainedInTypeThatImplementsMultipleInterfaces() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodThatThrowsAnException() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodDeclaredInConcreteType() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	// TODO I'll need an inner type helper for this.
	// public void testMethodDeclaredInStaticType() throws Exception {
	// helperFail(new String[] { "m" }, new String[][] { new String[0] });
	// }

	public void testMethodWithParameters() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[] { Signature.SIG_INT } });
	}

	public void testMethodWithReturnType() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodWithTypeParameters() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

}
