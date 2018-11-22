package parameter_ids;

public class SBSYS_sensor_loop_param_id implements ParamId {
	// name of parameterid used in satellite
	private String paramIdName= "SBSYS_sensor_loop_param_id";
	// default value to be given when initialized
	private String defaultValue= "100000";
	private int type = 32;
	// parameter name
	private String name = paramIdName.substring(0,paramIdName.length()-9);

	public SBSYS_sensor_loop_param_id() {

	}

	public SBSYS_sensor_loop_param_id(String paramIdName, String defaultValue) {
		this.paramIdName = paramIdName;
		this.defaultValue = defaultValue;
	}

	public String memPoolStruct() {
		return "\tuint" + type + "_t " + name + ";";
	}

	public String initFunc() {
		return "\tmem_pool." + name + " = " + defaultValue + ";";
	}

	public String getterFunc() {
		String baseIndent = "\t\t";
		String func = baseIndent + "case " + paramIdName + " :\n";
		func += baseIndent + "\t*((uint" + type + "_t*)value) = mem_pool." + name + ";\n";
		func += baseIndent + "\tcnv" + type + "_8(mem_pool." + name + ", buf);\n";
		func += baseIndent + "\t*size = " + type/8 +";\n";
		func += baseIndent + "\tbreak;";
		return func;
	}

	public String setterFunc() {
		String baseIndent = "\t\t";
		String func = baseIndent + "case " + paramIdName + " :\n";
		func += baseIndent + "\tuint8_t *buf;\n";
		func += baseIndent + "\tbuf = (uint8_t*)value;\n";
		func += baseIndent + "\tcnv8_" + type + "LE(&buf[0], &mem_pool." + name +
				" );\n";
		func += baseIndent + "\tbreak;";
		return func;
	}

	public void setParamIdName(String paramIdName) {
		this.paramIdName = paramIdName;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}


