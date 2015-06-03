# Before
- We have two interfaces, `I` and `J`, each declaring a method `m()`. 
- `A` is the `abstract` skeletal class that provides a partial implementation of `m()`. 

# After
- Pull up `A.m()` to either `I` or `J` and now we have an error.