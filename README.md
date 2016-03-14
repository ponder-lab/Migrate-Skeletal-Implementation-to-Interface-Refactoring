# Migrate Skeletal Implementation to Interface Refactoring [![Build Status](https://travis-ci.org/khatchad/Java-8-Interface-Refactoring.svg?branch=master)](https://travis-ci.org/khatchad/Java-8-Interface-Refactoring) [![Coverage Status](https://coveralls.io/repos/khatchad/Java-8-Interface-Refactoring/badge.svg)](https://coveralls.io/r/khatchad/Java-8-Interface-Refactoring)

## Introduction

The *skeletal implementation pattern* is a software design pattern consisting of defining an abstract class that provides a partial interface implementation. However, since Java allows only single class inheritance, if implementers decide to extend a skeletal implementation, they will not be allowed to extend any other class. Also, discovering the skeletal implementation may require a global analysis. Java 8 enhanced interfaces alleviate these problems by allowing interfaces to contain (default) method implementations, which implementers inherit. Java classes are then free to extend a different class, and a separate abstract class is no longer needed; developers considering implementing an interface need only examine the interface itself. Both of these benefits improve software modularity.

This prototype refactoring plug-in for [Eclipse](http://eclipse.org) represents ongoing work in developing an automated refactoring tool that would assist developers in taking advantage of the enhanced interface feature for their legacy Java software.

## Installation for Development

The project includes a maven configuration file using the tycho plug-in. Running `mvn install` will install all dependencies. Note that if you are not using maven, this plugin depends on https://github.com/khatchad/edu.cuny.citytech.refactoring.common.

## Running the Evaluator
1. Select a group of projects.
2. Press CMD-3 or CTRL-3 (command dialog).
3. Search for "Find." You'll see an option to "find candidate skeletal implementers". Choose that option.
4. A set of `.csv` files will appear in the working directory.
