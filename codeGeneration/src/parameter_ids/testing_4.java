package parameter_ids;

/**
 * This class defines how code sections for this parameter should be generated.
 * Advised not to modify the constructors, default values can instead be
 * edited in ParamDefaults.java
 */
public class testing_4 implements ParamCode {
	// Parameter representation.
	private Param param;
	// Declare commonly used code parts.
	private int dType;
	private String name;
	
	/**
	 * Constructor, creates a testing_4 parameter with default values.
	 */
	public testing_4() {
		param = ParamDefaults.testing_4;
		commonFormats();
	}
	
	/**
	 * Constructor, creates a testing_4 parameter with the specified values:
	 * @param idName
	 *      Parameter global identifier name.
	 * @param dataType
	 *      Parameter data type.
	 * @param defaultValue
	 *      Parameter default value.
	 */
	public testing_4(int enumValue, String idName, String dataType,
	                 String defaultValue) {
		param = new Param(enumValue, idName, dataType, defaultValue);
		commonFormats();
	}
	
	/**
	 * Derives parts for code sections that are used often in this class.
	 */
	private void commonFormats() {
		// conversion functions are identified by just the bitcount of datatype
		//TODO: this solution works fine with the current packet utilities code. But
		// using the full data type for the conv function names would be nicer.
		dType = Integer.parseInt(param.dataType.substring(4,6));
		// name used for variable in generated code (can't be the global name)
		name = param.idName.substring(0, param.idName.length()-9);
	}

	public String memPoolStruct() {
		return "\t" + param.dataType + " " + name + ";";
	}

	public String initFunc() {
		return "\tmem_pool." + name + " = " + param.defaultValue + ";";
	}

	public String getterFunc() {
		LineBuilder lb = new LineBuilder("\t\t");
		lb.add("case " + param.idName + " :");
		lb.setIndent("\t\t\t");
		lb.add("*((" + param.dataType + "*)value) = mem_pool." + name + ";");
		lb.add("cnv" + dType + "_8(mem_pool." + name + ", " +	"buf);");
		lb.add("*size = " + dType /8 +";");
		lb.addIndentOnly("break;");
		return lb.toString();
	}

	public String setterFunc() {
		return null;
	}
	
	public String subSpecific() {
		return null;
	}
}


