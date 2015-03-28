* Have to remove the @Override annotation.
	* What if that method overrides a method in another class?
		* For example, AbstractCollection implements Collection and Iterable.
		* If we move it, what happens?
		* TODO: Try this by adding a second interface.
