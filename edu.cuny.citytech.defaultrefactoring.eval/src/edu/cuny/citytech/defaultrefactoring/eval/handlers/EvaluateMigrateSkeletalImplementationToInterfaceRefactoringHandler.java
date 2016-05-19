package edu.cuny.citytech.defaultrefactoring.eval.handlers;

import static com.google.common.io.Files.touch;
import static edu.cuny.citytech.defaultrefactoring.core.utils.Util.createMigrateSkeletalImplementationToInterfaceRefactoringProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.osgi.framework.FrameworkUtil;

import edu.cuny.citytech.defaultrefactoring.core.refactorings.MigrateSkeletalImplementationToInterfaceRefactoringProcessor;
import edu.cuny.citytech.defaultrefactoring.core.utils.RefactoringAvailabilityTester;
import edu.cuny.citytech.defaultrefactoring.core.utils.TimeCollector;
import edu.cuny.citytech.defaultrefactoring.eval.utils.Util;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class EvaluateMigrateSkeletalImplementationToInterfaceRefactoringHandler extends AbstractHandler {

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job.create("Evaluating Migrate Skeletal Implementation to Interface Refactoring ...", monitor -> {
			CSVPrinter resultsPrinter = null;
			CSVPrinter candidateMethodPrinter = null;
			CSVPrinter migratableMethodPrinter = null;
			CSVPrinter unmigratableMethodPrinter = null;
			CSVPrinter errorPrinter = null;

			try {
				IJavaProject[] javaProjects = Util.getSelectedJavaProjectsFromEvent(event);

				resultsPrinter = createCSVPrinter("results.csv",
						new String[] { "subject", "#methods", "#migration available methods", "#migratable methods",
								"#failed preconditions", "#methods after refactoring", "time (s)" });
				candidateMethodPrinter = createCSVPrinter("candidate_methods.csv",
						new String[] { "subject", "method", "type FQN" });
				migratableMethodPrinter = createCSVPrinter("migratable_methods.csv",
						new String[] { "subject", "method", "type FQN", "destination interface FQN" });

				unmigratableMethodPrinter = createCSVPrinter("unmigratable_methods.csv",
						new String[] { "subject", "method", "type FQN", "destination interface FQN" });
				errorPrinter = createCSVPrinter("failed_preconditions.csv", new String[] { "subject", "method",
						"type FQN", "destination interface FQN", "code", "message" });

				for (IJavaProject javaProject : javaProjects) {
					if (!javaProject.isStructureKnown())
						throw new IllegalStateException(
								String.format("Project: %s should compile beforehand.", javaProject.getElementName()));

					resultsPrinter.print(javaProject.getElementName());

					/*
					 * TODO: We probably need to filter these. Actually, we
					 * could use the initial precondition check for filtering (I
					 * think) but there's enough TODOs in there that it's not
					 * possible right now.
					 */
					Set<IMethod> allMethods = getAllMethods(javaProject);
					resultsPrinter.print(allMethods.size());

					Set<IMethod> interfaceMigrationAvailableMethods = new HashSet<IMethod>();

					TimeCollector resultsTimeCollector = new TimeCollector();

					resultsTimeCollector.start();
					for (IMethod method : allMethods)
						if (RefactoringAvailabilityTester.isInterfaceMigrationAvailable(method, true,
								Optional.of(monitor)))
							interfaceMigrationAvailableMethods.add(method);
					resultsTimeCollector.stop();

					resultsPrinter.print(interfaceMigrationAvailableMethods.size());

					// candidate methods.
					for (IMethod method : interfaceMigrationAvailableMethods) {
						candidateMethodPrinter.printRecord(javaProject.getElementName(),
								Util.getMethodIdentifier(method), method.getDeclaringType().getFullyQualifiedName());
					}

					resultsTimeCollector.start();
					MigrateSkeletalImplementationToInterfaceRefactoringProcessor processor = createMigrateSkeletalImplementationToInterfaceRefactoringProcessor(
							javaProject, interfaceMigrationAvailableMethods.toArray(
									new IMethod[interfaceMigrationAvailableMethods.size()]),
							Optional.of(monitor));
					resultsTimeCollector.stop();
					processor.setLogging(false);

					System.out.println(
							"Original methods before preconditions: " + interfaceMigrationAvailableMethods.size());
					System.out.println("Source methods before preconditions: " + processor.getSourceMethods().size());
					System.out.println(
							"Unmigratable methods before preconditions: " + processor.getUnmigratableMethods().size());
					System.out.println(
							"Migratable methods before preconditions: " + processor.getMigratableMethods().size());

					// run the precondition checking.
					resultsTimeCollector.start();
					ProcessorBasedRefactoring processorBasedRefactoring = new ProcessorBasedRefactoring(processor);
					RefactoringStatus status = processorBasedRefactoring.checkAllConditions(new NullProgressMonitor());
					resultsTimeCollector.stop();

					System.out.println(
							"Original methods after preconditions: " + interfaceMigrationAvailableMethods.size());
					System.out.println("Source methods after preconditions: " + processor.getSourceMethods().size());

					File file = new File("source.txt");
					touch(file);
					Files.write(
							file.toPath(), processor.getSourceMethods().parallelStream()
									.map(m -> m.getHandleIdentifier()).collect(Collectors.toSet()),
							StandardOpenOption.APPEND);

					System.out.println(
							"Unmigratable methods after preconditions: " + processor.getUnmigratableMethods().size());

					file = new File("unmigratable.txt");
					touch(file);
					Files.write(
							file.toPath(), processor.getUnmigratableMethods().parallelStream()
									.map(m -> m.getHandleIdentifier()).collect(Collectors.toSet()),
							StandardOpenOption.APPEND);

					System.out.println(
							"Migratable methods after preconditions: " + processor.getMigratableMethods().size());

					file = new File("migratable.txt");
					touch(file);
					Files.write(
							file.toPath(), processor.getMigratableMethods().parallelStream()
									.map(m -> m.getHandleIdentifier()).collect(Collectors.toSet()),
							StandardOpenOption.APPEND);

					// passed methods.
					resultsPrinter.print(processor.getMigratableMethods().size()); // number.

					for (IMethod method : processor.getMigratableMethods()) {
						migratableMethodPrinter.printRecord(javaProject.getElementName(),
								Util.getMethodIdentifier(method), method.getDeclaringType().getFullyQualifiedName(),
								getDestinationTypeFullyQualifiedName(method, monitor));

					}

					// failed methods.
					for (IMethod method : processor.getUnmigratableMethods()) {
						unmigratableMethodPrinter.printRecord(javaProject.getElementName(),
								Util.getMethodIdentifier(method), method.getDeclaringType().getFullyQualifiedName(),
								getDestinationTypeFullyQualifiedName(method, monitor));
					}

					// failed preconditions.
					resultsPrinter.print(status.getEntries().length); // number.

					for (RefactoringStatusEntry entry : status.getEntries()) {
						if (!entry.isFatalError()) {
							Object correspondingElement = entry.getData();

							if (!(correspondingElement instanceof IMethod))
								throw new IllegalStateException("The element: " + correspondingElement
										+ " corresponding to a failed precondition is not a method. Instead, it is a: "
										+ correspondingElement.getClass());

							IMethod failedMethod = (IMethod) correspondingElement;
							errorPrinter.printRecord(javaProject.getElementName(),
									Util.getMethodIdentifier(failedMethod),
									failedMethod.getDeclaringType().getFullyQualifiedName(),
									getDestinationTypeFullyQualifiedName(failedMethod, monitor), entry.getCode(),
									entry.getMessage());
						}
					}

					// actually perform the refactoring if there are no fatal
					// errors.
					if (!status.hasFatalError()) {
						resultsTimeCollector.start();
						Change change = processorBasedRefactoring
								.createChange(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
						change.perform(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
						resultsTimeCollector.stop();
					}

					// ensure that we can build the project.
					if (!javaProject.isConsistent())
						javaProject.makeConsistent(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));

					if (!javaProject.isStructureKnown())
						throw new IllegalStateException(String.format("Project: %s should compile after refactoring.",
								javaProject.getElementName()));

					// TODO: Count warnings?

					// count the new number of methods.
					resultsPrinter.print(getAllMethods(javaProject).size());

					// overall results time.
					resultsPrinter.print((resultsTimeCollector.getCollectedTime()
							- processor.getExcludedTimeCollector().getCollectedTime()) / 1000.0);

					// end the record.
					resultsPrinter.println();
				}
			} catch (Exception e) {
				return new Status(IStatus.ERROR, FrameworkUtil.getBundle(this.getClass()).getSymbolicName(),
						"Encountered exception during evaluation", e);
			} finally {
				try {
					// closing the files writer after done writing
					if (resultsPrinter != null)
						resultsPrinter.close();
					if (candidateMethodPrinter != null)
						candidateMethodPrinter.close();
					if (migratableMethodPrinter != null)
						migratableMethodPrinter.close();
					if (unmigratableMethodPrinter != null)
						unmigratableMethodPrinter.close();
					if (errorPrinter != null)
						errorPrinter.close();
				} catch (IOException e) {
					return new Status(IStatus.ERROR, FrameworkUtil.getBundle(this.getClass()).getSymbolicName(),
							"Encountered exception during file closing", e);
				}
			}

			return new Status(IStatus.OK, FrameworkUtil.getBundle(this.getClass()).getSymbolicName(),
					"Evaluation successful.");
		}).schedule();

		return null;
	}

	private static String getDestinationTypeFullyQualifiedName(IMethod method, IProgressMonitor monitor)
			throws JavaModelException {
		return MigrateSkeletalImplementationToInterfaceRefactoringProcessor
				.getTargetMethod(method, Optional.of(monitor)).getDeclaringType().getFullyQualifiedName();
	}

	private static Set<IMethod> getAllMethods(IJavaProject javaProject) throws JavaModelException {
		Set<IMethod> methods = new HashSet<>();

		// collect all methods from this project.
		IPackageFragment[] packageFragments = javaProject.getPackageFragments();
		for (IPackageFragment iPackageFragment : packageFragments) {
			ICompilationUnit[] compilationUnits = iPackageFragment.getCompilationUnits();
			for (ICompilationUnit iCompilationUnit : compilationUnits) {
				IType[] allTypes = iCompilationUnit.getAllTypes();
				for (IType type : allTypes) {
					Collections.addAll(methods, type.getMethods());
				}
			}
		}
		return methods;
	}

	private static CSVPrinter createCSVPrinter(String fileName, String[] header) throws IOException {
		return new CSVPrinter(new FileWriter(fileName, true), CSVFormat.EXCEL.withHeader(header));
	}
}