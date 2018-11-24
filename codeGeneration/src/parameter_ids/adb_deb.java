package parameter_ids;

/**
 * This class defines how code sections for this parameter should be generated.
 * Advised not to modify the constructors, default values can instead be
 * edited in ParamDefaults.java
 */
public class adb_deb extends ParamCode {
	// Parameter representation.
	private Param param;
	
	/**
	 * Creates a code generator object for this parameter class, uses specified
	 * values for code generation.
	 * @param param
	 *      Parameter values for which specific code is generated
	 */
	public adb_deb(Param param) {
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
		return null;
	}

	public String setterFunc() {
		LineBuilder lb = new LineBuilder("\t\t");
		lb.add("case " + param.idName + " :");
		lb.setIndent("\t\t\t");
		lb.add("uint8_t *buf;");
		lb.add("buf = (uint8_t*)value;");
		lb.add("");
		lb.add("burn_sw_num = buf[0];");
		lb.add("burn_feedback = buf[1];");
		lb.add("");
		lb.add("cnv8_16LE(&buf[2], &burn_time);");
		lb.add("");
		lb.add("if(!C_ASSERT(burn_time > 0 && burn_time < 200) == true) {");
		lb.add("\treturn false;");
		lb.add("}");
		lb.add("");
		lb.add("HAL_post_burn_event();");
		lb.addIndentOnly("break;");
		return lb.toString();
	}
	
	public String parSpecific() {
		LineBuilder lb = new LineBuilder();
		lb.add("uint8_t burn_sw_num;");
		lb.add("uint8_t burn_feedback;");
		lb.addIndentOnly("uint16_t burn_time;");
		return lb.toString();
	}
}


