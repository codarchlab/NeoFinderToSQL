package main;

import java.util.*;
import java.util.regex.*;
import java.io.*;

public class Main {

	private static int lineCounter = 0;
	private static int entityCounter = 0;
	private static int folderCounter = 0;
	private static int fileCounter = 0;
	private static int otherCounter = 0;

	private static int sqlLineCounter = 0;
	private static int sqlLineLimit = 500;
	private static int sqlCounter = 0;

	private static int indexName;
	private static int indexPath;
	private static int indexType;

	private static int indexCreated;
	private static int indexChanged;

	private static int indexCatalog;
	private static int indexVolume;

	private static boolean firstLine = false;

	private static Pattern bauwerkPattern = Pattern.compile("^BT(\\d+)$");
	private static Pattern zeroPattern = Pattern.compile("^0+(\\d+)$");
	private static Pattern numbersPattern = Pattern.compile("\\d+");
	private static Pattern fileNamePattern = Pattern
			.compile("^.*_(\\w+)(,\\d{2})?\\.\\w{3}$");

	private static String outputPath = "results";

	public static void main(String[] args) {
		
		for(int i = 0; i < args.length; i++)
		{
			System.out.println(args[i]);
		}
		
		
		File outputFolder = new File(outputPath);
		if (!outputFolder.exists())
			outputFolder.mkdirs();

		if (args.length != 2) {
			System.out
					.println("Needings two parameters: arg[0] = source folder or file, arg[1] = number of lines per SQL-update file.");
			return;
		}

		File scanDirectory = new File(args[0]);

		if (!scanDirectory.exists()) {
			System.out.println("Source " + args[0] + " does not exist.");
			return;
		}

		try {
			sqlLineLimit = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.out.println("Invalid sql line limit: " + args[1]);
			return;
		}

		if (scanDirectory.isDirectory()) {
			String[] files = scanDirectory.list();
			for (int i = 0; i < files.length; i++) {
				if (files[i].endsWith(".csv")) {
					readCSV(files[i]);
				}
			}
		} else if (scanDirectory.getName().endsWith(".csv")) {
			readCSV(scanDirectory.getAbsolutePath());
			
		}

	}

