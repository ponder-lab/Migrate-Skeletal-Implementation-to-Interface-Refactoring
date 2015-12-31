package edu.cuny.citytech.defaultrefactoring.eval.handlers;

import java.io.FileWriter;
import java.io.IOException;
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
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
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
			FileWriter abstract_classesWriter = new FileWriter("abstract_classes.csv");
			FileWriter interfacesWriter = new FileWriter("interfaces.csv");
			FileWriter classes_implementing_interfacesWriter = new FileWriter("classes_implementing_interfaces.csv");

			// getting the csv file header
			String[] typesHeader = { "Project Name", ",", "CompilationUnit", ",", "Fully Qualified Name" };
			String[] classesHeader = { "Fully Qualified Name" };
			String[] abstract_classesHeder = { "Fully Qualified Name" };
			String[] interfacesHeder = { "Fully Qualified Name" };
			String[] classes_implementing_interfacesHeder = { "Class FQN", ",", "Interface FQN" };

			csvHeader(typesWriter, typesHeader);
			csvHeader(classesWriter, classesHeader);
			csvHeader(abstract_classesWriter, abstract_classesHeder);
			csvHeader(interfacesWriter, interfacesHeder);
			csvHeader(classes_implementing_interfacesWriter, classes_implementing_interfacesHeder);

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
									abstract_classesWriter.append(type.getFullyQualifiedName());
									abstract_classesWriter.append('\n');
								}
							}
							
							// getting all the implemented interface
							ITypeHierarchy typeHierarchy = type.newTypeHierarchy(new NullProgressMonitor());
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

									classes_implementing_interfacesWriter.append(type.getFullyQualifiedName());
									classes_implementing_interfacesWriter.append(",");
									classes_implementing_interfacesWriter.append(superInterface.getFullyQualifiedName() + " ");
									classes_implementing_interfacesWriter.append('\n');
								}
							}
						}
					}
				}
			}

			// closing the files writer after done writing
			typesWriter.close();
			classesWriter.close();
			abstract_classesWriter.close();
			interfacesWriter.close();
			classes_implementing_interfacesWriter.close();
		} catch (JavaModelException | IOException fileException) {
			JavaPlugin.log(fileException);
		}
		return null;
	}

	private static void writeType(FileWriter typesWriter, IType type) throws IOException {
		typesWriter.append(Optional.ofNullable(type.getJavaProject()).map(IJavaElement::getElementName).orElse("NULL"));
		typesWriter.append(',');
		typesWriter.append(Optional.ofNullable(type.getCompilationUnit()).map(IJavaElement::getElementName).orElse("NULL"));
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