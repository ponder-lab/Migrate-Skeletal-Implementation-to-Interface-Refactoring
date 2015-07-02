/**
 * 
 */
package edu.cuny.citytech.defaultrefactoring.ui.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.tests.refactoring.Java18Setup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.citytech.defaultrefactoring.core.refactorings.MigrateSkeletalImplementationToInterfaceRefactoring;

/**
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 *
 */
@SuppressWarnings("restriction")
public class MigrateSkeletalImplementationToInterfaceRefactoringTest extends
		RefactoringTest {

	/**
	 * 
	 */
	private static final String RESOURCE_DIRECTORY_NAME = "resources";

	private static final Class<MigrateSkeletalImplementationToInterfaceRefactoringTest> clazz = MigrateSkeletalImplementationToInterfaceRefactoringTest.class;

	private static final Logger logger = Logger.getLogger(clazz.getName());

	private static final String REFACTORING_PATH = "MigrateSkeletalImplementationToInterface/";

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

	@Override
	public String getFileContents(String fileName) throws IOException {
		Path path = Paths.get(RESOURCE_DIRECTORY_NAME, fileName);
		Path absolutePath = path.toAbsolutePath();
		return Files.lines(absolutePath).collect(Collectors.joining());
	}

	/**
	 * @return the refactoringPath
	 */
	@Override
	public String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void helper(String[] methodNames, String[][] signatures)
			throws Exception {
		ICompilationUnit cu = createCUfromTestFile(getPackageP(), "A");
		IType type = getType(cu, "A");
		IMethod[] methods = getMethods(type, methodNames, signatures);

		MigrateSkeletalImplementationToInterfaceRefactoring refactoring = new MigrateSkeletalImplementationToInterfaceRefactoring(
				methods);

		RefactoringStatus initialStatus = refactoring
				.checkInitialConditions(new NullProgressMonitor());
		logger.info("Initial status: " + initialStatus);

		RefactoringStatus finalStatus = refactoring
				.checkFinalConditions(new NullProgressMonitor());
		logger.info("Final status: " + finalStatus);

		assertTrue("Precondition was supposed to fail.", !initialStatus.isOK()
				|| !finalStatus.isOK());
	}

	public void testFail0() throws Exception {
		helper(new String[] { "A" }, new String[][] { new String[0] });
	}
}
