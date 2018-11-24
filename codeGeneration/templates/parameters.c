//< This file is a template to be used by the subsystem code generator.
//< <- Indicates a template comment, template comments will be skipped entirely
//< and will no longer be visible in the generated code.
//<
//< TODO: It's possible to define required parameter & default values here
//< The rest of the file will be automatically generated when running the code generator
//< WARNING: The code generator should have a class for each used parameter type
//<
//< Use the format: $param$ paramId (default value)
//< Example: $param$ SBSYS_sensor_loop_param_id_32 100000
$param$ SBSYS_sensor_loop_param_id 100000
$param$ testing_2_param_id 0xCAFE
$param$ testing_4_param_id 0xDEADBEEF
$param$ adb_deb_param_id
$param$ SBSYS_reset_cmd_int_wdg_param_id
$param$ SBSYS_reset_clr_int_wdg_param_id
//< The code generator can handle some basic errors and warns about possibly unintended output
$param$ random_unfindable thing
$param$
$param$ no_default_value_specified
#include "parameters.h"
// satellite parameter definitions
#include "satellite.h"
#include "devices.h"
#include <stdint.h>
// contains utility functions for conversion between parameter data types.
#include "packet_utilities.h"

$par_specific$

/*
 * Defines a mem_pool struct containing all the parameters that are required
 * by this subsystem.
 */
struct parameters_memory_pool {
	//< This tag indicates mem_pool struct code should be generated here.
	$mem_pool$
} mem_pool;

/*
 * Can be called to (re)set all parameters of this subsystem to default values.
 */
void init_parameters() {
	//< This tag indicates init param code should be generated here.
	$initParams$
}

/*
 * Returns the current values of the requested parameter id. The pid argument
 * defines the requested parameter, the *value, *buf and *size pointer
 * arguments are used as output for the return values.
 *
 * pid : parameter id (input)
 * *value : parameter value in it's original data type (output)
 * *buf : value of parameter as a uint8_t representation (output)
 * *size : size in bytes of the data type stored in *(*value) (output)
 */
void get_parameter(param_id pid, void *value, uint8_t *buf, uint16_t *size) {
	switch (pid) {
		//< This tag indicates get param code should be generated here
		$getParams$
		default :
			*size = 0;
	}
}

/*
 * Updates the local value to the specified value for the specified parameter id.
 */
bool set_parameter(param_id pid, void* value) {
	bool res = true;
	switch (pid) {
		//< This tag indicates set param code should be generated here
		$setParams$
		default :
			res = false;
	}
	
	return res;
}