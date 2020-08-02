/*
 *    Hubitat Import URL: https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatRemoteSensorChild.groovy
 *
 *
 */

import hubitat.helper.RMUtils

definition(
    name: "Thermostat Remote Sensor (Child)",
    namespace: "mdo",
    author: "Mike O'Brian",
    description: "Use remote temperature sensor(s) to control your thermostat",
    category: "HVAC",
    parent: "mdo:Thermostat Remote Sensor",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatRemoteSensorChild.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
        section {
            //input "thisName", "text", title: "Name this app", submitOnChange: true
            input "selectedThermostat", "capability.thermostat", title: "Select Thermostat", submitOnChange: true, required: true, multiple: false
            input "remoteSensors", "capability.temperatureMeasurement", title: "Select Remote Sensor(s)", submitOnChange: true, required: true, multiple: true
            input "overrideSwitch", "capability.switch", title: "Activation Switch", submitOnChange: false, required: true, multiple: false
            input "debugLogging", "bool", title: "Use Debug Logging", submitOnChange: false, required: false
            input "doLog6x", "bool", title: "Extra Logging", submitOnChange: false, required: false
            app.updateLabel(defaultLabel())
        }
    }
}
def debugLog(msg)
{
    if (debugLogging)
        log.debug msg
}
def appIsActive() {
    return overrideSwitch.currentState("switch").value == "on"
}
def isRunning() {
    def opState = selectedThermostat.currentState("thermostatOperatingState").value.toLowerCase()
    return opState.indexOf("heat") >= 0 || opState.indexOf("cool") >= 0
}

def defaultLabel() {
    def label = "Thermostat Remote Sensor (Child)"
    if (selectedThermostat && remoteSensors) {
        label ="${selectedThermostat.label} => "
        def b = false
        def sensorList = ""
        remoteSensors.each {
            if (b) sensorList += ", "
            b = true
            sensorList += it.label
        }
        if (sensorList.indexOf(",") > 0) sensorList = "($sensorList)"
        label += sensorList
    }
    return label
}

