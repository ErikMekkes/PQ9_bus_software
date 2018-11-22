package parameter_ids;

public interface ParamID {
    // Generates the code for the mem_pool struct
    public String memPoolStruct();

    // Generates the code for the init_parameters function
    public String initFunc();

    // Generates the code for the get_parameter function
    public String getterFunc();

    // Generates the code for the set_parameter function
    public String setterFunc();

    // Updates the paramIDName
    public void setParamIdName(String idName);

    // Updates the defaultValue
    public void setDefaultValue(String dValue);
}