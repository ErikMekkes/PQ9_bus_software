#include "devices.h"

#include "hal_functions.h"
#include "hal_subsystem.h"

#include "INA226.h"
#include "TMP100.h"
#include "MB85RS256A.h"

#define MAX_TMP_DEVS 1
#define MAX_INA_DEVS 4

struct tmp_device tmp_dev[MAX_TMP_DEVS];
struct ina_device ina_dev[MAX_INA_DEVS];
struct tmp_device int_temp_dev[1];
struct int_wdg_device int_wdg_dev;

bool tmp_status[MAX_TMP_DEVS];
bool ina_status[MAX_INA_DEVS];
bool int_temp_status[1];

void device_init() {

  HAL_peripheral_open();

  uint8_t i;

  for(i = 0; i < MAX_TMP_DEVS; i++) {
    tmp_dev[i].id = ADCS_TEMP_DEV_ID;
    tmp_dev[i].temp = 0;
    tmp_dev[i].raw_temp = 0;
    tmp_init(tmp_dev[i].id);
  }

  for(i = 0; i < MAX_INA_DEVS; i++) {
    ina_dev[i].id = i + ADCS_1_MON_DEV_ID;
    ina_dev[i].voltage = 0;
    ina_dev[i].current = 0;
    ina_dev[i].power = 0;
    ina_dev[i].voltage_raw = 0;
    ina_dev[i].current_raw = 0;
    ina_dev[i].power_raw = 0;
    ina_dev[i].shunt = 0.040;
    INA226_configure(ina_dev[i].id,
                      INA226_AVERAGES_1,
                      INA226_BUS_CONV_TIME_1100US,
                      INA226_SHUNT_CONV_TIME_1100US,
                      INA226_MODE_SHUNT_BUS_CONT);
    usleep(10);
      // Calibrate INA226. Rshunt = 0.01 ohm, Max excepted current = 4A
    INA226_calibrate(ina_dev[i].id,
                     0.04,
                     0.5,
                     &(ina_dev[i].currentLSB),
                     &(ina_dev[i].powerLSB));
    usleep(10);
  }

  FRAM_init(ADCS_FRAM_DEV_ID);

  int_wdg_dev.clr = 0;
  int_wdg_dev.cmd = 0;

}

void update_device(dev_id id) {

  OSAL_device_pend();

  if(id == ADCS_1_MON_DEV_ID ||
     id == ADCS_2_MON_DEV_ID ||
     id == ADCS_3_MON_DEV_ID ||
     id == ADCS_4_MON_DEV_ID) {

    uint8_t pos_index = id - ADCS_1_MON_DEV_ID;

    ina_dev[pos_index].voltage_raw = 0;
    ina_dev[pos_index].current_raw = 0;

    bool r1 = INA226_readShuntCurrent_raw(id, &ina_dev[pos_index].current_raw );
    OSAL_sys_delay(1);

    bool r2 = INA226_readBusVoltage_raw(id, &ina_dev[pos_index].voltage_raw);
    OSAL_sys_delay(1);

    INA226_readBusPower_raw(id, &ina_dev[pos_index].power_raw);

    ina_status[pos_index] = r1 & r2;

  } else if(id == ADCS_TEMP_DEV_ID) {

    uint8_t pos_index = id - ADCS_TEMP_DEV_ID;

    tmp_dev[pos_index].raw_temp = 0;

    tmp_status[pos_index] = tmp_getTemperature_raw(id, &tmp_dev[pos_index].raw_temp);
    tmp_getRawTemperature(id, &tmp_dev[pos_index].raw_temp, &tmp_dev[pos_index].temp);

  }

  OSAL_device_post();

}

void read_device_status(bool *status, uint16_t *size) {

  status[0] = tmp_status[0];
  status[1] = ina_status[0];
  status[2] = ina_status[1];
  status[3] = ina_status[2];
  status[4] = ina_status[3];
  status[5] = int_temp_status[0];

  *size = 6;
}

void read_device_parameters(dev_id id, void * data) {

  OSAL_device_pend();

  if(id == ADCS_1_MON_DEV_ID ||
     id == ADCS_2_MON_DEV_ID ||
     id == ADCS_3_MON_DEV_ID ||
     id == ADCS_4_MON_DEV_ID) {

      uint8_t pos_index = id - ADCS_1_MON_DEV_ID;

    ((struct ina_device*)data)->power = ina_dev[pos_index].power;
    ((struct ina_device*)data)->current = ina_dev[pos_index].current;
    ((struct ina_device*)data)->voltage = ina_dev[pos_index].voltage;

    ((struct ina_device*)data)->power_raw = ina_dev[pos_index].power_raw;
    ((struct ina_device*)data)->current_raw = ina_dev[pos_index].current_raw;
    ((struct ina_device*)data)->voltage_raw = ina_dev[pos_index].voltage_raw;


  } else if(id == ADCS_TEMP_DEV_ID) {

      uint8_t pos_index = id - ADCS_TEMP_DEV_ID;

     ((struct tmp_device*)data)->raw_temp = tmp_dev[pos_index].raw_temp;
     ((struct tmp_device*)data)->temp = tmp_dev[pos_index].temp;

  }  else if(id == ADCS_FRAM_DEV_ID) {

     FRAM_read(id,
               ((struct fram_device*)data)->address,
               ((struct fram_device*)data)->buffer,
               ((struct fram_device*)data)->count);

   }  else if(id == INT_WDG_DEV_ID) {

     ((struct int_wdg_device*)data)->clr = int_wdg_dev.clr;
     ((struct int_wdg_device*)data)->cmd = int_wdg_dev.cmd;

  }

  OSAL_device_post();

}

void write_device_parameters(dev_id id, void * data) {

  if(id == ADCS_FRAM_DEV_ID) {

     FRAM_write(id,
                ((struct fram_device*)data)->address,
                ((struct fram_device*)data)->buffer,
                ((struct fram_device*)data)->count);

  }  else if(id == INT_WDG_DEV_ID) {

    int_wdg_dev.clr = ((struct int_wdg_device*)data)->clr;
    int_wdg_dev.cmd = ((struct int_wdg_device*)data)->cmd;

    if(int_wdg_dev.clr && !int_wdg_dev.cmd) {
      HAL_clear_int_wdg();
      int_wdg_dev.clr = false;
    }

  }

}

bool set_device_parameters(dev_id id, void * data) {

  bool res = false;

  return res;
}
