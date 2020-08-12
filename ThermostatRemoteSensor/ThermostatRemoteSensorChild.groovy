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
boolean isRunning() {
    String opState = selectedThermostat.currentState('thermostatOperatingState').value.toLowerCase()
    return opState.indexOf('heat') >= 0 || opState.indexOf('cool') >= 0
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
void monitorSetpoints(boolean enable) {
    /* groovylint-disable-next-line InvertedIfElse */
    if (!enable) {
        debugLog('Disabling setpoint monitoring')
        unsubscribe(selectedThermostat, 'coolingSetpoint', handleEvent)
        unsubscribe(selectedThermostat, 'heatingSetpoint', handleEvent)
    } else {
        debugLog('Enabling setpoint monitoring')
        subscribe(selectedThermostat, 'coolingSetpoint', handleEvent)
        subscribe(selectedThermostat, 'heatingSetpoint', handleEvent)
    }
    pauseExecution(2000)
}
void initialize() {
    unschedule('work2x')
    unschedule('log6x')
    subscribe(overrideSwitch, 'switch', handleEvent)
    monitorSetpoints(true)
    subscribe(selectedThermostat, 'thermostatMode', handleEvent)
    subscribe(selectedThermostat, 'temperature', handleEvent)
    subscribe(selectedThermostat, 'thermostatOperatingState', handleEvent)
    subscribe(remoteSensors, 'temperature', handleEvent)
    startLog6x()
    debugLogState()
}
String thermostatState() {
    return selectedThermostat.currentState('thermostatOperatingState').value
}
String thermostatMode() {
    return selectedThermostat.currentState('thermostatMode').value
}
int thermostatSetpoint() {
    String mode = thermostatMode().toLowerCase()
    if (mode.indexOf('cool') >= 0) {
        return selectedThermostat.currentState('coolingSetpoint').value.toInteger()
    } else if (mode.indexOf('heat') >= 0) {
        return selectedThermostat.currentState('heatingSetpoint').value.toInteger()
    }
    return 0
}
/* groovylint-disable-next-line NoFloat */
Float thermostatTemperature() {
    return Float.parseFloat(selectedThermostat.currentState('temperature').value)
}
/* groovylint-disable-next-line NoFloat */
Float referenceSensorTemp() {
    // if heat then get min, if cool then get max
    int temp = savedSetpoint()
    String mode = thermostatMode().toLowerCase()
    remoteSensors.each {
        /* groovylint-disable-next-line ImplicitClosureParameter, NoFloat */
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
        runEvery1Minute('log6x')
    }
}

void log6x() {
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
int savedSetpoint(Integer sp = null) {
    int retVal = 0
    if (sp == null) {
        retVal = Integer.parseInt(setpointSensor.currentState('temperature').value)
    } else {
        retVal = sp
        setpointSensor.setTemperature(sp)
    }
    return retVal
}
void workWhile() {
    // record temp
    savedSetpoint(thermostatSetpoint())
    debugLog("Saved setpoint @ ${savedSetpoint()}°")
    runEvery1Minute('work2x')
    work2x()
}

void work2x() {
    work()
    pauseExecution(30000)
    work()
}

boolean workCheckCancel() {
    boolean b = appIsActive()
    if (!b) {
        unschedule('work2x')
        debugLog('Work is cancelled.')
        updateTempSetpoint(0)
    }
    return b
}
void updateTempSetpoint(int diff) {
    monitorSetpoints(false)
    int newValue = savedSetpoint() + diff
    debugLog("updateTempSetpoint to ${newValue}")
    String mode = thermostatMode().toLowerCase()
    if (mode == 'heat') {
        selectedThermostat.setHeatingSetpoint(newValue)
    } else if (mode == 'cool') {
        selectedThermostat.setCoolingSetpoint(newValue)
    }
    pauseExecution(2000)
    monitorSetpoints(true)
}

void work() {
    if (workCheckCancel()) {
        String mode = thermostatMode().toLowerCase()
        /* groovylint-disable-next-line NoFloat */
        Float refTemp = referenceSensorTemp()
        String opState = thermostatState().toLowerCase()
        if (mode.indexOf('heat') >= 0) {
            if (refTemp < savedSetpoint()) {
                if (opState == 'idle') {
                    updateTempSetpoint(5) // add 5 to saved setpoint
                }
            } else {
                updateTempSetpoint(-5) // return to saved setpoint
            }
        } else if (mode.indexOf('cool') >= 0) {
            if (refTemp > savedSetpoint()) {
                if (opState == 'idle') {
                    updateTempSetpoint(-5)
                }
            } else {
                updateTempSetpoint(5)
            }
        }
        debugLogState()
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
        if (appIsActive()) { workWhile() }
    }
    else {
        debugLog("Name: ${evt.device.displayName} (${evt.device.name} ${evt.device.type}); Attribute: ${evt.name}; value: ${evt.value}")
    }
}
