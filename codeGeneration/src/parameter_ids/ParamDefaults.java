package parameter_ids;

public class ParamDefaults {
	// list of all param_id
	public static final String testing_2_param_id = "testing_2_param_id";
	public static final String testing_4_param_id = "testing_4_param_id";
	public static final String SBSYS_sensor_loop_param_id = "SBSYS_sensor_loop_param_id";
	
	// default values of all param_id
	public static final Param testing_2 =
					new Param(10, testing_2_param_id, "uint16_t", "0xCAFE");
	public static final Param testing_4 =
					new Param(11, testing_4_param_id, "uint32_t", "0xDEADBEEF");
	public static final Param SBSYS_sensor_loop =
					new Param(61, SBSYS_sensor_loop_param_id, "uint32_t", "100000");
}
