package edu.cuny.citytech.defaultrefactoring.ui.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.cuny.citytech.defaultrefactoring.ui.wizards.MigrateSkeletalImplementationToInterfaceRefactoringWizard;

public class MigrateSkeletalImplementationToInterfaceHandler extends AbstractHandler {

	/**
	 * Gather all the methods from the user's selection.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelectionChecked(event);
		List<?> list = SelectionUtil.toList(currentSelection);

		if (list != null) {
			try {
				Set<IMethod> methodSet = new HashSet<>();

				for (Object obj : list) {
					if (obj instanceof IJavaElement) {
						IJavaElement jElem = (IJavaElement) obj;
						switch (jElem.getElementType()) {
						case IJavaElement.METHOD:
							methodSet.add((IMethod) jElem);
							break;
						case IJavaElement.TYPE:
							// A type is either a class, interface, or enum. Get
							// only methods from classes.
							extractMethodsFromClass(methodSet, (IType) jElem);
							break;
						case IJavaElement.COMPILATION_UNIT:
							extractMethodsFromCompilationUnit(methodSet, (ICompilationUnit) jElem);
							break;
						case IJavaElement.PACKAGE_FRAGMENT:
							extractMethodsFromPackageFragment(methodSet, (IPackageFragment) jElem);
							break;
						case IJavaElement.PACKAGE_FRAGMENT_ROOT:
							extractMethodsFromPackageFragmentRoot(methodSet, (IPackageFragmentRoot) jElem);
							break;
						case IJavaElement.JAVA_PROJECT:
							extractMethodsFromJavaProject(methodSet, (IJavaProject) jElem);
							break;
						}
					}
				}

				Shell shell = HandlerUtil.getActiveShellChecked(event);
				MigrateSkeletalImplementationToInterfaceRefactoringWizard
						.startRefactoring(methodSet.toArray(new IMethod[methodSet.size()]), shell, Optional.empty());
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				throw new ExecutionException("Failed to start refactoring", e);
			}
		}
		// TODO: What do we do if there was no input? Do we display some
		// message?
		return null;
	}

	private void extractMethodsFromJavaProject(Set<IMethod> methodSet, IJavaProject jProj) throws JavaModelException {
		IPackageFragmentRoot[] roots = jProj.getPackageFragmentRoots();
		for (IPackageFragmentRoot iPackageFragmentRoot : roots) {
			extractMethodsFromPackageFragmentRoot(methodSet, iPackageFragmentRoot);
		}
	}

	private void extractMethodsFromPackageFragmentRoot(Set<IMethod> methodSet, IPackageFragmentRoot root)
			throws JavaModelException {
		IJavaElement[] children = root.getChildren();
		for (IJavaElement child : children)
			if (child.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
				extractMethodsFromPackageFragment(methodSet, (IPackageFragment) child);
	}

	private void extractMethodsFromPackageFragment(Set<IMethod> methodSet, IPackageFragment frag)
			throws JavaModelException {
		ICompilationUnit[] units = frag.getCompilationUnits();
		for (ICompilationUnit iCompilationUnit : units) {
			extractMethodsFromCompilationUnit(methodSet, iCompilationUnit);
		}
	}

	private void extractMethodsFromCompilationUnit(Set<IMethod> methodSet, ICompilationUnit cu)
			throws JavaModelException {
		IType[] types = cu.getTypes();
		for (IType iType : types) {
			extractMethodsFromClass(methodSet, iType);
		}
	}

	private void extractMethodsFromClass(Set<IMethod> methodSet, IType type) throws JavaModelException {
		if (type.isClass())
			methodSet.addAll(Arrays.asList(type.getMethods()));
	}
}