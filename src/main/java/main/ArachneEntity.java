package main;

import java.util.ArrayList;

public class ArachneEntity {
	private String arachneID;
	private String catalogName;
	private String volumeName;
	private ArrayList<ImageInfo> images;
	
	public ArachneEntity(String arachneID, String catalogName, String volumeName)
	{
		
		this.arachneID = arachneID;
		this.catalogName = catalogName;
		this.volumeName = volumeName;
		images = new ArrayList<ImageInfo>();
//		System.out.println("Creating new arachne entity:");
//		printContents();
	}

	public String getArachneID() {
		return arachneID;
	}

	public String getCatalogName() {
		return catalogName;
	}

	public String getVolumeName() {
		return volumeName;
	}

	public void AddFolder(ImageInfo folderInfo)
	{
		images.add(folderInfo);
	}
	
	public ArrayList<ImageInfo> getFolders()
	{
		return images;
	}
	
	public void printContents()
	{
		System.out.println("ArachneID: " + arachneID);
		System.out.println("Catalog: " + catalogName);
		System.out.println("Volume: " + volumeName);
		
		for(int i = 0; i < images.size(); i++)
		{
			images.get(i).printContents();
		}
	}	
}
