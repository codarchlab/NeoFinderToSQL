package main;

import java.util.*;
import java.util.regex.*;
import java.io.*;

public class Main {

	private static int inputLineCounter = 0;
	private static int entityCounter = 0;
	private static int folderCounter = 0;
	private static int fileCounter = 0;
	private static int skippedCounter = 0;

	private static int resultLineCounter = 0;
	private static int resultLineLimit = 5000000;
	private static int resultFileCounter = 0;

	private static int indexName;
	private static int indexPath;
	private static int indexType;

	private static int indexCreated;
	private static int indexChanged;

	private static int indexCatalog;
	private static int indexVolume;

	private static boolean isFirstLine = false;

	private static Pattern bauwerkPattern = Pattern.compile("^BT(\\d+)$");
	private static Pattern zeroPattern = Pattern.compile("^0+(\\d+)$");
	private static Pattern numbersPattern = Pattern.compile("\\d+");
	private static Pattern fileNamePattern = Pattern
			.compile("^.*_(\\w+)(,\\d{2})?\\.\\w{3}$");

	private static String outputPath = "results";

	public static void main(String[] args) {
		
		File outputFolder = new File(outputPath);
		if (!outputFolder.exists())
			outputFolder.mkdirs();

		if (args.length != 1) {
			System.out
					.println("Expecting one parameter: arg[0] = source folder or file.");
			return;
		}

		File scanDirectory = new File(args[0]);

		if (!scanDirectory.exists()) {
			System.out.println("Source " + args[0] + " does not exist.");
			return;
		}

		if (scanDirectory.isDirectory()) {
			String[] files = scanDirectory.list();
			for (int i = 0; i < files.length; i++) {
				if (files[i].endsWith(".csv")) {
					readCSV(scanDirectory + "/" + files[i]);
				}
			}
		} else if (scanDirectory.getName().endsWith(".csv")) {
			readCSV(scanDirectory.getAbsolutePath());			
		}
		
		System.out.println("Done. Lines processed: " + inputLineCounter);
		System.out.println(skippedCounter + " lines skipped");
		System.out.println(folderCounter + " folders");
		System.out.println(fileCounter + " files");
		System.out.println(entityCounter + " of those files potentially referring to arachne entities");
	}


