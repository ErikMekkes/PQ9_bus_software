#include "parameters.h"
#include "satellite.h"
#include "devices.h"
#include <stdint.h>
//< Only  implemented by RED & COMMS
#include "packet_utilities.h"


struct parameters_memory_pool {
	uint16_t testing_2;
	uint32_t testing_4;
	//< Not implemented by ADB & ADCS
	uint32_t testing_4_rw;
	
	//< Not implemented by RED
	uint32_t sensor_loop;
	//< Only implemented by OBC
	uint32_t command_loop;
	
	//< Only implemented by EPS
	uint16_t bus1_current_threshold;
	uint16_t bus2_current_threshold;
	uint16_t bus3_current_threshold;
	uint16_t bus4_current_threshold;
} mem_pool;


void init_parameters() {
	mem_pool.testing_2 = 0xCAFE;
	mem_pool.testing_4 = 0xDEADBEEF;
	//< when implemented
	mem_pool.testing_4_rw = 0xDEADBEEF;
	
	mem_pool.sensor_loop = 100000;
	//< when implemented
	mem_pool.command_loop = 900000;
	
	//< When implemented
	mem_pool.bus1_current_threshold = 0xFFFF;
	mem_pool.bus2_current_threshold = 0xFFFF;
	mem_pool.bus3_current_threshold = 0xFFFF;
	mem_pool.bus4_current_threshold = 0xFFFF;
}


void get_parameter(param_id pid, void* value, uint8_t *buf, uint16_t *size) {
}