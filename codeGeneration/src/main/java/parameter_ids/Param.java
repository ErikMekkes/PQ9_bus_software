package parameter_ids;

public class Param {
	public int enumValue;
	public String idName;
	public String dataType;
	public String defaultValue;
	
	public Param(int enumValue, String idName, String dataType,
	             String defaultValue) {
		this.enumValue = enumValue;
		this.idName = idName;
		this.dataType = dataType;
		this.defaultValue = defaultValue;
	}
}
