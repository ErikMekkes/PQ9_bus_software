import org.json.JSONArray;
import org.json.JSONException;

/**
 * Represents a parameter as a java object, includes the attributes:
 *  - id
 *  - name
 *  - enumName
 *  - dataType
 *  - defaultValue
 *
 * Includes methods to construct a parameter object from various inputs, as
 * well as a toString implementation.
 */
public class Param {
	public int id;
	public String enumName;
	public String name;
	public String dataType;
	public String defaultValue;
	
	/**
	 * Constructor. Accepts an integer as id value.
	 *
	 * @param id
	 * @param name
	 * @param dataType
	 * @param defaultValue
	 */
	public Param(int id, String name, String dataType,
	             String defaultValue) {
		this.id = id;
		this.name = name;
		this.enumName = name + "_param_id";
		this.dataType = dataType;
		this.defaultValue = defaultValue;
	}
	
	/**
	 * Constructor. Accepts a string as id value.
	 *
	 * @param id
	 * @param name
	 * @param dataType
	 * @param defaultValue
	 */
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
	
	/**
	 * Constructor. Attempts to create a parameter object from a JSONArray.
	 *
	 * @param par
	 *      JSONArray to convert to parameter.
	 */
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
	
	/**
	 * Returns a strign representation of the parameter object.
	 *
	 * @return
	 *      String representation of the parameter object.
	 */
	public String toString() {
		String res = "";
		res += id + ",";
		res += name + ",";
		res += dataType + ",";
		res += defaultValue;
		return res;
	}
}
