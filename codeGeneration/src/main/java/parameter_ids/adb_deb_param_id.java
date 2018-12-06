package parameter_ids;

/**
 * This class defines how code sections for this parameter should be generated.
 * Advised not to modify the constructors, default values can instead be
 * edited in ParamDefaults.java
 */
public class adb_deb_param_id extends ParamCode {
	// Parameter representation.
	private Param param;
	
	/**
	 * Creates a code generator object for this parameter class, uses specified
	 * values for code generation.
	 * @param param
	 *      Parameter values for which specific code is generated
	 */
	public adb_deb_param_id(Param param) {
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
		lb.add("struct dep_device dev;");
		lb.add("");
		lb.add("read_device_parameters(ADB_DEP_DEV_ID, &dev);");
		lb.add("");
		lb.add("buf[0] = 0;");
		lb.add("buf[1] = 0;");
		lb.add("");
		lb.add("buf[1] = (dev.b1_status << 3) | \\");
		lb.add("(dev.b2_status << 2) | \\");
		lb.add("(dev.b3_status << 1) | \\");
		lb.add("dev.b4_status;");
		lb.add("");
		lb.add("buf[0] = (dev.b1_enabled << 7) | \\");
		lb.add("(dev.b2_enabled << 6) | \\");
		lb.add("(dev.b3_enabled << 5) | \\");
		lb.add("(dev.b4_enabled << 4) | \\");
		lb.add("(dev.b1_state << 3) | \\");
		lb.add("(dev.b2_state << 2) | \\");
		lb.add("(dev.b3_state << 1) | \\");
		lb.add("dev.b4_state;");
		lb.add("");
		lb.add("*((uint16_t*)value) = (buf[1] << 8) | buf[0];");
		lb.add("*size = 2;");
		lb.addIndentOnly("break;");
		return lb.toString();
	}

	public String setterFunc() {
		LineBuilder lb = new LineBuilder("\t\t");
		lb.add("case " + param.enumName + " :");
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