def installed() {
    state.trscInstalled = true
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}
def monitorSetpoints(enable)
{
    if (!enable)
    {
        debugLog("Disabling setpoint monitoring")
        unsubscribe(selectedThermostat, "coolingSetpoint", handleEvent)
        unsubscribe(selectedThermostat, "heatingSetpoint", handleEvent)
    }
    else 
    {
        debugLog("Enabling setpoint monitoring")
        subscribe(selectedThermostat, "coolingSetpoint", handleEvent)
        subscribe(selectedThermostat, "heatingSetpoint", handleEvent)
    }
    pauseExecution(2000)
}
def initialize() {
    unschedule("work2x")
    unschedule("log6x")
    subscribe(overrideSwitch, "switch", handleEvent)
    monitorSetpoints(true)
    subscribe(selectedThermostat, "thermostatMode", handleEvent)
    subscribe(selectedThermostat, "temperature", handleEvent)
    subscribe(selectedThermostat, "thermostatOperatingState", handleEvent)
    subscribe(remoteSensors, "temperature", handleEvent)
    startLog6x()
}
def thermostatState() {
    return selectedThermostat.currentState("thermostatOperatingState").value
}
def thermostatMode() {
    return selectedThermostat.currentState("thermostatMode").value
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
def thermostatTemperature() {
    return Float.parseFloat(selectedThermostat.currentState("temperature").value)
}
def referenceSensorTemp() {
    // if heat then get min, if cool then get max
    def temp = thermostatSetpoint()
    def mode = thermostatMode().toLowerCase()
    remoteSensors.each {
        def sensorTemp = Float.parseFloat(it.currentState("temperature").value)
        if (mode.indexOf("heat") >= 0)
        {
            if (temp > sensorTemp) temp = sensorTemp
        }
        else if (mode.indexOf("cool") >= 0)
        {
            if (temp < sensorTemp) temp = sensorTemp
        }
    }
    return temp
}

def debugLogState() {
    def s = "${app.label}\n"
    s += "Thermostat: ${selectedThermostat.label} (${thermostatTemperature()}°) set to ${thermostatSetpoint()}° and ${thermostatState()}\n"
    remoteSensors.each {
        s += "Remote Sensor: ${it.label} is ${it.currentState("temperature").value}°\n"
    }
    s += "Switch: ${overrideSwitch.label} is ${overrideSwitch.currentState("switch").value}\n"
    s += "Saved Setpoint: ${state.savedSetpoint} / Reference Sensor Temp: ${referenceSensorTemp()}"
    debugLog(s)
}
def startLog6x()
{
    if (doLog6x)
    {
        runEvery1Minute("log6x")
    }
}
def log6x()
{
    debugLogState()
    pauseExecution(10000)
    debugLogState()
    pauseExecution(10000)
    debugLogState()
    pauseExecution(10000)
    debugLogState()
    pauseExecution(10000)
    debugLogState()
    pauseExecution(10000)
    debugLogState()
}
def workWhile()
{
    // record temp
    state.savedSetpoint = thermostatSetpoint()
    debugLog("Saved setpoint @ ${state.savedSetpoint}°")
    runEvery1Minute("work2x")
    work2x()
}
def work2x()
{
    work()
    pauseExecution(30000)
    work()
}
def workCheckCancel() {
    def b = appIsActive()
    if (!b)
    {
        unschedule("work2x");
        debugLog("Work is cancelled.")
        updateSetpoint(0)
    }
    return b
}
def updateSetpoint(diff)
{
    monitorSetpoints(false)
    def newValue = state.savedSetpoint + diff
    debugLog("updateSetpoint to ${newValue}")
    def mode = thermostatMode().toLowerCase()
    if (mode == "heat")
        selectedThermostat.setHeatingSetpoint(newValue)
    else if (mode == "cool")
        selectedThermostat.setCoolingSetpoint(newValue)
    pauseExecution(2000)
    monitorSetpoints(true)
}
def work()
{
    if (workCheckCancel())
    {
        def mode = thermostatMode().toLowerCase()
        def refTemp = referenceSensorTemp()
        def currentTemp = thermostatTemperature()
        def setpoint = thermostatSetpoint()
        def opState = thermostatState().toLowerCase()
        if (mode.indexOf("heat") >= 0)
        {
            if (currentTemp < setpoint)
            {
                updateSetpoint(0)
            }
            else if (refTemp < state.savedSetpoint)
            {
                if (opState == "idle")
                {
                    updateSetpoint(5) // add 5 to saved setpoint
                }
            } else 
            {
                updateSetpoint(0) // return to saved setpoint
            }
        } 
        else if (mode.indexOf("cool") >= 0)
        {
            if (currentTemp > setpoint)
            {
                updateSetpoint(0)
            }
            if (refTemp > state.savedSetpoint)
            {
                if (opState == "idle")
                {
                    updateSetpoint(-5)
                }
            } else 
            {
                updateSetpoint(0)
            }
        }
        debugLogState()
    }
}
def handleEvent(evt) {
    if (appIsActive() && evt.device.name.toLowerCase().indexOf("thermostat") >= 0) {
        switch(evt.name.toLowerCase()) {
            case "coolingsetpoint":
            case "heatingsetpoint":
                debugLog("Setpoint updated via Thermostat ${evt.getIntegerValue()}")
                state.setpoint = evt.getIntegerValue()
                break;
        }
    } 
    // START/STOP
    else if (evt.device.name.toLowerCase().indexOf("switch") >= 0) {
        debugLog("${app.label} is active? ${appIsActive()}")
        if (appIsActive()) workWhile()
    }
    else {
        debugLog("Name: ${evt.device.displayName} (${evt.device.name} ${evt.device.type}); Attribute: ${evt.name}; value: ${evt.value}")
    }
}
