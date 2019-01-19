# About the Code Generator
The PQ9 Code Generator is a template system that is specifically written for generating PQ9 Subsystem software. The goal is to provide a program that makes the subsystem software accessible to those without in-depth understanding of either the PQ9 software structure or the subsystem specific hardware code by generating the code from templates and parameters. 

The Code Generator comes with a custom template format, for which a number of commands and keywords have been defined to make the templates very versatile. The templates and Code Generator can be used for any application that benefits from templates and parameters. The generator is originally intended to use templates that produce code in the C programming language for use within the PQ9 system, but the Code Generator doesn't explicitly require this. Technically any text or code language should be accepted by the the code generator as a valid template, as long as the template contents don't conflict with the pre-defined template commands and keywords (See chapter \ref{wwtemp}).

The intended use is to provide a standard code structure that is shared among hardware within the PQ9 system. This structure can be represented as a set of base templates for a PQ9 subsystem. Subsystem experts can then be asked to fill in the subsystem specific code sections of these templates for their subsystems. An extra burden is placed on the subsystem experts by further asking them to define their code in terms of subsystem parameters, for as much as such a request is possible. Thus producing a subsystem specific set of templates and parameters, which when run through the Code Generator produce fully functional subsystem software for the specified parameters.

This could be used to work towards a standard for PQ9 software that is consistent across subsystems. It would also allow for simple modification of a subsystem's behaviour by changing subsystem parameters, without having to consider underlying code. Similarly, the base templates should make implementing hardware specific code sections easier to manage by not having to consider the overhead of the PQ9 software structure.


# Quick Start Guide

## Where to get the Code Generator
For a direct download of the latest version of the PQ9 Code Generator, including this manual, the source code and example templates, head over to the latest release in the releases section:


https://github.com/ErikMekkes/PQ9_bus_software/releases

## Running the code generator
Update the settings.json file :

- Change the desired name for the subsystem.
- Update the list of desired sub-directories.
- Update the list of desired files to generate, for each file :
  - provide the output filename
  - provide the base template name if it's different from the output filename
  - provide the set of parameters used within the output file (as list or as csv file)

Check / fill in your specified .csv files (or update the params.csv file when using list).

Check / fill in your specified base templates, make sure to use to /cgen\_template extension.

Start the code Generator by running CodeGenerator.jar

## Modifying the source code
The entire code generator project is open source, you are free to make your own copy, clone or fork of the original source files and free to customize it to your needs.\vsp

The project is set up with maven, for which the configuration has been included. It should be possible to directly import the source code as a maven project and modify / build / run it in most IDE's. Maven has been configured to create an executable jar file in ./target/CodeGenerator directory, along with all required external files such as templates and settings.json during the build cycle. If you wish to include additional external files, please check out the resources section and comments in the maven pom.xml.\vsp

Have a contribution that you believe should be added to the original? Make a fork of the original github repository, apply your changes and create a pull request to the code generator repository.

## Sharing templates
Currently there is no service provided by the maintainers to share templates. Feel free to set one up if there is a demand.


# Program Manual
For a more detailed explanation of the available settings and options, including examples, please refer to the Program Manual included with the product. It is available as direct download here : 

https://github.com/ErikMekkes/PQ9_bus_software/blob/master/CodeGenerator/doc/PQ9_Code_Generator_Manual.pdf

The manual is also included within the latest release.
