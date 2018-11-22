package parameter_ids;

public class testing_2_param_id implements ParamId {
	// name of parameterid used in satellite
	private String paramIdName= "testing_2_param_id";
	// default value to be given when initialized
	private String defaultValue= "0xCAFE";
	// parameter name
	private int type = 16;
	private String name = paramIdName.substring(0,paramIdName.length()-9);

	public testing_2_param_id() {

	}

	public testing_2_param_id(String paramIdName, String defaultValue) {
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
		return null;
	}

	public void setParamIdName(String paramIdName) {
		this.paramIdName = paramIdName;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}


