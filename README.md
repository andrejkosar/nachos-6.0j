# Nachos for Java (Nachos 6.0j)

## About
**Not Another Completely Heuristic Operating System**, or **Nachos**, is instructional software for teaching undergraduate, and potentially graduate level operating systems courses. It was developed at the University of California, Berkeley, designed by Thomas Anderson, and is used by numerous schools around the world.

This repository contains modified Nachos project. It is result of my 2016/2017 master thesis at the Faculty of Electrical Engineering and Information Technology, Slovak University of Technology in Bratislava were I studied Applied Informatics. The purpose of the master thesis was to create demonstrational implementation of the Nachos and to create documentation which will be used by students in Operating systems course at the university.

Nachos usage in operating system courses was discontinued at Berkeley in 2014. This caused, that a lot of nachos parts were outdated (or rather this was the reason to discontinue Nachos usage). Therefor the thesis ended up being much more than just demonstrational implementation. Plenty of improvements were introduced to recover Nachos for Java and to change it, hopefully, for the better.

To learn more about installation, usage and about project structure and phases head over to [Nachos wiki](https://github.com/andrejkosar/nachos-6.0j/wiki).

## Improvements from Nachos 5.0j
#### 1. ELF binary support
- This is very important and vital improvement for Java Nachos. GCC has discontinued support for _mips-coff_ platforms like _"mips-\*-ultrix\*"_, so in order to use it, you would have to use an old version of binutils and GCC. 
- We have introduced new ELF binary loader to allow students to execute user programs compiled into ELF binary widely used in Unix world nowadays. All classes related to ELF binary loading are in `nachos.machine.elf` package. This package consists of several classes, namely `Elf`, `ElfHeader`, `ElfProgramHeader` and `ElfSectionHeader`. 

#### 2. Grading & testing Nachos implementation using JUnit
 - Original Nachos used so called `AutoGrader`. Purpose of this class was to test student's implementation and automatically grade projects. 
 - This implementation of Nachos shifted testing logic from `AutoGrader` classes to JUnit tests. This brings plethora of additional options when writing tests, better descriptive assertion failures and overall better project organization, as grading tests are now separated from actual Nachos source code.
 - Implementation still uses something similar to `AutoGrader` called `NachosRuntimeRecorder`. Hopefully the name is self explanatory, as most of the times (except some necessary wiring), only thing what this class does is that it collects Nachos runtime information, which are in turn used by the JUnit tests.

#### 3. Exceptions back-propagation
 - This improvement is kind of related to ability to use JUnit tests, but it has meaning even on its own.
 - When an exception is thrown in any thread of the original Nachos, everything, what implementation does is, that it prints stack trace of the exception and kills JVM with `System.exit(1)`. This caused every JUnit test, besides the first one, to not execute at all. Only solution was to run each test in separate process with separate JVM instance, which, of course, caused a lot of overhead and slowed the tests drastically.
 - This implementation does not use `System.exit()` call. It relies on exceptions, when finishing all Nachos threads and correctly propagating exit cause back to main Java thread created automatically when Java starts. All of the exceptions used to finish all Nachos threads cleanly are inside of the `nachos.machine.tcb` package. Namely `NachosSystemExitException` thrown and caught only by the main thread, `NachosThreadExitException` thrown when system exit was called (with or without an causing exception) in one of the threads and `NachosThreadFinishedException` thrown when nachos thread finishes it's execution without errors. 

#### 4. Grading JUnit tests as part of the project
 - There are 76 tests implemented together for first and second phase of the Nachos project. Each test checks implementation of some task and is well enough documented, to give clue to the students, what is possibly wrong (or correct) about their implementation of the related task.
 - These tests are part of the project and can be run by students on the fly locally on their machine. 

#### 5. Plenty of other improvements
Besides the major things mentioned above, there are a lot of other improvements. To name just a few:

 - Main machine simulation package `machine` was divided to smaller sub-packages, which give students immediate clue about responsibilities of the classes inside them.
 - Project is refactored to Gradle project, for simple dependency management of the JUnit dependencies (and possibly other dependencies, when required in the future).
 - Makefile in the nachos home directory with user programs written in C was enhanced with tasks to easily compile all C files within the directory, clean it or to clean all temporary files and revert it back as if nothing happened.
 - Demonstrational implementation with comments available for the students, after they submit their implementation (not included in this repository).
 - Thorough Java documentation.

## Future TODOs
There are multiple things that can be considered as next tasks to improve Nachos even more. To name a few:

 - Demonstrational implementation of the 3. phase.
 - Grading JUnit tests for the 3. phase.
 - Demonstrational implementation of the 4. phase.
 - Grading JUnit tests for the 4. phase.
 - Port the file system project from C/C++ version of Nachos.
 - Introduce more tasks (or replace some of the existing) to better cover course syllabus.

## Authors
[Andrej Kosar](https://github.com/andrejkosar)

## Credits
Nachos was originally written by **Wayne A. Christopher**, **Steven J. Procter**, and **Thomas E. Anderson**. It incorporates the SPIM simulator written by **John Ousterhout**. Nachos was rewritten in Java by **Daniel Hettena**.

## License
Copyright &copy; 1992-2001 The Regents of the University of California.
All rights reserved.

Permission to use, copy, modify, and distribute this software and its documentation for any purpose, without fee, and without written agreement is hereby granted, provided that the above copyright notice and the following two paragraphs appear in all copies of this software.

IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

## References
 - [Official Berkeley project course from 2010](http://inst.eecs.berkeley.edu/~cs162/fa10/)
 - [CS 162 Berkeley lectures webcasts](https://www.youtube.com/playlist?list=PL0FFC69A114ECD59D)
 - [A Road Map Through Nachos (C/C++ version)](https://users.cs.duke.edu/~narten/110/nachos/main/main.html)
 - [1995 document containing only the ELF Specification](https://refspecs.linuxfoundation.org/elf/elf.pdf)
 - [How to Build a GCC Cross-Compiler](http://preshing.com/20141119/how-to-build-a-gcc-cross-compiler/)
 - [GCC Cross-Compiler](http://wiki.osdev.org/GCC_Cross-Compiler)
 - [Wikipedia article on Nachos](https://en.wikipedia.org/wiki/Not_Another_Completely_Heuristic_Operating_System)