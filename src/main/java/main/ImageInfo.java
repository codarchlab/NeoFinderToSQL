package main;

public class ImageInfo {
	private String name;
	private String path;
	
	private String created;
	private String lastChanged;
	
	public ImageInfo(String name, String path, String created, String lastChanged){
		this.name = name;
		this.path = path;
		this.created = created;
		this.lastChanged = lastChanged;
	}
	
	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public String getCreated() {
		return created;
	}

	public String getLastChanged() {
		return lastChanged;
	}
	
	public void printContents()
	{
		System.out.println(name + ", " + path + ", "+ created +", " + lastChanged);
	}
}
