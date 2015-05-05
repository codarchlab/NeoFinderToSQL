package main;

public class ArachneEntity {
	public String arachneID;
	public boolean foreignKey;
	public String restrictingTable;
	
	public ArachneEntity(String arachneID, boolean useForeignKey, String restrictingTable)
	{
		this.arachneID = arachneID;
		this.foreignKey = useForeignKey;
		this.restrictingTable = restrictingTable;
	}
}