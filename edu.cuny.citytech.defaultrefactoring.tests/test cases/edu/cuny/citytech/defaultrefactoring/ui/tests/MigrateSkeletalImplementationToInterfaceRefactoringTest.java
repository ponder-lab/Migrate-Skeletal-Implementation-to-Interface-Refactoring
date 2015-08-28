/**
 * 
 */
package edu.cuny.citytech.defaultrefactoring.ui.tests;

import java.util.logging.Logger;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.tests.refactoring.Java18Setup;
import org.eclipse.ltk.core.refactoring.Refactoring;

import edu.cuny.citytech.defaultrefactoring.core.utils.Util;
import edu.cuny.citytech.refactoring.common.tests.RefactoringTest;
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

	/**
	 * @return the refactoringPath
	 */
	@Override
	public String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	protected Refactoring getRefactoring(IMethod... methods) throws JavaModelException {
		return Util.createRefactoring(methods);
	}

	protected Logger getLogger() {
		return logger;
	}

	private void helperFailLambdaMethod(String typeName, String lambdaExpression) throws Exception {
		ICompilationUnit cu = createCUfromTestFile(getPackageP(), typeName);
		IBuffer buffer = cu.getBuffer();
		String contents = buffer.getContents();
		int start = contents.indexOf(lambdaExpression);
		IJavaElement[] elements = cu.codeSelect(start, 1);

		assertEquals("Incorrect no of elements", 1, elements.length);
		IJavaElement element = elements[0];

		assertEquals("Incorrect element type", IJavaElement.LOCAL_VARIABLE, element.getElementType());

		IMethod method = (IMethod) element.getParent();
		assertFailedPrecondition(method);

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

	public void testLambdaMethod() throws Exception {
		helperFailLambdaMethod("A", "x) -> {}");
	}

	public void testMethodContainedInInterface() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodContainedInAnonymousType() throws Exception {
		helperFail("m", new String[] {}, new String[] { "n" }, new String[][] { new String[0] });
	}

	public void testMethodContainedInEnum() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodDeclaredInLocalType() throws Exception {
		helperFail("m", new String[] {}, "B", new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodDeclaredInMemberType() throws Exception {
		helperFail("B", new String[] { "m" }, new String[][] { new String[0] });
	}

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

	public void testMethodDeclaredInTypeThatImplementsInterfaceWithSuperInterfaces() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodDeclaredInTypeThatImplementsInterfaceWithSuperInterfaces2() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodThatThrowsAnException() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodDeclaredInConcreteType() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodDeclaredInStaticType() throws Exception {
		helperFail("B", new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodWithParameters() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[] { Signature.SIG_INT } });
	}

	public void testMethodWithReturnType() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodWithTypeParameters() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMethodWithStatements() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testNoMethods() throws Exception {
		helperFail();
	}

	public void testMultipleMethods() throws Exception {
		helperFail(new String[] { "m", "n" }, new String[][] { new String[0], new String[0] });
	}

	public void testTargetInterfaceWithMultipleMethods() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testTargetInterfaceWithNoMethods() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testPureTargetInterface() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testTargetInterfaceWithNoTargetMethods() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDefaultTargetMethod() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceWithAnnotations() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testNonTopLevelDestinationInterface() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testNonTopLevelDestinationInterface2() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceWithFields() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceThatExtendsInterface() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceWithTypeParameters() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceWithMemberTypes() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMemberDestinationInterface() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMemberDestinationInterface2() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceHierarchyWithInvalidClass() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceHierarchyWithInvalidClass2() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceHierarchyWithInvalidClass3() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceHierarchyWithInvalidClass4() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}
	
	public void testDestinationInterfaceHierarchyWithInvalidClass5() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceHierarchyWithInvalidInterface() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceHierarchyWithInvalidInterface2() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceHierarchyWithInvalidInterface3() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMultipleDestinationInterfaces() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMultipleDestinationInterfaces2() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testMultipleDestinationInterfaces3() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceHierarchyWithSubtype() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceHierarchyWithSuperInterface() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceWithExtendingInterface() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void testDestinationInterfaceWithInvalidImplementingClass() throws Exception {
		helperFail(new String[] { "m" }, new String[][] { new String[0] });
	}
}