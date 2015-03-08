# JenaOntApp

## Introduction

JenaOntApp is a program to combine the ontology and jena generic reasoner for the application, the goal is to create a program that user only have to define the ontology, input data and the rule without modifying the program to achieve the goal.

This program is still under construction, more features on the way.

## Build

The program is build by using the [maven](http://maven.apache.org/) build tools, you only need to key in

```
$ mvn clean compile assembly:single
```

The source code will be compiled into an independent jar file, so you don't have to add lots of class path to execute :)


## Execute

For executing this program you will need 3 input files, and the command line will be like this

```
$ java -cp <jarfile> com.delta.JenaOntApp <rdffile> <rulefile> <jsonfile>
```

* <jarfile> : the file you compiled 
