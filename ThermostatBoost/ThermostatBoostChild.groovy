definition(
    name: 'Thermostat Boost (Child)',
    namespace: 'mdo',
    author: "Mike O'Brian",
    description: 'Temporarily adjust temp +/- for a set number of minutes, returning to previous setting after',
    category: 'HVAC',
    parent: 'mdo:Thermostat Boost',
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: '',
    importUrl: 'https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatBoost/ThermostatBoostChild.groovy'
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
            input 'numDegrees', 'integer', title: 'Degrees to adjust (up if heat, down if cool)', submitOnChange: true, required: true
            input 'numMinutes', 'integer', title: 'Minutes to keep change', submitOnChange: true, required: true
            input 'triggerSwitch', 'capability.switch', title: 'Activation Switch', submitOnChange: true, required: true, multiple: false
            input 'debugLogging', 'bool', title: 'Use Debug Logging', submitOnChange: false, required: false

            app.updateLabel(defaultLabel())
        }
    }
}
void debugLog(String msg) {
    if (debugLogging) {
        log.debug msg
    }
}

String defaultLabel() {
    String label = 'Boost (Child)'
    if (selectedThermostat && numDegrees && numMinutes && triggerSwitch) {
        label = "${selectedThermostat.label}: Boost ${numDegrees}° for ${numMinutes} minutes"
    }
    return label
}

void installed() {
    state.isInstalled = true
    initialize()
}

void updated() {
    unsubscribe()
    initialize()
}

void initialize() {
    debugLog('Reinitializing')
    unschedule('returnToSetting')
    triggerSwitch.off()
    subscribe(triggerSwitch, 'switch', switchEvent)
}
String thermostatState() {
    return selectedThermostat.currentState('thermostatOperatingState').value
}
String thermostatMode() {
    return selectedThermostat.currentState('thermostatMode').value.toLowerCase()
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

void updateSetpoint(int diff) {
    int newValue = state.savedSetpoint - diff
    debugLog("updateSetpoint to ${newValue}")
    String mode = thermostatMode().toLowerCase()
    if (mode == 'heat') {
        selectedThermostat.setHeatingSetpoint(newValue)
    }
    else if (mode == 'cool') {
        selectedThermostat.setCoolingSetpoint(newValue)
    }
    pauseExecution(2000)
}

/* groovylint-disable-next-line MethodParameterTypeRequired, NoDef, UnusedMethodParameter */
void switchEvent(evt) {
    if (evt.value == 'on') {
        debugLog("${app.label} - started")
        state.savedSetpoint = thermostatSetpoint()
        updateSetpoint(Integer.parseInt(numDegrees, 16))
        runIn(Integer.parseInt(numMinutes, 16) * 60, switchOff)
    } else {
        switchOff()
    }
}

void switchOff() {
    triggerSwitch.off()
    debugLog("${app.label} completing.")
    updateSetpoint(0)
}
