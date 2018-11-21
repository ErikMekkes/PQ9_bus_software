//< This file is a template to be used by the subsystem code generator.
//< Indicates a template comment, it will be skipped entirely when generating code.
#include "parameters.h"
// satellite parameter definitions
#include "satellite.h"
#include "devices.h"
#include <stdint.h>
// contains utility functions for conversion between parameter data types.
#include "packet_utilities.h"


// mem_pool is a struct containing all the parameters used by this subsystem.
struct parameters_memory_pool {
	//< TODO: Include the required parameter ids & default values here
	//< The rest of the file will be automatically generated when running the code generator
	//< WARNING: The code generator should have a class for each parameter id
	//<
	//< Use the format: $param$ (descriptive name)_param_id_(datatype suffix) (default value)
	//< Example: $param$ SBSYS_sensor_loop_param_id_32 100000
	//< available data type suffixes are '_16' or '_32'
	$param$ testing_2_param_id_16 0xCAFE;
	$param$ testing_4_param_id_32 0xDEADBEEF;
	$param$ SBSYS_sensor_loop_param_id_32 100000;
	
} mem_pool;

/*
 * Can be called to set all parameters of this subsystem to their default values.
 */
void init_parameters() {
	$initLine$
}

/*
 * Returns the value of the requested pid parameter type.
 * pid: parameter to get
 * value: void pointer, used as
 *
 */
void get_parameter(param_id pid, void* value, uint8_t *buf, uint16_t *size) {
}