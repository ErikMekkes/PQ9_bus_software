package parameter_ids;

/**
 * This class defines how code sections for this parameter should be generated.
 * Advised not to modify the constructors, default values can instead be
 * edited in ParamDefaults.java
 */
public class SBSYS_reset_clr_int_wdg implements ParamCode {
	// Parameter representation.
	private Param param;
	
	/**
	 * Constructor, creates a testing_4 parameter with default values.
	 */
	public SBSYS_reset_clr_int_wdg() {
		param = ParamDefaults.SBSYS_reset_clr_int_wdg;
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
	public SBSYS_reset_clr_int_wdg(int enumValue, String idName, String dataType,
	                               String defaultValue) {
		param = new Param(enumValue, idName, dataType, defaultValue);
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
		lb.add("dev.clr = true;");
		lb.add("write_device_parameters(INT_WDG_DEV_ID, &dev);");
		lb.addIndentOnly("break;");
		return lb.toString();
		
	}
	
	public String subSpecific() {
		return null;
	}
}