	private static void readCSV(String path) {
		isFirstLine = true;
		resultFileCounter = 0;
		resultLineCounter = 0;			
		
		File file = new File(path);
		if (!file.canRead()) {
			System.out.println("Unable to read file: " + path);
			return;
		}
		
		try {		
			BufferedReader reader = new BufferedReader(new FileReader(file));
			PrintWriter out = createNewOutstream(file.getName());			
			String currentLine = null;

			while ((currentLine = reader.readLine()) != null) {
				inputLineCounter++;
				String[] lineContents = currentLine.split("\\t");

				for (int i = 0; i < lineContents.length; i++)
					lineContents[i] = lineContents[i].trim();

				if (lineContents.length < 12) {
					skippedCounter++;
					// System.out.println("Skipping line: " + currentLine);
					continue;
				}
				if (lineContents[0].compareTo("Name") == 0 && isFirstLine) {
					System.out.println("New file: " + path);
					isFirstLine = false;
					setIndices(lineContents);
					continue;
				}
				
				if (lineContents[indexType].compareTo("Ordner") == 0 || lineContents[indexType].compareTo("Folder") == 0) {
					folderCounter++;
					continue;
				} 
				
				fileCounter++;
				try {
					String currentName = (indexName != -1) 
							? lineContents[indexName] : null;
					String currentPath = (indexPath != -1) 
							? lineContents[indexPath] : null;
					String currentCreated = (indexCreated != -1) 
							? lineContents[indexCreated] : null;
					String currentChanged = (indexChanged != -1) 
							? lineContents[indexChanged] : null;
					String currentType = (indexType != -1) 
							? lineContents[indexType] : null;
					String currentCatalog = (indexCatalog != -1) 
							? lineContents[indexCatalog] : null;
					String currentVolume = (indexVolume != -1) 
							? lineContents[indexVolume]	: null;

					ArachneEntity entityInfo = null;

					if (currentPath.toLowerCase().contains("datenbank")
							|| currentPath.toLowerCase().contains("rohscan")
							|| currentPath.toLowerCase().contains("druck")) {
						entityInfo = tryParsingArachneEntityFromFileName(lineContents[indexName]);
					}

					String currentArachneID = (entityInfo != null) ? entityInfo.arachneID : null;
					String currentRestrictingTable = (entityInfo != null) ? entityInfo.restrictingTable	: null;
					boolean currentForeignKey = (entityInfo != null) ? entityInfo.foreignKey : false;

					ArchivedFileInfo fileInfo = new ArchivedFileInfo(
							currentArachneID, currentName, currentPath,
							currentCreated, currentChanged,
							currentForeignKey, currentRestrictingTable,
							currentCatalog, currentVolume, currentType);

					ArrayList<ArchivedFileInfo> list = new ArrayList<ArchivedFileInfo>();

					list.add(fileInfo);

					if (resultLineCounter == resultLineLimit) {
						resultFileCounter++;
						resultLineCounter = 0;
						out.close();	
						out = createNewOutstream(file.getName());
					} else {
						resultLineCounter++;
					}

					writeSQLUpdate(list, out);
				} catch (ArrayIndexOutOfBoundsException e) {
					System.out.println("Wrong index at line:");
					System.out.println(currentLine);
					System.out.println("Indices:");
					System.out.println(indexName + " " + indexPath + " "
							+ indexCreated + " " + indexChanged + " "
							+ indexType + " " + indexCatalog + " "
							+ indexVolume);
					
					e.printStackTrace();
				}
				lineContents = null;
			}
			reader.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.lang.NumberFormatException e) {
			e.printStackTrace();
		}
	}

	private static void writeSQLUpdate(List<ArchivedFileInfo> list,	PrintWriter out) {		
		String updateString = new String();
		for (int i = 0; i < list.size(); i++) {
			ArchivedFileInfo currentFile = list.get(i);
			{
				String folderType = new String();

				if (currentFile.getPath().toLowerCase().contains("rohscan"))
					folderType = "Rohscan";
				else if (currentFile.getPath().toLowerCase()
						.contains("datenbank"))
					folderType = "datenbankfertig";
				else if (currentFile.getPath().toLowerCase()
						.contains("druckfertig"))
					folderType = "druckfertig";
				else
					folderType = "unbekannt";

				String arachneEntityID = new String();
				String dateinameTivoli = null;

				if (currentFile.getArachneID() == null) {
					arachneEntityID = null;
				} else {
					if (currentFile.isForeignKey()
							&& currentFile.getForcedTable() != null) {
						arachneEntityID = "(SELECT `ArachneEntityID` "
								+ "FROM `arachneentityidentification` "
								+ " WHERE `TableName` = '" + currentFile.getForcedTable() + "' "
								+ " AND `ForeignKey` = " + Long.parseLong(currentFile.getArachneID().replace("BT", ""))
								+ " HAVING COUNT(*) = 1)";
					} else if (currentFile.isForeignKey()) {
						arachneEntityID = "(SELECT `ArachneEntityID` FROM `arachneentityidentification` "
								+ " WHERE `ForeignKey` = " + Long.parseLong(currentFile.getArachneID().replace("BT", ""))
								+ " HAVING COUNT(*) = 1)";
					} else {
						arachneEntityID = currentFile.getArachneID();
					}

					dateinameTivoli = "(SELECT `DateinameMarbilder` FROM `marbilder` "
							+ "WHERE `DateinameMarbilder`='" + currentFile.getName().replace("tif", "jpg") + "')";
				}

				updateString = "INSERT INTO `marbildertivoli` "
						+ "(`FS_ArachneEntityID`,"
						+ " `DateinameMarbilderTivoli`," + " `Dateiname`,"
						+ " `Pfad`," + " `Ordnertyp`," + " `erstellt`,"
						+ " `geaendert`," + " `Katalog`," + " `Volume`,"
						+ " `Dateityp`)" + "VALUES" + "("
						+ arachneEntityID + ", "+ dateinameTivoli + ", "
						+ "'" + currentFile.getName().replaceAll("'", "''") + "', "
						+ "'" + currentFile.getPath().replaceAll("'", "''") + "', "
						+ "'" + folderType	+ "', "
						+ "'" + currentFile.getCreated() + "', "
						+ "'" + currentFile.getLastChanged() + "', "
						+ "'" + currentFile.getCatalog() + "', "
						+ "'" + currentFile.getVolume()	+ "', "
						+ "'" + currentFile.getResourceType() + "');";

				out.println(updateString);
			}

		}
	}

