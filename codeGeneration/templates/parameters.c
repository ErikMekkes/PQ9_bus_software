//< This file is a template to be used by the subsystem code generator.
//< <- Indicates a template comment, template comments will be skipped entirely
//< and will no longer be visible in the generated code.
#include "parameters.h"
// satellite parameter definitions
#include "satellite.h"
#include "devices.h"
#include <stdint.h>
// contains utility functions for conversion between parameter data types.
#include "packet_utilities.h"


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
 *
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