	private static void readCSV(String path) {
		firstLine = true;
		sqlCounter = 0;
		sqlLineCounter = 0;
		
		File file = new File(path);
		if (!file.canRead()) {
			System.out.println("Unable to read file: " + path);
			return;
		}
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
			String currentLine = null;

			while ((currentLine = reader.readLine()) != null) {
				lineCounter++;
				String[] lineContents = currentLine.split("\\t");

				for (int i = 0; i < lineContents.length; i++)
					lineContents[i] = lineContents[i].trim();

				if (lineContents.length < 12) {
					otherCounter++;
					// System.out.println("Skipping line: " + currentLine);
					continue;
				}
				if (lineContents[0].compareTo("Name") == 0 && firstLine) {
					System.out.println("New file: " + currentLine);
					firstLine = false;
					setIndices(lineContents);
					continue;
				}

				if (lineContents[indexType].compareTo("Ordner") == 0 || lineContents[indexType].compareTo("Folder") == 0) {
					folderCounter++;
				} else {
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
								|| currentPath.toLowerCase()
										.contains("rohscan")
								|| currentPath.toLowerCase().contains("druck")) {
							entityInfo = tryParsingArachneEntityFromFileName(lineContents[indexName]);
						}

						String currentArachneID = (entityInfo != null) ? entityInfo.arachneID
								: null;
						String currentRestrictingTable = (entityInfo != null) ? entityInfo.restrictingTable
								: null;
						boolean currentForeignKey = (entityInfo != null) ? entityInfo.foreignKey
								: false;

						ArchivedFileInfo fileInfo = new ArchivedFileInfo(
								currentArachneID, currentName, currentPath,
								currentCreated, currentChanged,
								currentForeignKey, currentRestrictingTable,
								currentCatalog, currentVolume, currentType);

						ArrayList<ArchivedFileInfo> list = new ArrayList<ArchivedFileInfo>();

						list.add(fileInfo);

						if (sqlLineCounter == sqlLineLimit) {
							sqlCounter++;
							sqlLineCounter = 0;
						} else {
							sqlLineCounter++;
						}

						writeSQLUpdate(
								list,
								file.getName().substring(0,
										file.getName().lastIndexOf('.'))
										+ "_sqlUpdate_" + sqlCounter + ".sql");
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println("Wrong index at line:");
						System.out.println(currentLine);
						System.out.println(indexName + " " + indexPath + " "
								+ indexCreated + " " + indexChanged + " "
								+ indexType + " " + indexCatalog + " "
								+ indexVolume);
					}

				}
				lineContents = null;
			}
			reader.close();
			
			System.out.println("Processed " + path + ": " + lineCounter + " lines, "
					+ entityCounter + " entities, " + folderCounter
					+ " folders, " + fileCounter + " files and " + otherCounter
					+ " other.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeSQLUpdate(List<ArchivedFileInfo> list,
			String fileName) {
		try {
			File outputDirectory = new File(outputPath + "/" + fileName);
			if (outputDirectory.exists()) {
				// System.out.println("File at " + path +
				// " already exists, appending.");
			} else {
				if (!outputDirectory.createNewFile())
					System.err.println("Could not create output file at "
							+ fileName);
				else
					System.out.println("Created new file " + fileName);
			}

			PrintWriter out = new PrintWriter(new FileOutputStream(
					outputDirectory, true));

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
							+ "'" + currentFile.getResourceType() + "')"
							+ " ON DUPLICATE KEY UPDATE "
							+ "`FS_ArachneEntityID` = "	+ arachneEntityID + ", "
							+ "`DateinameMarbilderTivoli`="	+ dateinameTivoli + ", "
							+ "`Dateiname`='" + currentFile.getName().replaceAll("'", "''") + "', "
							+ "`Pfad`='" + currentFile.getPath().replaceAll("'", "''") + "', "
							+ "`Ordnertyp`='" + folderType + "', "
							+ "`erstellt`='" + currentFile.getCreated()	+ "', "
							+ "`geaendert`='" + currentFile.getLastChanged() + "', "
							+ "`Katalog`='"	+ currentFile.getCatalog() + "', "
							+ "`Volume`='" + currentFile.getVolume() + "', "
							+ "`Dateityp`='" + currentFile.getResourceType() + "';";

					out.println(updateString);
				}

			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.lang.NumberFormatException e) {
			e.printStackTrace();
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
		int setIndices = 0;

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
				setIndices++;
			} else if (lineContents[i].compareTo("Pfad") == 0 || lineContents[i].compareTo("Path") == 0) {
				indexPath = i;
				setIndices++;
			} else if (lineContents[i].compareTo("Erstelldatum") == 0 || lineContents[i].compareTo("Date Created") == 0) {
				indexCreated = i;
				setIndices++;
			} else if (lineContents[i].compareTo("Änderungsdatum") == 0 || lineContents[i].compareTo("Date Modified") == 0) {
				indexChanged = i;
			} else if (lineContents[i].compareTo("Art") == 0 || lineContents[i].compareTo("Kind") == 0) {
				indexType = i;
				setIndices++;
			} else if (lineContents[i].compareTo("Katalog") == 0 || lineContents[i].compareTo("Catalog") == 0) {
				indexCatalog = i;
				setIndices++;
			} else if (lineContents[i].compareTo("Volume") == 0) {
				indexCatalog = i;
				setIndices++;
			}

		}

		if (setIndices < 6) {
			System.err.println("Only found " + setIndices + " indices.");
			System.err.println(indexName + " " + indexPath + " " + indexCreated
					+ " " + indexChanged + " " + indexType + " " + indexCatalog
					+ " " + indexVolume);

		}
	}
}
