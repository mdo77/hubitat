/* groovylint-disable NoFloat */
definition(
    name: 'Thermostat Remote Sensor (Child)',
    namespace: 'mdo',
    author: "Mike O'Brian",
    description: 'Use remote temperature sensor(s) to control your thermostat',
    category: 'HVAC',
    parent: 'mdo:Thermostat Remote Sensor',
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: '',
    importUrl: 'https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatRemoteSensorChild.groovy'
)

preferences {
    page(name: 'mainPage')
}

/* groovylint-disable-next-line MethodReturnTypeRequired, NoDef */
def mainPage() {
    dynamicPage(name: 'mainPage', title: ' ', install: true, uninstall: true) {
        section {
            //input "thisName", "text", title: "Name this app", submitOnChange: true
            input 'selectedThermostat', 'capability.thermostat', title: 'Select Thermostat', submitOnChange: true, required: true, multiple: false
            input 'remoteSensors', 'capability.temperatureMeasurement', title: 'Select Temperature Sensor(s)', submitOnChange: true, required: true, multiple: true
            input 'setpointSensor', 'capability.temperatureMeasurement', title: 'Select Setpoint Sensor', submitOnChange: true, required: true, multiple: false
            input 'overrideSwitch', 'capability.switch', title: 'Activation Switch', submitOnChange: false, required: true, multiple: false
            input 'debugLogging', 'bool', title: 'Use Debug Logging', submitOnChange: false, required: false
            input 'doLog6x', 'bool', title: 'Extra Logging', submitOnChange: false, required: false
            app.updateLabel(defaultLabel())
        }
    }
}
void debugLog(String msg) {
    if (debugLogging) {
        log.debug msg
    }
}
boolean appIsActive() {
    return overrideSwitch.currentState('switch').value == 'on'
}

String defaultLabel() {
    String label = 'Thermostat Remote Sensor (Child)'
    if (selectedThermostat && remoteSensors) {
        label = "${selectedThermostat.label} => "
        boolean b = false
        String sensorList = ''
        remoteSensors.each {
            if (b) { sensorList += ', ' }
            b = true
            /* groovylint-disable-next-line ImplicitClosureParameter */
            sensorList += it.label
        }
        if (sensorList.indexOf(',') > 0) { sensorList = "($sensorList)" }
        label += sensorList
    }
    return label
}

void installed() {
    state.trscInstalled = true
    initialize()
}

void updated() {
    unsubscribe()
    initialize()
}
void setAutoUpdate(boolean enable) {
    state.autoUpdate = enable
}
void initialize() {
    unschedule('work')
    unschedule('debugLogState')
    subscribe(overrideSwitch, 'switch', handleEvent)
    subscribe(selectedThermostat, 'coolingSetpoint', handleEvent)
    subscribe(selectedThermostat, 'heatingSetpoint', handleEvent)
    //subscribe(selectedThermostat, 'thermostatMode', handleEvent)
    //subscribe(selectedThermostat, 'temperature', handleEvent)
    //subscribe(selectedThermostat, 'thermostatOperatingState', handleEvent)
    //subscribe(remoteSensors, 'temperature', handleEvent)
    setAutoUpdate(false)
    startLog6x()
    debugLogState()
}
String thermostatState() {
    return selectedThermostat.currentState('thermostatOperatingState').value
}
String thermostatMode() {
    return selectedThermostat.currentState('thermostatMode').value
}
Float thermostatSetpoint() {
    String mode = thermostatMode().toLowerCase()
    if (mode.indexOf('cool') >= 0) {
        return Float.parseFloat(selectedThermostat.currentState('coolingSetpoint').value)
    } else if (mode.indexOf('heat') >= 0) {
        return Float.parseFloat(selectedThermostat.currentState('heatingSetpoint').value)
    }
    return 0
}
Float thermostatTemperature() {
    return Float.parseFloat(selectedThermostat.currentState('temperature').value)
}

Float referenceSensorTemp() {
    // if heat then get min, if cool then get max
    int temp = savedSetpoint()
    String mode = thermostatMode().toLowerCase()
    remoteSensors.each {
        /* groovylint-disable-next-line ImplicitClosureParameter */
        Float sensorTemp = Float.parseFloat(it.currentState('temperature').value)
        if (mode.indexOf('heat') >= 0) {
            if (temp > sensorTemp) { temp = sensorTemp }
        } else if (mode.indexOf('cool') >= 0) {
            if (temp < sensorTemp) { temp = sensorTemp }
        }
    }
    return temp
}

