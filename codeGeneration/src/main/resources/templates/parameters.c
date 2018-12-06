//< This file is a template to be used by the subsystem code generator.
//< TEMPLATE COMMENTS
//< //< Indicates a template comment, these comments will be skipped entirely
//< and will no longer be visible in the generated code.
//<
//< TEMPLATE VARIABLES
//< It is possible to define variables to use in the template with:
//< $var$ variable_identifier variablue_value
//< example : replace every occurrence of 'foo' with 888
//< $var$ foo 888
//<
//< SUB-TEMPLATES
//< It is also possible to include sub-templates with :
//< $template$ template_name
//< The code generator will replace such lines with that template's contents.
//< Subtemplates can contain further subtemplates and so on.
//< NOTE ON VARIABLES IN SUBTEMPLATES :
//< Variable values defined in a subtemplate will only apply within the scope
//< of that subtemplate.
//< Variables from parent templates carry over! there is no warning for this!
//< Variables from parent templates can be redefined with a different value,
//< the generator will offer warnings when a parent's variable is redefined.
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
	$p-template$ [all] mem_pool
	
	$p-template$ [testing_2|testing_4] mem_pool
	
	$p-line$ [all] 	p_dataType p_name;
} mem_pool;

/*
 * Can be called to (re)set all parameters of this subsystem to default values.
 */
void init_parameters() {
	//< This tag indicates init param code should be generated here.
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
		$param$ get_parameter
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
		$param$ set_parameter
		default :
			res = false;
	}

	return res;
}