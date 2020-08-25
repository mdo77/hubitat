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
            input 'overrideSwitch', 'capability.switch', title: 'Activation Switch', submitOnChange: false, required: true, multiple: false
            input 'offset', 'capability.sensor', title: 'Offset Variable', submitOnChange: false, required: true, multiple: false
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

void initialize() {
    // unschedule('work')
    // unschedule('debugLogState')
    subscribe(overrideSwitch, 'switch', handleEvent)
    subscribe(remoteSensors, 'temperature', handleEvent)
    //startLog6x()
    debugLogState()
}

int thermostatTemperature() {
    int temp = selectedThermostat.currentState('temperature').getNumberValue()
    return temp
}
int calibration() {
    return selectedThermostat.currentState('currentSensorCal').getNumberValue()
}

Float referenceSensorTemp() {
    // if heat then get min, if cool then get max
    Float temp = 0
    int count = 0
    remoteSensors.each {
        count = count + 1
        /* groovylint-disable-next-line ImplicitClosureParameter */
        temp += it.currentState('temperature').getFloatValue()
    }
    Float x = temp / count
    debugLog("referenceSensorTemp: ${x} -> ${Math.round(x)}")
    return x
}

void debugLogState() {
    String s = "${app.label}\n"
    s += "Thermostat: ${selectedThermostat.label} (${thermostatTemperature()}°) +/- ${calibration()}\n"
    remoteSensors.each {
        /* groovylint-disable-next-line ImplicitClosureParameter */
        s += "Remote Sensor: ${it.label} is ${it.currentState('temperature').value}°\n"
    }
    s += "Switch: ${overrideSwitch.label} is ${overrideSwitch.currentState('switch').value}\n"
    s += "Reference Sensor Temp: ${referenceSensorTemp()}"
    debugLog(s)
}

void startLog6x() {
    if (doLog6x) {
        schedule('0/10 * * * * ? *', debugLogState)
    }
}

void refreshAux() {
    debugLog('Refreshing Sensors...')
    remoteSensors.each {
        /* groovylint-disable-next-line ImplicitClosureParameter */
        //it.refresh()
    }
}

void calculateAndSetSensorCalibration() {
    int temp = thermostatTemperature() - calibration()
    int refTemp = Math.round(referenceSensorTemp())
    int neededOffset = refTemp - temp
    if (neededOffset > 7) { neededOffset = 7 }
    if (neededOffset < -7) { neededOffset = -7 }
    debugLogState()
    debugLog("${refTemp} - ${temp} => Updating SensorCal to ${neededOffset}...")
    selectedThermostat.SensorCal(neededOffset)
    offset.setVariable(neededOffset)
}
/* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
void handleEvent(def evt) {
    debugLog("handleEvent ${evt.name} ${evt.value}")
    // groovy.json.JsonOutput jo = new groovy.json.JsonOutput()
    // String eventText = jo.toJson(evt)
    // debugLog(eventText)
    if (evt.name == 'temperature') {
        calculateAndSetSensorCalibration()
    }
    // START/STOP
    else if (evt.name == 'switch') {
        debugLog("${app.label} is active? ${appIsActive()}")
        if (evt.value == 'on') {
            debugLog('Starting...')
            startLog6x()
            calculateAndSetSensorCalibration()
            //schedule('0 2 * * * ? *', refreshAux) // refresh sensors every 2 min
        } else {
            // stop any scheduled tasks
            debugLog('Stopping...')
            //unschedule('refreshAux')
            unschedule('debugLogState')
            selectedThermostat.SensorCal(0)
            offset.setVariable(0)
        }
    }
}
