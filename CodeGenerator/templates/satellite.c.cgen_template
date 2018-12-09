#include "satellite.h"

#include <stdint.h>

uint8_t assertion_last_file = 0;
uint16_t assertion_last_line = 0;

uint8_t services_error_handler(uint16_t fi, uint32_t l, char *e) {

  assertion_last_file = fi;
  assertion_last_line = l;

  return false;
}
