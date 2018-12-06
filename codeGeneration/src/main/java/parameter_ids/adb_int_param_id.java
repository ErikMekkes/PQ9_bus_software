package parameter_ids;

/**
 * This class defines how code sections for this parameter should be generated.
 * Advised not to modify the constructors, default values can instead be
 * edited in ParamDefaults.java
 */
public class adb_int_param_id extends ParamCode {
	// Parameter representation.
	private Param param;
	
	/**
	 * Creates a code generator object for this parameter class, uses specified
	 * values for code generation.
	 * @param param
	 *      Parameter values for which specific code is generated
	 */
	public adb_int_param_id(Param param) {
		super(param);
		this.param = super.getParam();
	}

	public String memPoolStruct() {
		return null;
	}

	public String initFunc() {
		return null;
	}

	public String getterFunc() {
		LineBuilder lb = new LineBuilder("\t\t");
		lb.add("case " + param.enumName + " :");
		lb.setIndent("\t\t\t");
		lb.add("struct tmp_device tmp_dev;");
		lb.add("");
		lb.add("read_device_parameters(ADB_INT_TEMP_DEV_ID, &tmp_dev);");
		lb.add("");
		lb.add("*((uint16_t*)value) = tmp_dev.raw_temp;");
		lb.add("cnv16_8(tmp_dev.raw_temp, buf);");
		lb.add("*size = 2;");
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


