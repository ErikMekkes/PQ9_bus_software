package parameter_ids;

/**
 * This class defines how code sections for this parameter should be generated.
 * Advised not to modify the constructors, default values can instead be
 * edited in ParamDefaults.java
 */
public class testing_2_param_id extends ParamCode {
	// Parameter representation.
	private Param param;
	// Declare commonly used code parts.
	private int dType;
	private String name;
	
	/**
	 * Creates a code generator object for this parameter class, uses specified
	 * values for code generation.
	 * @param param
	 *      Parameter values for which specific code is generated
	 */
	public testing_2_param_id(Param param) {
		super(param);
		this.param = super.getParam();
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
		name = param.enumName.substring(0, param.enumName.length()-9);
	}

	public String memPoolStruct() {
		System.out.println(param.id);
		return "\t" + param.dataType + " " + name + ";";
	}

	public String initFunc() {
		return "\tmem_pool." + name + " = " + param.defaultValue + ";";
	}

	public String getterFunc() {
		LineBuilder lb = new LineBuilder("\t\t");
		lb.add("case " + param.enumName + " :");
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
	
	public String parSpecific() {
		return null;
	}
}


