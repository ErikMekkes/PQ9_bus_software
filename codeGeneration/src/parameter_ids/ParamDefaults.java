package parameter_ids;

public class ParamDefaults {
	// list of all param_id
	public static final String testing_2_param_id = "testing_2_param_id";
	public static final String testing_4_param_id = "testing_4_param_id";
	public static final String adb_deb_param_id = "adb_deb_param_id";
	public static final String SBSYS_reset_cmd_int_wdg_param_id = "SBSYS_reset_cmd_int_wdg_param_id";
	public static final String SBSYS_reset_clr_int_wdg_param_id = "SBSYS_reset_clr_int_wdg_param_id";
	public static final String SBSYS_sensor_loop_param_id = "SBSYS_sensor_loop_param_id";
	
	// default values of all param_id
	public static final Param testing_2 =
					new Param(10, testing_2_param_id, "uint16_t", "0xCAFE");
	public static final Param testing_4 =
					new Param(11, testing_4_param_id, "uint32_t", "0xDEADBEEF");
	public static final Param adb_deb =
					new Param(49, adb_deb_param_id, "", "");
	public static final Param SBSYS_reset_cmd_int_wdg =
					new Param(59, SBSYS_reset_cmd_int_wdg_param_id, "", "");
	public static final Param SBSYS_reset_clr_int_wdg =
					new Param(60, SBSYS_reset_clr_int_wdg_param_id, "", "");
	public static final Param SBSYS_sensor_loop =
					new Param(61, SBSYS_sensor_loop_param_id, "uint32_t", "100000");
}
