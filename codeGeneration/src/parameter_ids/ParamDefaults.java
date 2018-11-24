package parameter_ids;

import java.util.HashMap;
import java.util.Map;

/**
 * Lists all the available parameters for code generation and their defaults.
 */
public class ParamDefaults {
	// list of all param_id
	private static final String testing_2_param_id = "testing_2_param_id";
	private static final String testing_4_param_id = "testing_4_param_id";
	private static final String adb_deb_param_id = "adb_deb_param_id";
	private static final String SBSYS_reset_cmd_int_wdg_param_id = "SBSYS_reset_cmd_int_wdg_param_id";
	private static final String SBSYS_reset_clr_int_wdg_param_id = "SBSYS_reset_clr_int_wdg_param_id";
	private static final String SBSYS_sensor_loop_param_id = "SBSYS_sensor_loop_param_id";
	
	// default values of all param_id
	private static final Param testing_2 = new Param(10, testing_2_param_id, "uint16_t", "0xCAFE");
	private static final Param testing_4 = new Param(11, testing_4_param_id, "uint32_t", "0xDEADBEEF");
	private static final Param adb_deb = new Param(49, adb_deb_param_id, "", "");
	private static final Param SBSYS_reset_cmd_int_wdg = new Param(59, SBSYS_reset_cmd_int_wdg_param_id, "", "");
	private static final Param SBSYS_reset_clr_int_wdg = new Param(60, SBSYS_reset_clr_int_wdg_param_id, "", "");
	private static final Param SBSYS_sensor_loop = new Param(61, SBSYS_sensor_loop_param_id, "uint32_t", "100000");
	
	
	/**
	 * Creates a Map for quick lookup of parameters by param_id key String.
	 * @return
	 *      A Map of code generator classes for each parameter, each identifiable
	 *      by their param_id key.
	 */
	public static Map<String, ParamCode> mapDefaultParamCodes() {
		Map<String, ParamCode> paramCodes = new HashMap<>();
		
		paramCodes.put(testing_2_param_id, new testing_2(testing_2));
		paramCodes.put(testing_4_param_id, new testing_4(testing_4));
		paramCodes.put(adb_deb_param_id, new adb_deb(adb_deb));
		paramCodes.put(SBSYS_reset_cmd_int_wdg_param_id, new SBSYS_reset_cmd_int_wdg(SBSYS_reset_cmd_int_wdg));
		paramCodes.put(SBSYS_reset_clr_int_wdg_param_id, new SBSYS_reset_clr_int_wdg(SBSYS_reset_clr_int_wdg));
		paramCodes.put(SBSYS_sensor_loop_param_id, new SBSYS_sensor_loop(SBSYS_sensor_loop));
		
		return paramCodes;
	}
	
}
