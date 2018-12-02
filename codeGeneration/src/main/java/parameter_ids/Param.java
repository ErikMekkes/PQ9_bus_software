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
	
	public Param(String enumValue, String name, String dataType,
	             String defaultValue) {
		try {
			if (null == enumValue) {
				throw new NumberFormatException();
			} else {
				this.enumValue = Integer.parseInt(enumValue);
			}
		} catch (NumberFormatException e) {
			System.err.println("Enum value for parameter is not a number : " +
							enumValue + "," + name + "," + dataType + "," + defaultValue);
		}
		this.idName = name;
		this.dataType = dataType;
		this.defaultValue = defaultValue;
	}
	
	public String toString() {
		String res = "";
		res += enumValue + ",";
		res += idName + ",";
		res += dataType + ",";
		res += defaultValue;
		return res;
	}
}
