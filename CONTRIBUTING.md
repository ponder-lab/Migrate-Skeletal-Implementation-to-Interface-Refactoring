# Contributing

We are currently seeking new collaborations. If you are interested in contributing, please see refer to our [wiki](https://github.com/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring/wiki) and [issues](https://github.com/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring/issues). Please also feel free to visit our [research page](https://openlab.citytech.cuny.edu/interfacerefactoring) to get in touch with the authors.

## Installation for Development

The project includes a maven configuration file using the tycho plug-in, which is part of the [maven eclipse plugin](http://www.eclipse.org/m2e/). Running `mvn install` will install all dependencies. Note that if you are not using maven, this plugin depends on [edu.cuny.citytech.refactoring.common](/ponder-lab/edu.cuny.citytech.refactoring.common), the **Eclipse SDK**, **Eclipse SDK tests**, and the **Eclipse testing framework**. The latter three can be installed from the "Install New Software..." menu option under "Help" in Eclipse.

## Running the Evaluator

The plug-in edu.cuny.citytech.defaultrefactoring.eval is the evaluation plug-in. Note that it is not included in the standard update site as that it user focused. To run the evaluator, clone the repository and build and run the plug-in from within Eclipse. This will load the plug-in edu.cuny.citytech.defaultrefactoring.eval (verify in "installation details.").

There is no UI menu options for the evaluator, however, there is an Eclipse command, which is available from the quick execution dialog in Eclipse. Please follow these steps:

1. Select a group of projects.
2. Press CMD-3 or CTRL-3 (command dialog).
3. Search for "evaluate." You'll see an option to run the migration evaluator. Choose it.
4. Once the evaluator completes, a set of `.csv` files will appear in the working directory.
