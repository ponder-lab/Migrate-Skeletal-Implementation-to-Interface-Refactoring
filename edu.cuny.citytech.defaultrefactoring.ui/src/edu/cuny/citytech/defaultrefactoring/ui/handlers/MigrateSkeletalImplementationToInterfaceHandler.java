package edu.cuny.citytech.defaultrefactoring.ui.handlers;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.cuny.citytech.defaultrefactoring.ui.plugins.MigrateSkeletalImplementationToInterfaceRefactoringPlugin;
import edu.cuny.citytech.defaultrefactoring.ui.wizards.MigrateSkeletalImplementationToInterfaceRefactoringWizard;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class MigrateSkeletalImplementationToInterfaceHandler extends AbstractHandler {

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelectionChecked(event);

		List<?> list = SelectionUtil.toList(currentSelection);
		IMethod[] methods = list.stream().filter(e -> e instanceof IMethod).toArray(length -> new IMethod[length]);
		IJavaProject[] javaProjects = list.stream().filter(e -> e instanceof IJavaProject)
				.toArray(length -> new IJavaProject[length]);

		try {

			FileWriter writer = new FileWriter("InterfaceDefaultRefactoringTest.csv");

			String[] cvsHeader = { "Project Name", ",", "Package", ",", "CompilationUnit", ",", "Type Name", ",", "IsClass",
					",", "IsInterface", ",", " IsAbstract", ",", "Interfaces Extended" };

			for (int i = 0; i < cvsHeader.length; i++) {
				writer.append(cvsHeader[i]);
			}
			writer.append('\n');

			for (IJavaProject iJavaProject : javaProjects) {
				IPackageFragment[] packageFragments = iJavaProject.getPackageFragments();
				for (IPackageFragment iPackageFragment : packageFragments) {
					ICompilationUnit[] compilationUnits = iPackageFragment.getCompilationUnits();
					for (ICompilationUnit iCompilationUnit : compilationUnits) {
						// printing the iCompilationUnit,
						IType[] allTypes = iCompilationUnit.getAllTypes();
						for (IType iType : allTypes) {
							// print the info about the type.
							writer.append(iJavaProject.getElementName());
							writer.append(',');
							writer.append(iPackageFragment.getElementName());
							writer.append(',');
							writer.append(iCompilationUnit.getElementName());
							writer.append(',');
							writer.append(iType.getElementName());
							writer.append(',');
							writer.append(iType.isClass() + "");
							writer.append(',');
							writer.append(iType.isInterface() + "");
							writer.append(',');
							writer.append(Flags.isAbstract(iType.getFlags()) + "");
							writer.append(',');
							// getting all the implemented interface
							ITypeHierarchy typeHierarchie = iType.newTypeHierarchy(new NullProgressMonitor());
							IType[] interfaceType = typeHierarchie.getAllSuperInterfaces(iType);
							int interfaceCount  = 0;
							for (IType InterfaceIType : interfaceType) {
								interfaceCount++;
							}
							writer.append(interfaceCount+" ");

							// next row (done with this type).
							writer.append('\n');

						}
					}
				}
			}

			// closing the files after done writing
			writer.flush();
			writer.close();
		} catch (JavaModelException | IOException fileException) {
			fileException.printStackTrace();
		}
		getIMethods(event, methods);
		return null;
	}

	private void getIMethods(ExecutionEvent event, IMethod[] methods) throws ExecutionException {
		if (methods.length > 0) {
			Shell shell = HandlerUtil.getActiveShellChecked(event);
			HandlerUtil.getActiveWorkbenchWindowChecked(event).getWorkbench().getProgressService().showInDialog(shell,
					Job.create("Migrate skeletal implementation to interface", monitor -> {
						try {
							MigrateSkeletalImplementationToInterfaceRefactoringWizard.startRefactoring(methods, shell,
									monitor);
							return Status.OK_STATUS;
						} catch (JavaModelException e) {
							JavaPlugin.log(e);
							return new Status(Status.ERROR, MigrateSkeletalImplementationToInterfaceRefactoringPlugin
									.getDefault().getBundle().getSymbolicName(), "Failed to start refactoring.");
						}
					}));
		}
	}
}