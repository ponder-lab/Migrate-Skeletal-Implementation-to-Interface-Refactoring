# Migrate Skeletal Implementation to Interface Refactoring 

[![Build Status](https://travis-ci.org/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring.svg?branch=master)](https://travis-ci.org/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring) [![Coverage Status](https://coveralls.io/repos/github/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring/badge.svg?branch=master&service=github)](https://coveralls.io/github/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring?branch=master) [![GitHub license](https://img.shields.io/badge/license-Eclipse-blue.svg)](https://raw.githubusercontent.com/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring/master/LICENSE.txt) [![DOI](https://zenodo.org/badge/33016251.svg)](https://zenodo.org/badge/latestdoi/33016251)

## Screenshot

![Screenshot](https://i2.wp.com/openlab.citytech.cuny.edu/interfacerefactoring/files/2011/06/Screen-Shot-2016-03-14-at-11.43.53-PM-e1458161353498.png)

## Demonstration

(click to view)

[![Video demo of refactoring tool](http://img.youtube.com/vi/YZHIy0yePh8/0.jpg)](http://www.youtube.com/watch?v=YZHIy0yePh8 "Migrate Skeletal Implementation to Interface Refactoring Tool Demonstration")

## Introduction

The *skeletal implementation pattern* is a software design pattern consisting of defining an abstract class that provides a partial interface implementation. However, since Java allows only single class inheritance, if implementers decide to extend a skeletal implementation, they will not be allowed to extend any other class. Also, discovering the skeletal implementation may require a global analysis. Java 8 enhanced interfaces alleviate these problems by allowing interfaces to contain (default) method implementations, which implementers inherit. Java classes are then free to extend a different class, and a separate abstract class is no longer needed; developers considering implementing an interface need only examine the interface itself. Both of these benefits improve software modularity.

This prototype refactoring plug-in for [Eclipse](http://eclipse.org) represents ongoing work in developing an automated refactoring tool that would assist developers in taking advantage of the enhanced interface feature for their legacy Java software.

## Usage

Currently, the prototype refactoring works only via the package explorer and the outline views (see issues [#2](https://github.com/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring/issues/2) and [#65](https://github.com/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring/issues/65)). You can either select a single method to migrate or select a class, package, or (multiple) projects. In the latter case, the tool will find methods in the enclosing item(s) that are eligible for migration.

## Installation

A beta version of our tool is available via an Eclipse update site at: https://raw.githubusercontent.com/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring/master/edu.cuny.citytech.defaultrefactoring.updatesite. Please choose the latest version.

You may also install the tool via the [Eclipse Marketplace](https://marketplace.eclipse.org/content/migrate-skeletal-implementation-interface-refactoring) by dragging this icon to your running Eclipse workspace: [![Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client](https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png)](http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=3746776 "Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client").

## Limitations

The research prototype refactoring is conservative. While tool should not produce any type-incorrect or semantic-inequivalent code, it may not refactor *all* code that may be safe to refactor.

## Contributing

See the [contribution guide](https://github.com/ponder-lab/Migrate-Skeletal-Implementation-to-Interface-Refactoring/blob/master/CONTRIBUTING.md).

## Publications

Raffi Khatchadourian and Hidehiko Masuhara. Proactive empirical assessment of new language feature adoption via automated refactoring: The case of Java 8 default methods. In *International Conference on the Art, Science, and Engineering of Programming*, volume 2 of *Programming '18*, pages 6:1--6:30. AOSA, March 2018. \[ [bib](http://www.cs.hunter.cuny.edu/~Raffi.Khatchadourian99/all_bib.html#Khatchadourian2018) | [DOI](http://dx.doi.org/10.22152/programming-journal.org/2018/2/6) | [http](https://academicworks.cuny.edu/hc_pubs/354) \]

Raffi Khatchadourian and Hidehiko Masuhara. Defaultification refactoring: A tool for automatically converting Java methods to default. In *International Conference on Automated Software Engineering*, ASE '17, pages 984--989, Piscataway, NJ, USA, October 2017. ACM/IEEE, IEEE Press. \[ [bib](http://www.cs.hunter.cuny.edu/~Raffi.Khatchadourian99/all_bib.html#Khatchadourian2017b) | [DOI](http://dx.doi.org/10.1109/ASE.2017.8115716) | [http](http://academicworks.cuny.edu/hc_pubs/329) \]

Raffi Khatchadourian and Hidehiko Masuhara. Automated refactoring of legacy Java software to default methods. In *International Conference on Software Engineering*, ICSE '17, pages 82--93, Piscataway, NJ, USA, May 2017. ACM/IEEE, IEEE Press. \[ [bib](http://www.cs.hunter.cuny.edu/~Raffi.Khatchadourian99/all_bib.html#Khatchadourian2017a) | [DOI](http://dx.doi.org/10.1109/ICSE.2017.16) | [http](http://academicworks.cuny.edu/hc_pubs/287) \]
