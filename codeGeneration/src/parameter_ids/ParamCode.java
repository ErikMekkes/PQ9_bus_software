package parameter_ids;

/**
 * This interface defines code sections, a parameter code class must
 * provide a function that generates the required code for each section.
 *
 * If a parameter is not used for a specific code section, it must return an
 * empty string for that function.
 */
public interface ParamCode {
	// Generates the code for the mem_pool struct
	public String memPoolStruct();

	// Generates the code for the init_parameters function
	public String initFunc();

	// Generates the code for the get_parameter function
	public String getterFunc();

	// Generates the code for the set_parameter function
	public String setterFunc();
	
	// Generate miscellaneous subsystem specific code
	public String subSpecific();
}