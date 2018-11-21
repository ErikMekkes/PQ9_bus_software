package parameter_ids;

public class SBSYS_sensor_loop_param_id_32 implements ParamID {
    // name of parameterid used in satellite
    private String paramIdName= "SBSYS_sensor_loop_param_id_32";
    // default value to be given when initialized
    private String defaultValue= "100000";
    // parameter name
    private String name = paramIdName.substring(0,paramIdName.length()-12);

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
        String func = "\tcase " + paramIdName + " :\n";
        func += "\t\t*((uint32_t*)value) = mem_pool." + name + ";\n";
        func += "\t\tcnv32_8(mem_pool." + name + ", buf);\n";
        func += "\t\t*size = 4;\n";
        func += "\t\tbreak;\n";
        return func;
    }

    public String setterFunc() {
        String func = "\tcase " + paramIdName + " :\n";
        func += "uint8_t *buf;";
        func += "buf = (uint8_t*)value;";
        func += "cnv8_32LE(&buf[0], &mem_pool." + name + " );";
        func += "break;\n";
        return func;
    }

    public void setParamIdName(String paramIdName) {
        this.paramIdName = paramIdName;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}


