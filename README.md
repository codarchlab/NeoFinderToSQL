# NeoFinderToSQL

Parses CDFinder's (or NeoFinder's) catalog exports (CSV-Files) and produces SQL Updates for Arachne. 

The command line application requires as its only parameter a path to a folder containing the CSV files or the direct path to a single CSV file. The program will prompt for the name of the target table.

Example for starting the runnable jar with CSV-files in the same directory:
java -jar neofinderToSQL.jar .
