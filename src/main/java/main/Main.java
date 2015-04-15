package main;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.Date;


public class Main {

	private static Matcher matcher;
	private static int lineCounter = 0;
	private static int entityCounter = 0;
	private static int folderCounter = 0;
	private static int imageCounter = 0;
	private static int otherCounter = 0;
	
	private static String outputPath;
	
	public static void main(String[] args) {
		
		System.out.println("CSV data has to be tab-separated.");
		List<ArachneEntity> arachneEntities = new ArrayList<ArachneEntity>();
		
		if(args.length < 2)
		{
			System.err.println("No path for csv files provided.");
			return;
		}
		
		File scanDirectory = new File(args[0]);
		
		if(scanDirectory.isDirectory())
		{
			String[] files = scanDirectory.list();
			for(int i = 0; i < files.length; i++)
			{
				if(files[i].endsWith(".csv"))
					arachneEntities.addAll(readCSV(files[i]));
			}
		} 
		else if(scanDirectory.getName().endsWith(".csv"))
		{
			arachneEntities.addAll(readCSV(scanDirectory.getAbsolutePath()));
		}
		
		System.out.println("Processed " + lineCounter + " lines, " + entityCounter + " entities, " + folderCounter +  " folders, " + imageCounter + " images and " + otherCounter + " other.");
		// Schreibe SQL Update
		
		
	}
	
	private static List<ArachneEntity> readCSV(String path) {
		List<ArachneEntity> arachneEntities = new ArrayList<ArachneEntity>();
		File file = new File(path);
		if(!file.canRead())
		{
			System.out.println("Unable to read file: " + path);
			return null;
		}
		
		Pattern arachneIDPattern = Pattern.compile(".*:richtig:\\w+$");
		Pattern seriennummerPattern = Pattern.compile("Seriennummer (\\d+)$");

		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
			String currentLine = null;
		
			ArachneEntity currentEntity = null;
			
			while ((currentLine = reader.readLine()) != null) {
				lineCounter++;
				String[] lineContents = currentLine.split("\\t");
				
				
				if(lineContents.length < 12)
				{
					otherCounter++;
//					System.out.println("No relevant line: " + currentLine);
					continue;
				}		
				
				matcher = arachneIDPattern.matcher(lineContents[1].replace(" ", ""));
				//System.out.println(lineContents[1]);
				if(matcher.matches())
				{
					if(currentEntity != null)
						arachneEntities.add(currentEntity);
					currentEntity = new ArachneEntity(lineContents[0], lineContents[10], lineContents[11]);
					entityCounter++;
					continue;
				}
				
				if(currentEntity == null || !lineContents[1].contains(currentEntity.getArachneID()))
				{
					otherCounter++;
					//System.out.println("Path not matching current entity. Awaiting new entity within folder " + lineContents[1]);
					continue;
				}
				
				if(lineContents[0].endsWith(".tif") || lineContents[0].endsWith(".jpg"))
				{
					ImageInfo image = new ImageInfo(lineContents[0], lineContents[1], lineContents[3], lineContents[4]);
					currentEntity.AddFolder(image);
					imageCounter++;
				}
				else
				{
					folderCounter++;
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
	
	private void writeSQLUpdate(ArrayList<ArachneEntity> data)
	{
		try {

			File outputDirectory = new File(outputPath);
			if(outputDirectory.exists())
			{
				System.err.println("Error, file at " + outputPath + " already exists.");
			}
			else
			{
				if(!outputDirectory.createNewFile())
						System.err.println("Could not create output file at " + outputPath);
				else
					{
						PrintWriter out = new PrintWriter(outputDirectory);
						String updateString = new String();
						for(int i = 0; i < data.size(); i++)
						{
							ArachneEntity currentEntity = data.get(i);
							for(int j = 0; j < currentEntity.getFolders().size(); j++)
							{
								updateString ="INSERT INTO...";
								
								
								out.println(updateString);
							}	
							
						}
					}
				} 
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
