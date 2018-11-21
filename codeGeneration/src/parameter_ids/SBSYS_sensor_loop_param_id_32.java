package parameter_ids;

public class SBSYS_sensor_loop_param_id_32 implements ParamID {
    // name of parameterid used in satellite
    private String paramIdName= "SBSYS_sensor_loop_param_id_32";
    // default value to be given when initialized
    private String defaultValue= "100000";
    // parameter name
    private String name = paramIdName.substring(0,paramIdName.length()-12);
    private String baseIndent = "\t\t";

    public SBSYS_sensor_loop_param_id_32(String paramIdName, String defaultValue) {
        this.paramIdName = paramIdName;
        this.defaultValue = defaultValue;
    }

    public String memPoolStruct() {
        return "\tuint32_t " + name + ";\n";
    }

    public String initFunc() {
        return "\tmem_pool." + name + " = " + defaultValue + ";\n";
    }

    public String getterFunc() {
        String func = baseIndent + "case " + paramIdName + " :\n";
        func += baseIndent + "\t*((uint32_t*)value) = mem_pool." + name + ";\n";
        func += baseIndent + "\tcnv32_8(mem_pool." + name + ", buf);\n";
        func += baseIndent + "\t*size = 4;\n";
        func += baseIndent + "\tbreak;";
        return func;
    }

    public String setterFunc() {
        String func = baseIndent + "case " + paramIdName + " :\n";
        func += baseIndent + "uint8_t *buf;\n";
        func += baseIndent + "buf = (uint8_t*)value;\n";
        func += baseIndent + "cnv8_32LE(&buf[0], &mem_pool." + name + " );\n";
        func += baseIndent + "break;";
        return func;
    }

    public void setParamIdName(String paramIdName) {
        this.paramIdName = paramIdName;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}


