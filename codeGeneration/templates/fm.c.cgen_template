//< Indicates a template comment, it will be skipped entirely when generating code.
//<
#include "fm.h"

#include "hal_functions.h"
#include "hal_subsystem.h"
#include "subsystem.h"

//<ADB, RED and COMMS have deviating implementation
bool fm_set_parameter(param_id pid, FM_fun_id fid, uint8_t *data) {
	bool res = false;
	
	if(fid == P_ON || pid == P_OFF) {
		res = set_parameter(pid, fid);
	} else if(pid == SET_VAL) {
		res = set_parameter(pid, *data);
	}
	
	return res;
}

//<RED and COMMS do not include getter
void fm_get_parameter(param_id pid) {

}