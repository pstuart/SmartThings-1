/**
 *  Copyright 2015 Nathan Jacobson <natecj@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

definition(
  name: "Smart Thermostat",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Control multiple Thermostat's heating and cooling settings between active and inactive depending on what mode is set.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  page(name: "rootPage")
  page(name: "thermostatPage")
}

def rootPage() {
  dynamicPage(name: "rootPage", title: "", install: true, uninstall: true) {
    section("How many Thermostat Schedules?") {
      input name: "scheduleCount", title: "Number of Schedules", type: "number", multiple: false, required: true, submitOnChange: true
    }
    if (scheduleCount > 0) {
      section('Thermostat Schedules') {
        (1..scheduleCount).each { index->
          href([
            name: "toThermostatPage${index}",
            page: "thermostatPage",
            params: [number: index],
            required: false,
            description: thermostatHrefDescription(index),
            title: thermostatHrefTitle(index),
            state: thermostatPageState(index)
          ])
        }
      }
      section {
        label title: "Assign a name", required: false
        mode title: "Set for specific mode(s)", required: false
      }
    }
  }
}

def thermostatPage(params) {
  dynamicPage(name:"thermostatPage") {
    def i = getThermostat(params);
    def tdevice = settings."thermostatDevice${i}"
    log.debug "Thermostat Device #${i}: ${tdevice}"
    section("Thermostat Schedule #${i}") {
      input(name: "thermostatDevice${i}", type: "capability.thermostat", title: "Thermostat", required: false, defaultValue: settings."thermostatDevice${i}", refreshAfterSelection: true)
      input(name: "thermostatHeatTemp${i}", type: "number", title: "Heat Point", required: false, defaultValue: settings."thermostatHeatTemp${i}", refreshAfterSelection: true)
      input(name: "thermostatCoolTemp${i}", type: "number", title: "Cool Point", required: false, defaultValue: settings."thermostatCoolTemp${i}", refreshAfterSelection: true)
      input(name: "thermostatModes${i}", type: "mode", title: "Modes", multiple: true, required: false, defaultValue: settings."thermostatModes${i}", refreshAfterSelection: true)
    }
  }
}


def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  initialize()
}

def initialize() {
  log.debug "initialize()"
}





def thermostatHrefTitle(index) {
  log.debug("thermostatHrefTitle(${index}): " + settings."thermostatDevice${index}".class)
  if (settings."thermostatDevice${index}".class == "string") return index;
  if (settings."thermostatDevice${index}") {
    deviceLabel(settings."thermostatDevice${index}")
  } else {
    "Thermostat ${index}"
  }
}

def thermostatHrefDescription(index) {
  //if (isThermostatConfigured(index)) {
    def heatTemp = settings."thermostatHeatTemp${index}"
    def coolTemp = settings."thermostatCoolTemp${index}"
    def currentTemp = "XX"
    def modes = settings."thermostatModes${index}"
    "${modes} ${heatTemp} < ${currentTemp} < ${coolTemp} (${isThermostatConfigured(index) ? 'configured' : 'unconfigured'})"
  //} else {
  //  "Unconfigured"
  //}
}

def thermostatPageState(index) {
  (isThermostatConfigured(index) ? 'complete' : 'incomplete')
}

def isThermostatConfigured(index) {
  def deviceExists = (settings."thermostatDevice${index}" != null)
  def modesExists = ((settings."thermostatModes${index}"?.size() ?: 0) > 0)
  (deviceExists && modesExists)
}

def deviceLabel(device) {
  device.label ?: device.name
}

def getThermostat(params) {
  def i = 1
  // Assign params to i.  Sometimes parameters are double nested.
  if (params.number) {
    i = params.number
  } else if (params.params){
    i = params.params.number
  } else if (state.lastThermostat) {
    i = state.lastThermostat
  }

  //Make sure i is a round number, not a float.
  if ( ! i.isNumber() ) {
    i = i.toInteger();
  } else if ( i.isNumber() ) {
    i = Math.round(i * 100) / 100
  }
  state.lastThermostat = i
  return i
}
