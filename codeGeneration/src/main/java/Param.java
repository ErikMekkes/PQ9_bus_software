import org.json.JSONArray;
import org.json.JSONException;

public class Param {
	public int id;
	public String enumName;
	public String name;
	public String dataType;
	public String defaultValue;
	
	public Param(int id, String name, String dataType,
	             String defaultValue) {
		this.id = id;
		this.name = name;
		this.enumName = name + "_param_id";
		this.dataType = dataType;
		this.defaultValue = defaultValue;
	}
	
	public Param(String id, String name, String dataType,
	             String defaultValue) {
		try {
			if (null == id) {
				throw new NumberFormatException();
			} else {
				this.id = Integer.parseInt(id);
			}
		} catch (NumberFormatException e) {
			System.err.println("Enum value for parameter is not a number : " +
							id + "," + name + "," + dataType + "," + defaultValue);
		}
		this.name = name;
		this.enumName = name + "_param_id";
		this.dataType = dataType;
		this.defaultValue = defaultValue;
	}
	
	public Param(JSONArray par) {
		try {
			this.id = par.getInt(0);
			this.name = par.getString(1);
			this.enumName = name + "_param_id";
			this.dataType = par.getString(2);
			this.defaultValue = par.getString(3);
		} catch (JSONException e) {
			System.err.println(par.toString() + " : " + e.getMessage());
		}
	}
	
	public String toString() {
		String res = "";
		res += id + ",";
		res += name + ",";
		res += dataType + ",";
		res += defaultValue;
		return res;
	}
}
