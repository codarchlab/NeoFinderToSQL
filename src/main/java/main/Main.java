package main;

import java.io.*;
import java.util.*;
import java.util.regex.*;


public class Main {
	private static ArachneEntity currentEntityInfo;
	
	private static int lineCounter = 0;
	private static int entityCounter = 0;
	private static int folderCounter = 0;
	private static int fileCounter = 0;
	private static int filesWithArachneIDCounter = 0;
	private static int otherCounter = 0;
	
	
	private static Pattern richtigPattern = Pattern.compile(".*:richtig:(\\w+)$");
	private static Pattern bauwerkPattern = Pattern.compile("^BT(.*)$");
	private static Pattern zeroPattern = Pattern.compile("^0+(\\w+)$");
	private static Pattern numbersPattern = Pattern.compile("\\d+");
	private static Pattern fileNamePattern = Pattern.compile("^.*_(\\w+)(,\\d{2})?\\.\\w{3}$");

	private static String outputPath;

	public static void main(String[] args) {

		System.out.println("CSV data has to be tab-separated.");

		if(args.length < 2)
		{
			System.err.println("No path for csv files provided.");
			return;
		}

		File scanDirectory = new File(args[0]);
		outputPath = args[1];

		if(scanDirectory.isDirectory())
		{
			String[] files = scanDirectory.list();
			for(int i = 0; i < files.length; i++)
			{
				if(files[i].endsWith(".csv"))
				{
					writeSQLUpdate(readCSV(files[i]));
					
					System.out.println("Processed " + lineCounter + " lines, " + entityCounter 
						+ " entities, " + folderCounter +  " folders, " + fileCounter 
						+ " files and " + otherCounter + " other.");
				}
			}
		} 
		else if(scanDirectory.getName().endsWith(".csv"))
		{
			writeSQLUpdate(readCSV(scanDirectory.getAbsolutePath()));
			System.out.println("Processed " + lineCounter + " lines, " + entityCounter 
					+ " entities, " + folderCounter +  " folders, " + fileCounter 
					+ " files and " + otherCounter + " other.");
		}

		
	}

	private static List<ArchivedFileInfo> readCSV(String path) {
		currentEntityInfo = null;
		List<ArchivedFileInfo> arachneEntities = new ArrayList<ArchivedFileInfo>();
		File file = new File(path);
		if(!file.canRead())
		{
			System.out.println("Unable to read file: " + path);
			return null;
		}
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
			String currentLine = null;
			
			while ((currentLine = reader.readLine()) != null) {
				lineCounter++;
				String[] lineContents = currentLine.split("\\t");
				
				for(int i = 0; i < lineContents.length; i++)
					lineContents[i] = lineContents[i].trim();
				
				if(lineContents.length < 12 || lineContents[0].compareTo("Name") == 0)
				{
					otherCounter++;
					//System.out.println("Skipping line: " + currentLine);
					continue;
				}
				
				if(lineContents[9].compareTo("Ordner") == 0)
				{
					folderCounter++;
					updateCurrentArachneEntityInfo(lineContents);					
				}
				else
				{						
					fileCounter++;
					if(currentEntityInfo != null)
					{
						arachneEntities.add(new ArchivedFileInfo(currentEntityInfo.arachneID, 
								lineContents[0], lineContents[1], lineContents[3], lineContents[4], 
								currentEntityInfo.foreignKey, currentEntityInfo.restrictingTable,
								lineContents[10], lineContents[11]));
					}	
					else
					{			
//						if(lineContents[1].toLowerCase().contains("datenbank") || 
//								lineContents[1].toLowerCase().contains("druckfertig") ||
//								lineContents[1].toLowerCase().contains("rohscan"))
//						{
						ArachneEntity info = tryParsingArachneEntityFromFileName(lineContents[0]);
						if(info != null)
						{}
//						}
						
						
						arachneEntities.add(new ArchivedFileInfo(null, 
								lineContents[0], lineContents[1], lineContents[3], lineContents[4], 
								false, null, lineContents[10], lineContents[11]));
					}									
				}
			}   
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return arachneEntities;
	}

