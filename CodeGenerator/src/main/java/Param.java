import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a parameter as a java object, includes the attributes:
 *  - id
 *  - name
 *  - enumName
 *  - dataType
 *  - defaultValue
 *  - ldType
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
	public String dType;
	public String hexId;
	
	/**
	 * Constructor. Accepts an integer as id value.
	 *
	 * @param id
	 * @param name
	 * @param dataType
	 * @param defaultValue
	 */
	Param(int id, String name, String dataType,
	             String defaultValue) {
		this.id = id;
		this.name = name;
		this.enumName = name + "_param_id";
		this.dataType = dataType;
		this.defaultValue = defaultValue;
		this.dType = convertToLongDataType(this.dataType);
		this.hexId = Integer.toHexString(this.id);
	}
	
	/**
	 * Constructor. Accepts a string as id value.
	 *
	 * @param id
	 * @param name
	 * @param dataType
	 * @param defaultValue
	 */
	Param(String id, String name, String dataType,
	             String defaultValue) {
		try {
			if (null == id) {
				throw new NumberFormatException();
			} else {
				this.id = Integer.parseInt(id);
			}
		} catch (NumberFormatException e) {
			Utilities.log("Enum value for parameter is not a number : " +
							id + "," + name + "," + dataType + "," + defaultValue);
		}
		this.name = name;
		this.enumName = name + "_param_id";
		this.dataType = dataType;
		this.defaultValue = defaultValue;
		this.dType = convertToLongDataType(this.dataType);
		this.hexId = Integer.toHexString(this.id);
	}
	
	/**
	 * Constructor. Attempts to create a parameter object from a JSONArray.
	 *
	 * @param par
	 *      JSONArray to convert to parameter.
	 */
	Param(JSONArray par) {
		try {
			this.id = par.getInt(0);
			this.name = par.getString(1);
			this.enumName = name + "_param_id";
			this.dataType = par.getString(2);
			this.defaultValue = par.getString(3);
			this.dType = convertToLongDataType(this.dataType);
			this.hexId = Integer.toHexString(this.id);
		} catch (JSONException e) {
			Utilities.log(par.toString() + " : " + e.getMessage());
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
	
	/**
	 * Sorts a list of parameters based on their ids. Uses a simple top down
	 * merge sort.
	 * @param params
	 *      List of parameters to sort.
	 * @return
	 *      Sorted list of parameters.
	 */
	static ArrayList<Param> sortParams(ArrayList<Param> params) {
		if (null == params) {
			return null;
		}
		int size = params.size();
		if (1 >= size) {
			return params;
		}
		
		ArrayList<Param> left = new ArrayList<>();
		ArrayList<Param> right = new ArrayList<>();
		
		for (int i = 0; i < size; i++) {
			if (i < size/2) {
				left.add(params.get(i));
			} else {
				right.add(params.get(i));
			}
		}
		
		ArrayList<Param> left_sort = sortParams(left);
		ArrayList<Param> right_sort = sortParams(right);
		
		return mergeParams(left_sort, right_sort);
	}
	
	/**
	 * Merges two lists of parameters based on their ids.
	 * @param left
	 *      First list.
	 * @param right
	 *      Second List.
	 * @return
	 *      Merged result of the two lists.
	 */
	private static ArrayList<Param> mergeParams(ArrayList<Param> left,
	                                     ArrayList<Param> right) {
		ArrayList<Param> res = new ArrayList<>();
		
		while (!left.isEmpty() && !right.isEmpty()) {
			if (left.get(0).id <= right.get(0).id) {
				res.add(left.remove(0));
			} else {
				res.add(right.remove(0));
			}
		}
		while (!left.isEmpty()) {
			res.add(left.remove(0));
		}
		while (!right.isEmpty()) {
			res.add(right.remove(0));
		}
		return res;
	}
	
	/**
	 * Converts the standard data type string to a longer name variant used for
	 * XML control files.
	 * @param dataType
	 *      The regular data type that should be converted/
	 * @return
	 */
	private String convertToLongDataType(String dataType) {
		switch (dataType) {
			case "uint16_t" :
				return "short";
			case "uint32_t" :
				return "long";
			default :
				return "";
		}
	}
}
