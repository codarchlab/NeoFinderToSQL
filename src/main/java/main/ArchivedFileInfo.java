package main;

public class ArchivedFileInfo {
	private String arachneID;
	
	private String catalog;
	private String volume;
	
	private String name;
	private String path;
	
	private String created;
	private String lastChanged;
	
	private boolean foreignKey;
	private String forcedTable;
	
	private String resourceType;
	
	public ArchivedFileInfo(String arachneID, String name, String path, String created, String lastChanged, 
			boolean usesForeignKey, String forcedTable, String catalog, String volume, String resourceType){
		
		this.arachneID = arachneID;
				
		this.name = name;
		this.path = path;
		this.created = created;
		this.lastChanged = lastChanged;
		
		this.foreignKey = usesForeignKey;
		this.forcedTable = forcedTable;
		
		this.catalog = catalog;
		this.volume = volume;
		this.resourceType = resourceType;
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
	
	public String getArachneID() {
		return arachneID;
	}

	public void setArachneID(String arachneID) {
		this.arachneID = arachneID;
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getVolume() {
		return volume;
	}

	public void setVolume(String volume) {
		this.volume = volume;
	}

	public boolean isForeignKey() {
		return foreignKey;
	}

	public void setForeignKey(boolean foreignKey) {
		this.foreignKey = foreignKey;
	}

	public String getForcedTable() {
		return forcedTable;
	}

	public void setForcedTable(String forcedTable) {
		this.forcedTable = forcedTable;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public void printContents()
	{
		System.out.println(name + ", " + path + ", "+ created +", " 
			+ lastChanged + ", " + arachneID + ", " + forcedTable + ", " + foreignKey);
	}
}
