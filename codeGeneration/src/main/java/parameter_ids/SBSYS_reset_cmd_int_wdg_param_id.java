package parameter_ids;

/**
 * This class defines how code sections for this parameter should be generated.
 * Advised not to modify the constructors, default values can instead be
 * edited in ParamDefaults.java
 */
public class SBSYS_reset_cmd_int_wdg_param_id extends ParamCode {
	// Parameter representation.
	private Param param;
	
	/**
	 * Creates a code generator object for this parameter class, uses specified
	 * values for code generation.
	 * @param param
	 *      Parameter values for which specific code is generated
	 */
	public SBSYS_reset_cmd_int_wdg_param_id(Param param) {
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
		lb.add("struct int_wdg_device dev;");
		lb.add("read_device_parameters(INT_WDG_DEV_ID, &dev);");
		lb.add("dev.cmd = true;");
		lb.add("write_device_parameters(INT_WDG_DEV_ID, &dev);");
		lb.addIndentOnly("break;");
		return lb.toString();
		
	}
	
	public String parSpecific() {
		return null;
	}
}


