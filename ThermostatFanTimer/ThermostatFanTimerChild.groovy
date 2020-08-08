/*
 *    Hubitat Import URL: https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatFanTimer/ThermostatFanTimerChild.groovy
 *
 *
 */

import hubitat.helper.RMUtils

definition(
    name: "Thermostat Fan Timer (Child)",
    namespace: "mdo",
    author: "Mike O'Brian",
    description: "Temporarily turn on fan for a set number of minutes",
    category: "HVAC",
    parent: "mdo:Thermostat Fan Timer",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatFanTimer/ThermostatFanTimerChild.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
        section {
            //input "thisName", "text", title: "Name this app", submitOnChange: true
            input "selectedThermostat", "capability.thermostat", title: "Select Thermostat", submitOnChange: true, required: true, multiple: false
            input "numMinutes", "integer", title: "Minutes to keep change", submitOnChange: false, required: true
            input "triggerButton", "capability.pushableButton", title: "Activation Button", submitOnChange: true, required: true, multiple: false
            input "debugLogging", "bool", title: "Use Debug Logging", submitOnChange: false, required: false
            app.updateLabel(defaultLabel())
        }
    }
}
def debugLog(msg)
{
    if (debugLogging)
        log.debug msg
}

def defaultLabel() {
    def label = "Fan Timer (Child)"
    if (selectedThermostat && numMinutes && triggerButton) {
        label ="${selectedThermostat.label}: Timer for ${numMinutes} minutes"
    }
    return label
}

def installed() {
    state.isInstalled = true
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    debugLog("Reinitializing")
    unschedule("returnToAuto")
    state.isRunning = false
    subscribe(triggerButton, "pushed", start)
}

def start(evt) {
    if (!state.isRunning)
    {
        debugLog("${app.label} - started")
        state.isRunning = true
        selectedThermostat.setThermostatFanMode("on")
        runIn(Integer.parseInt(numMinutes, 16) * 60, returnToAuto)
    } else {
        debugLog("${app.label} is already running, ignoring button press")
    }
}

def returnToAuto()
{
    if (state.isRunning) {
        debugLog("${app.label} completing.")
        selectedThermostat.setThermostatFanMode("auto")
        state.isRunning = false
    }

}
