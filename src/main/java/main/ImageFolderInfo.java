package main;

import java.util.ArrayList;

public class ImageFolderInfo {
	private String folderName;
	private String path;
	private ArrayList<ImageInfo> images;
	
	public ImageFolderInfo(String folderName, String foldePath)
	{
		this.folderName = folderName;
		this.path = foldePath;
		
		images = new ArrayList<ImageInfo>();
	}
	
	public void addImage(ImageInfo image)
	{
		images.add(image);
	}
	
	public ArrayList<ImageInfo> getImages()
	{
		return images;
	}
}
