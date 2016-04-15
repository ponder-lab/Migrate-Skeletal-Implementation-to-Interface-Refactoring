package edu.cuny.citytech.defaultrefactoring.eval.handlers;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
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
			FileWriter abstractClassesWriter = new FileWriter("abstract_classes.csv");
			FileWriter interfacesWriter = new FileWriter("interfaces.csv");
			FileWriter classesImplementingInterfacesWriter = new FileWriter("classes_implementing_interfaces.csv");
			FileWriter classesExtendingClassesWriter = new FileWriter("classes_extending_classes.csv");
			FileWriter methodsWriter = new FileWriter("methods.csv");

			// getting the csv file header
			String[] typesHeader = { "Project Name", "CompilationUnit", "Fully Qualified Name" };
			String[] classesHeader = { "Fully Qualified Name" };
			String[] abstractClassesHeader = { "Fully Qualified Name" };
			String[] interfacesHeader = { "Fully Qualified Name" };
			String[] classesImplementing_interfacesHeder = { "Class FQN", "Interface FQN" };
			String[] classesExtendingClassesHeader = { "Source Class FQN", "Target Class FQN" };
			String[] methodsHeader = { "Method Identifier", "Declaring Type FQN" };

			CSVPrinter typesPrinter = new CSVPrinter(typesWriter, CSVFormat.EXCEL.withHeader(typesHeader));
			CSVPrinter classesPrinter = new CSVPrinter(classesWriter, CSVFormat.EXCEL.withHeader(classesHeader));
			CSVPrinter abstractClassesPrinter = new CSVPrinter(abstractClassesWriter,
					CSVFormat.EXCEL.withHeader(abstractClassesHeader));
			CSVPrinter interfacesPrinter = new CSVPrinter(interfacesWriter,
					CSVFormat.EXCEL.withHeader(interfacesHeader));
			CSVPrinter classesImplementingInterfacesPrinter = new CSVPrinter(classesImplementingInterfacesWriter,
					CSVFormat.EXCEL.withHeader(classesImplementing_interfacesHeder));
			CSVPrinter classesExtendingClassesPrinter = new CSVPrinter(classesExtendingClassesWriter,
					CSVFormat.EXCEL.withHeader(classesExtendingClassesHeader));
			CSVPrinter metPrinter = new CSVPrinter(methodsWriter, CSVFormat.EXCEL.withHeader(methodsHeader));

			for (IJavaProject iJavaProject : javaProjects) {
				IPackageFragment[] packageFragments = iJavaProject.getPackageFragments();
				for (IPackageFragment iPackageFragment : packageFragments) {
					ICompilationUnit[] compilationUnits = iPackageFragment.getCompilationUnits();
					for (ICompilationUnit iCompilationUnit : compilationUnits) {
						// printing the iCompilationUnit,
						IType[] allTypes = iCompilationUnit.getAllTypes();
						for (IType type : allTypes) {

							writeType(typesPrinter, type);

							// loop through all methods in the type.
							IMethod[] methods = type.getMethods();
							for (int x = 0; x < methods.length; x++) {
								StringBuilder sb = new StringBuilder();
								sb.append((methods[x].getElementName()) + "(");							
								ILocalVariable[] parameters = methods[x].getParameters();
								for (int i = 0; i < parameters.length; i++) {
									sb.append(getParamString(parameters[i], methods[x]));
									if (i != (parameters.length - 1)) {
										sb.append(",");
									}
								}
								sb.append(")");
								metPrinter.printRecord(sb, type.getFullyQualifiedName());
								sb.append("\n");								
							}

							// getting the class name that are not abstract and
							// not include enum
							if (type.isClass() && !(type.isEnum())) {
								classesPrinter.printRecord(type.getFullyQualifiedName());

								// checking if the class is abstract
								if (Flags.isAbstract(type.getFlags())) {
									abstractClassesPrinter.printRecord(type.getFullyQualifiedName());
								}

							}

							ITypeHierarchy typeHierarchy = type.newTypeHierarchy(new NullProgressMonitor());

							// get all super classes of this type.
							for (IType superClass : typeHierarchy.getAllSuperclasses(type))
								if (superClass.isClass()) { // just to be sure.
									// write out the super class to the types
									// file.
									writeType(typesPrinter, superClass);

									// write out the relation.
									classesExtendingClassesPrinter.printRecord(type.getFullyQualifiedName(),
											superClass.getFullyQualifiedName());
								}

							// getting all the implemented interface
							IType[] allSuperInterfaces = typeHierarchy.getAllSuperInterfaces(type);

							// getting all the interface full qualified name
							if (type.isInterface()) {
								// write this interface.
								interfacesPrinter.printRecord(type.getFullyQualifiedName());

							}

							if (type.isClass() && !(type.isEnum()) && allSuperInterfaces.length >= 1) {
								for (IType superInterface : allSuperInterfaces) {
									writeType(typesPrinter, superInterface);

									interfacesPrinter.printRecord(superInterface.getFullyQualifiedName());

									classesImplementingInterfacesPrinter.printRecord(type.getFullyQualifiedName(),
											superInterface.getFullyQualifiedName());

								}
							}
						}
					}
				}
			}

			// closing the files writer after done writing
			typesWriter.close();
			classesPrinter.close();
			abstractClassesPrinter.close();
			interfacesPrinter.close();
			classesImplementingInterfacesPrinter.close();
			classesExtendingClassesPrinter.close();
			metPrinter.close();
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
		} else
			fullName = simpleName;
		return fullName;
	}

	private static void writeType(CSVPrinter typesPrinter, IType type) throws IOException {
		typesPrinter.printRecord(
				Optional.ofNullable(type.getJavaProject()).map(IJavaElement::getElementName).orElse("NULL"),
				Optional.ofNullable(type.getCompilationUnit()).map(IJavaElement::getElementName).orElse("NULL"),
				type.getFullyQualifiedName());
	}
}