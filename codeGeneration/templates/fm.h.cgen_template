//< Indicates a template comment, it will be skipped entirely when generating code.
#ifndef __FM_H
#define __FM_H

#include <stdint.h>
#include "satellite.h"

//<ADB, RED and COMMS have deviating implementation
bool fm_set_parameter(param_id pid, FM_fun_id fid, uint8_t *data);

//<RED and COMMS do not include getter
void fm_get_parameter(param_id pid);

#endif