void debugLogState() {
    String s = "${app.label}\n"
    s += "Thermostat: ${selectedThermostat.label} (${thermostatTemperature()}°) set to ${thermostatSetpoint()}° and ${thermostatState()}\n"
    remoteSensors.each {
        /* groovylint-disable-next-line ImplicitClosureParameter */
        s += "Remote Sensor: ${it.label} is ${it.currentState('temperature').value}°\n"
    }
    s += "Switch: ${overrideSwitch.label} is ${overrideSwitch.currentState('switch').value}\n"
    s += "Saved Setpoint: ${savedSetpoint()} / Reference Sensor Temp: ${referenceSensorTemp()}"
    debugLog(s)
}

void startLog6x() {
    if (doLog6x) {
        schedule('0/10 * * * * ? *', debugLogState)
    }
}

Float savedSetpoint(Float sp = null) {
    int retVal = 0
    if (sp == null) {
        retVal = Float.parseFloat(setpointSensor.currentState('temperature').value)
        debugLog("Checked setpoint @ ${retVal}°")
    } else {
        if (!state.autoUpdate) {
            retVal = sp
            setpointSensor.setTemperature(sp)
            debugLog("Saved setpoint @ ${sp}°")
        }
    }
    return retVal
}
void workWhile() {
    // record temp
    savedSetpoint(thermostatSetpoint())
    schedule('0/30 * * * * ? *', work)
    schedule('0 2 * * * ? *', refreshAux)
}
void refreshAux() {
    debugLog('Refreshing Sensors...')
    remoteSensors.each {
        /* groovylint-disable-next-line ImplicitClosureParameter */
        it.refresh()
    }
}

boolean workCheckCancel() {
    boolean b = appIsActive()
    if (!b) {
        unschedule('work')
        unschedule('refreshAux')
        unschedule('debugLogState')
        debugLog('Work is cancelled.')
        resetToSaved()
    }
    return b
}
void resetToSaved() {
    String mode = thermostatMode()
    if (mode.indexOf('heat') >= 0) {
        selectedThermostat.setHeatingSetpoint(savedSetpoint())
    } else if (mode.indexOf('cool') >= 0) {
        selectedThermostat.setCoolingSetpoint(savedSetpoint())
    }
}
void forceOnOff(boolean on) {
    if (workCheckCancel()) {
        setAutoUpdate(true)
        // force temp to turn on by setting thermostat to +/- 10 degrees of current thermostat temperature (based on cool/heat)
        Float currentTemp = thermostatTemperature()
        String mode = thermostatMode()
        Float newTemp = currentTemp
        if (mode.indexOf('heat') >= 0) {
            newTemp = on ? (currentTemp + 10) : (currentTemp - 10)
            selectedThermostat.setHeatingSetpoint(newTemp)
        } else if (mode.indexOf('cool') >= 0) {
            newTemp = on ? (currentTemp - 10) : (currentTemp + 10)
            selectedThermostat.setCoolingSetpoint(newTemp)
        }
        pauseExecution(5000)
        setAutoUpdate(false)
    }
}

void work() {
    if (workCheckCancel()) {
        String mode = thermostatMode().toLowerCase()
        Float refTemp = referenceSensorTemp()
        Float check = savedSetpoint()
        debugLogState()
        if (mode.indexOf('heat') >= 0) {
            debugLog("Heat? ${refTemp} < ${check}")
            forceOnOff(refTemp < check)
        } else if (mode.indexOf('cool') >= 0) {
            debugLog("Cool? ${refTemp} < ${check}")
            forceOnOff(refTemp > check)
        }
    }
}
/* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
void handleEvent(def evt) {
    if (appIsActive() && evt.device.name.toLowerCase().indexOf('thermostat') >= 0) {
        switch (evt.name.toLowerCase()) {
            case 'coolingsetpoint':
            case 'heatingsetpoint':
                debugLog("Setpoint updated via Thermostat ${evt.getIntegerValue()}")
                savedSetpoint(evt.getIntegerValue())
                break
        }
    }
    // START/STOP
    else if (evt.device.name.toLowerCase().indexOf('switch') >= 0) {
        debugLog("${app.label} is active? ${appIsActive()}")
        if (appIsActive()) {
            debugLog('Starting...')
            workWhile()
        } else {
            debugLog('Stopping...')
            workCheckCancel()
        }
    }
    else {
        debugLog("Name: ${evt.device.displayName} (${evt.device.name} ${evt.device.type}); Attribute: ${evt.name}; value: ${evt.value}")
    }
}
