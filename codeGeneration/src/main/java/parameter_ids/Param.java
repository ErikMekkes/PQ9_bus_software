package parameter_ids;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	
	public Param(JSONArray par) {
		try {
			this.enumValue = par.getInt(0);
			this.idName = par.getString(1);
			this.dataType = par.getString(2);
			this.defaultValue = par.getString(3);
		} catch (JSONException e) {
			System.err.println(e.getMessage());
		}
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
