package parameter_ids;

/**
 * This class defines how code sections for this parameter should be generated.
 * Advised not to modify the constructors, default values can instead be
 * edited in ParamDefaults.java
 */
public class adb_sensor_status_param_id extends ParamCode {
	// Parameter representation.
	private Param param;
	
	/**
	 * Creates a code generator object for this parameter class, uses specified
	 * values for code generation.
	 * @param param
	 *      Parameter values for which specific code is generated
	 */
	public adb_sensor_status_param_id(Param param) {
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
		lb.add("case " + param.idName + " :");
		lb.setIndent("\t\t\t");
		lb.add("bool status[16];");
		lb.add("uint16_t size2;");
		lb.add("");
		lb.add("read_device_status(status, &size2);");
		lb.add("");
		lb.add("buf[0] = 0;");
		lb.add("");
		lb.add("for(uint8_t i = 0; i < size2; i++) {");
		lb.add("\tbuf[0] |= (status[i] << i);");
		lb.add("}");
		lb.add("");
		lb.add("*((uint8_t*)value) = buf[0];");
		lb.add("*size = 1;");
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


