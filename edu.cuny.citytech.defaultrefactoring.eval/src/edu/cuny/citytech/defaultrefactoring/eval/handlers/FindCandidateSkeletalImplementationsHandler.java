package edu.cuny.citytech.defaultrefactoring.eval.handlers;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class FindCandidateSkeletalImplementationsHandler extends AbstractHandler {

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelectionChecked(event);

		List<?> list = SelectionUtil.toList(currentSelection);
		IJavaProject[] javaProjects = list.stream().filter(e -> e instanceof IJavaProject)
				.toArray(length -> new IJavaProject[length]);

		try {

			// opening 5 separate files
			FileWriter typesWriter = new FileWriter("types.csv");
			FileWriter classesWriter = new FileWriter("classes.csv");
			FileWriter abstractClassesWriter = new FileWriter("abstract_classes.csv");
			FileWriter interfacesWriter = new FileWriter("interfaces.csv");
			FileWriter classesImplementingInterfacesWriter = new FileWriter("classes_implementing_interfaces.csv");
			FileWriter classesExtendingClassesWriter = new FileWriter("classes_extending_classes.csv");

			// getting the csv file header
			String[] typesHeader = { "Project Name", ",", "CompilationUnit", ",", "Fully Qualified Name" };
			String[] classesHeader = { "Fully Qualified Name" };
			String[] abstractClassesHeader = { "Fully Qualified Name" };
			String[] interfacesHeader = { "Fully Qualified Name" };
			String[] classesImplementing_interfacesHeder = { "Class FQN", ",", "Interface FQN" };
			String[] classesExtendingClassesHeader = { "Source Class FQN", ",", "Target Class FQN" };

			csvHeader(typesWriter, typesHeader);
			csvHeader(classesWriter, classesHeader);
			csvHeader(abstractClassesWriter, abstractClassesHeader);
			csvHeader(interfacesWriter, interfacesHeader);
			csvHeader(classesImplementingInterfacesWriter, classesImplementing_interfacesHeder);
			csvHeader(classesExtendingClassesWriter, classesExtendingClassesHeader);

			for (IJavaProject iJavaProject : javaProjects) {
				IPackageFragment[] packageFragments = iJavaProject.getPackageFragments();
				for (IPackageFragment iPackageFragment : packageFragments) {
					ICompilationUnit[] compilationUnits = iPackageFragment.getCompilationUnits();
					for (ICompilationUnit iCompilationUnit : compilationUnits) {
						// printing the iCompilationUnit,
						IType[] allTypes = iCompilationUnit.getAllTypes();
						for (IType type : allTypes) {

							writeType(typesWriter, type);

							// getting the class name that are not abstract and
							// not include enum
							if (type.isClass() && !(type.isEnum())) {
								classesWriter.append(type.getFullyQualifiedName());
								classesWriter.append('\n');

								// checking if the class is abstract
								if (Flags.isAbstract(type.getFlags())) {
									abstractClassesWriter.append(type.getFullyQualifiedName());
									abstractClassesWriter.append('\n');

									IMethod[] methods = type.getMethods();
									for (int x = 0; x < methods.length; x++) {
										System.out.print(methods[x].getElementName() + "(");
										ILocalVariable[] parameters = methods[x].getParameters();
										for (int i = 0; i < parameters.length; i++) {
											System.out.print(getParamString(parameters[i], methods[i]));
											if( i != (parameters.length - 1)){
											System.out.print(",");}
										}
										System.out.println(")");
									}

								}
							}

							ITypeHierarchy typeHierarchy = type.newTypeHierarchy(new NullProgressMonitor());

							// get all super classes of this type.
							for (IType superClass : typeHierarchy.getAllSuperclasses(type))
								if (superClass.isClass()) { // just to be sure.
									// write out the super class to the types
									// file.
									writeType(typesWriter, superClass);

									// write out the relation.
									classesExtendingClassesWriter.append(type.getFullyQualifiedName());
									classesExtendingClassesWriter.append(",");
									classesExtendingClassesWriter.append(superClass.getFullyQualifiedName());
									classesExtendingClassesWriter.append("\n");
								}

							// getting all the implemented interface
							IType[] allSuperInterfaces = typeHierarchy.getAllSuperInterfaces(type);

							// getting all the interface full qualified name
							if (type.isInterface()) {
								// write this interface.
								interfacesWriter.append(type.getFullyQualifiedName());
								interfacesWriter.append('\n');															
								
							}

							if (type.isClass() && !(type.isEnum()) && allSuperInterfaces.length >= 1) {
								for (IType superInterface : allSuperInterfaces) {
									writeType(typesWriter, superInterface);

									interfacesWriter.append(superInterface.getFullyQualifiedName());
									interfacesWriter.append('\n');

									classesImplementingInterfacesWriter.append(type.getFullyQualifiedName());
									classesImplementingInterfacesWriter.append(",");
									classesImplementingInterfacesWriter
											.append(superInterface.getFullyQualifiedName() + " ");
									classesImplementingInterfacesWriter.append('\n');
									
								}
							}
						}
					}
				}
			}

			// closing the files writer after done writing
			typesWriter.close();
			classesWriter.close();
			abstractClassesWriter.close();
			interfacesWriter.close();
			classesImplementingInterfacesWriter.close();
			classesExtendingClassesWriter.close();
		} catch (JavaModelException | IOException fileException) {
			JavaPlugin.log(fileException);
		}
		return null;
	}

	private static String getParamString(ILocalVariable parameterVariable, IMethod method) throws JavaModelException {
		IType declaringType = method.getDeclaringType();
		String name = parameterVariable.getTypeSignature();
		String simpleName = Signature.getSignatureSimpleName(name);
		String[][] allResults = declaringType.resolveType(simpleName);
		String fullName = null;
		if (allResults != null) {
			String[] nameParts = allResults[0];
			if (nameParts != null) {
				fullName = new String();
				for (int i = 0; i < nameParts.length; i++) {
					if (fullName.length() > 0) {
						fullName += '.';
					}
					String part = nameParts[i];
					if (part != null) {
						fullName += part;
					}
				}
			}
		}
		else
			fullName = simpleName;
		return fullName;
	}

	private static void writeType(FileWriter typesWriter, IType type) throws IOException {
		typesWriter.append(Optional.ofNullable(type.getJavaProject()).map(IJavaElement::getElementName).orElse("NULL"));
		typesWriter.append(',');
		typesWriter.append(
				Optional.ofNullable(type.getCompilationUnit()).map(IJavaElement::getElementName).orElse("NULL"));
		typesWriter.append(',');
		typesWriter.append(type.getFullyQualifiedName());
		typesWriter.append('\n');
	}

	/**
	 * this method create header for the csv file
	 * 
	 * @param typesWriter
	 * @param typesHeader
	 * @throws IOException
	 */
	protected void csvHeader(FileWriter typesWriter, String[] typesHeader) throws IOException {
		for (int i = 0; i < typesHeader.length; i++) {
			typesWriter.append(typesHeader[i]);
		}
		typesWriter.append('\n');
	}
}