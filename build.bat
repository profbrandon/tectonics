
@echo off

javac -d bin ^
      -sourcepath src/java ^
      src/java/com/tectonics/gui/TectonicSim.java

jar cfm TectonicSim.jar manifest.txt -C bin com