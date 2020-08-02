/*
 *    Hubitat Import URL: https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatBoost/ThermostatBoostChild.groovy
 *
 *
 */

import hubitat.helper.RMUtils

definition(
    name: "Thermostat Boost (Child)",
    namespace: "mdo",
    author: "Mike O'Brian",
    description: "Temporarily adjust temp +/- for a set number of minutes, returning to previous setting after",
    category: "HVAC",
    parent: "mdo:Thermostat Boost",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatBoost/ThermostatBoostChild.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
        section {
            //input "thisName", "text", title: "Name this app", submitOnChange: true
            input "selectedThermostat", "capability.thermostat", title: "Select Thermostat", submitOnChange: true, required: true, multiple: false
            input "numDegrees", "integer", title: "Degrees to adjust (up if heat, down if cool)", submitOnChange: false, required: true
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
    def label = "Boost (Child)"
    if (selectedThermostat && numDegrees && numMinutes && triggerButton) {
        label ="${selectedThermostat.label}: Boost ${numDegrees}° for ${numMinutes} minutes"
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
    unschedule("returnToSetting")
    state.isRunning = false
    subscribe(triggerButton, "pushed", start)
}
def thermostatState() {
    return selectedThermostat.currentState("thermostatOperatingState").value
}
def thermostatMode() {
    return selectedThermostat.currentState("thermostatMode").value.toLowerCase()
}
def thermostatSetpoint() {
    def val = 0
    def mode = thermostatMode().toLowerCase()
    if (mode.indexOf("cool") >=0) {
        return selectedThermostat.currentState("coolingSetpoint").value.toInteger()
    } else if (mode.indexOf("heat") >= 0) {
        return selectedThermostat.currentState("heatingSetpoint").value.toInteger()
    } else return 0
}

def updateSetpoint(diff)
{
    def newValue = state.savedSetpoint - diff
    debugLog("updateSetpoint to ${newValue}")
    def mode = thermostatMode().toLowerCase()
    if (mode == "heat")
        selectedThermostat.setHeatingSetpoint(newValue)
    else if (mode == "cool")
        selectedThermostat.setCoolingSetpoint(newValue)
    pauseExecution(2000)
}

def start(evt) {
    if (!state.isRunning)
    {
        debugLog("${app.label} - started")
        state.isRunning = true
        state.savedSetpoint = thermostatSetpoint()
        updateSetpoint(Integer.parseInt(numDegrees, 16))
        runIn(Integer.parseInt(numMinutes, 16) * 60, returnToSetting)
    } else {
        debugLog("${app.label} is already running, ignoring button press")
    }
}

def returnToSetting()
{
    if (state.isRunning) {
        debugLog("${app.label} completing.")
        updateSetpoint(0)
        state.isRunning = false
    }

}