	private static ArachneEntity tryParsingArachneEntityFromFileName(
			String fileName) {
		Matcher matchFile = fileNamePattern.matcher(fileName);

		if (matchFile.matches()) {
			Matcher bauwerkMatcher = bauwerkPattern.matcher(matchFile.group(1));
			if (bauwerkMatcher.matches()) {
				entityCounter++;
				return new ArachneEntity(bauwerkMatcher.group(1), true,
						"bauwerksteil");
			}

			Matcher zeroMatcher = zeroPattern.matcher(matchFile.group(1));
			if (zeroMatcher.matches()) {
				entityCounter++;
				return new ArachneEntity(zeroMatcher.group(1), true, null);
			}

			Matcher numbersMatcher = numbersPattern.matcher(matchFile.group(1));

			if (numbersMatcher.matches()) {
				entityCounter++;
				return new ArachneEntity(matchFile.group(1), false, null);
			}
		}

		return null;
	}

	private static void setIndices(String[] lineContents) {

		indexName = -1;
		indexPath = -1;

		indexCreated = -1;
		indexChanged = -1;

		indexType = -1;

		indexCatalog = -1;
		indexVolume = -1;

		for (int i = 0; i < lineContents.length; i++) {

			if (lineContents[i].compareTo("Name") == 0) {
				indexName = i;
			} else if (lineContents[i].compareTo("Pfad") == 0 || lineContents[i].compareTo("Path") == 0) {
				indexPath = i;
			} else if (lineContents[i].compareTo("Erstelldatum") == 0 || lineContents[i].compareTo("Date Created") == 0) {
				indexCreated = i;
			} else if (lineContents[i].compareTo("Änderungsdatum") == 0 || lineContents[i].compareTo("Date Modified") == 0) {
				indexChanged = i;
			} else if (lineContents[i].compareTo("Art") == 0 || lineContents[i].compareTo("Kind") == 0) {
				indexType = i;
			} else if (lineContents[i].compareTo("Katalog") == 0 || lineContents[i].compareTo("Catalog") == 0) {
				indexCatalog = i;
			} else if (lineContents[i].compareTo("Volume") == 0) {
				indexCatalog = i;
			}
		}
	}
	
	private static PrintWriter createNewOutstream(String currentFileName) throws IOException
	{
		String finalFileName = outputPath + "/" + currentFileName.substring(0,
				currentFileName.lastIndexOf('.'))
				+ "_sqlUpdate_" + resultFileCounter + ".sql";
		
		
		File outputDirectory = new File(finalFileName);
		if (outputDirectory.exists()) {
			System.out.println("File at " + finalFileName + " already exists, appending.");
		} else {
			if (!outputDirectory.createNewFile())
				System.err.println("Could not create output file at "
						+ finalFileName);
			else
				System.out.println("Created new file " + finalFileName);
		}
		
		return new PrintWriter(new FileOutputStream(outputDirectory, true));
	}
}