	private static void writeSQLUpdate(List<ArchivedFileInfo> list)
	{
		try {
			File outputDirectory = new File(outputPath);
			if(outputDirectory.exists())
			{
				System.out.println("File at " + outputPath + " already exists, appending.");
			}
			else
			{
				if(!outputDirectory.createNewFile())
					System.err.println("Could not create output file at " + outputPath);
				else
					System.out.println("Created new file " + outputPath);
			}

			PrintWriter out = new PrintWriter(new FileOutputStream(outputDirectory, true));

			String updateString = new String();
			for(int i = 0; i < list.size(); i++)
			{
				ArchivedFileInfo currentFile = list.get(i);
				{
					String folderType = new String();

					if(currentFile.getPath().toLowerCase().contains("rohscan"))
						folderType = "Rohscan";
					else if(currentFile.getPath().toLowerCase().contains("datenbank"))
						folderType = "datenbankFertig";
					else if(currentFile.getPath().toLowerCase().contains("druckfertig"))
						folderType = "druckfertig";
					else
						folderType = "unbekannt";

					String arachneEntityID = new String();
					String dateinameTivoli = null;
					
					if(currentFile.getArachneID() == null)
					{
						arachneEntityID = null;
					}
					else if(currentFile.isForeignKey() && currentFile.getForcedTable() != null)
					{
						arachneEntityID = "(SELECT `ArachneEntityID` FROM `arachneentityidentification` "
								+ " WHERE `TableName` = '"+ currentFile.getForcedTable() +"' "
								+ " AND `ForeignKey` = " + Long.parseLong(currentFile.getArachneID().replace("BT", "")) + ")"; 			
						dateinameTivoli = "'" + currentFile.getName().replace("tif", "jpg") + "'";
					}
					else if(currentFile.isForeignKey())
					{
						arachneEntityID = "(SELECT `ArachneEntityID` FROM `arachneentityidentification` "
								+ " WHERE `ForeignKey` = " + Long.parseLong(currentFile.getArachneID().replace("BT", "")) + ")"; 			
						dateinameTivoli =  "'" + currentFile.getName().replace("tif", "jpg") + "'";
					}
					else 
					{
						arachneEntityID = currentFile.getArachneID();
						dateinameTivoli = "'" + currentFile.getName().replace("tif", "jpg") + "'";
					}
					
					updateString = "INSERT INTO `marbildertivoli` "
							+ "(`FS_ArachneEntityID`,"
							+ " `DateinameMarbilderTivoli`,"
							+ " `Dateiname`,"
							+ " `Pfad`,"
							+ " `Ordnertyp`,"
							+ " `erstellt`,"
							+ " `geaendert`,"
							+ " `Katalog`,"
							+ " `Volume`)"
							+ "VALUES"
							+ "("+arachneEntityID+","
							+ " "+dateinameTivoli+","
							+ " '"+currentFile.getName()+"',"
							+ " '"+currentFile.getPath()+"',"
							+ " '"+folderType+"',"
							+ " '"+currentFile.getCreated()+"',"
							+ " '"+currentFile.getLastChanged()+"',"
							+ " '"+currentFile.getCatalog()+"',"
							+ " '"+currentFile.getVolume()+"'"
							+ " )"
							+ " ON DUPLICATE KEY UPDATE "
							+ "`FS_ArachneEntityID` = "+arachneEntityID+", "
							+ "`DateinameMarbilderTivoli`="+dateinameTivoli+", "
							+ "`Dateiname`='"+currentFile.getName()+"', "
							+ "`Pfad`='"+currentFile.getPath()+"', "
							+ "`Ordnertyp`='"+folderType+"', "
							+ "`erstellt`='"+currentFile.getCreated()+"', "
							+ "`geaendert`='"+currentFile.getLastChanged()+"', "
							+ "`Katalog`='"+currentFile.getCatalog()+"', "
							+ "`Volume`='"+currentFile.getVolume()+"';";

					out.println(updateString);
				}	

			}
			out.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void updateCurrentArachneEntityInfo(String[] lineContents)
	{
		if(currentEntityInfo != null && lineContents[1].contains(currentEntityInfo.arachneID)) // we are just in a subfolder
			return;
		
		Matcher richtigMatcher = richtigPattern.matcher(lineContents[1]);
		
		if(richtigMatcher.matches())
		{
			Matcher bauwerkMatcher = bauwerkPattern.matcher(richtigMatcher.group(1));			
			if(bauwerkMatcher.matches())
			{
				currentEntityInfo = new ArachneEntity(bauwerkMatcher.group(1), true, "bauwerksteil");
				entityCounter++;				
				return;
			}
			
			Matcher zeroMatcher = zeroPattern.matcher(richtigMatcher.group(1));			
			if(zeroMatcher.matches())
			{
				currentEntityInfo = new ArachneEntity(zeroMatcher.group(1), true, null);
				entityCounter++;
				return;
			}	
			
			Matcher numbersMatcher = numbersPattern.matcher(richtigMatcher.group(1));
			
			if(numbersMatcher.matches())
			{
				currentEntityInfo = new ArachneEntity(richtigMatcher.group(1), false, null);
				entityCounter++;
				return;
			}
		}		
		currentEntityInfo = null;
	}
	
	private static ArachneEntity tryParsingArachneEntityFromFileName(String fileName)
	{
		Matcher matchFile = fileNamePattern.matcher(fileName);
		
		if(matchFile.matches())
		{
			Matcher bauwerkMatcher = bauwerkPattern.matcher(matchFile.group(1));			
			if(bauwerkMatcher.matches())
			{
				entityCounter++;				
				return new ArachneEntity(bauwerkMatcher.group(1), true, "bauwerksteil");
			}
			
			Matcher zeroMatcher = zeroPattern.matcher(matchFile.group(1));			
			if(zeroMatcher.matches())
			{
				entityCounter++;
				return new ArachneEntity(zeroMatcher.group(1), true, null);
			}	
			
			Matcher numbersMatcher = numbersPattern.matcher(matchFile.group(1));
			
			if(numbersMatcher.matches())
			{
				entityCounter++;
				return new ArachneEntity(matchFile.group(1), false, null);
			}
			System.out.println("could not match: " + fileName);
		}	
		
		return null;
	}
}
