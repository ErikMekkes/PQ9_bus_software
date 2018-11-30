package parameter_ids;

/**
 * This class defines code generation sections, a parameter code class must
 * provide a function that generates the required code for each section.
 *
 * If a specific code section does not need to be generated the function must
 * still be implemented, but it may simply return null.
 */
public abstract class ParamCode {
	// Parameter representation.
	private Param param;
	
	/**
	 * Creates a code generator object for this parameter class, uses specified
	 * values for code generation.
	 * @param param
	 *      Parameter values for which specific code is generated
	 */
	ParamCode(Param param) {
		this.param = param;
	}
	
	// Generates the code for the mem_pool struct
	public abstract String memPoolStruct();

	// Generates the code for the init_parameters function
	public abstract String initFunc();

	// Generates the code for the get_parameter function
	public abstract String getterFunc();

	// Generates the code for the set_parameter function
	public abstract String setterFunc();
	
	// Generate miscellaneous parameter specific code
	public abstract String parSpecific();
	
	/**
	 * Returns the parameter values used by this code generator class.
	 * @return
	 *      Parameter representation of values used by this code generator class.
	 */
	public Param getParam() {
		return param;
	}
}