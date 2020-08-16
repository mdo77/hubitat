definition(
    name: 'Thermostat Alternating With Fan (Child)',
    namespace: 'mdo',
    author: "Mike O'Brian",
    description: 'Temporarily turn on fan for a set number of minutes to give heat/cool time to circulate',
    category: 'HVAC',
    parent: 'mdo:Thermostat Alternating With Fan',
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: '',
    importUrl: 'https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatAlternating/ThermostatAlternatingChild.groovy'
)

preferences {
    page(name: 'mainPage')
}

/* groovylint-disable-next-line MethodReturnTypeRequired, NoDef */
def mainPage() {
    dynamicPage(name: 'mainPage', title: ' ', install: true, uninstall: true) {
        section {
            input 'selectedThermostat', 'capability.thermostat', title: 'Select Thermostat', submitOnChange: true, required: true, multiple: false
            input 'numMinutesOn', 'integer', title: 'Minutes heat/cool is running', submitOnChange: true, required: true
            input 'numMinutesOff', 'integer', title: 'Minutes to go with fan only', submitOnChange: true, required: true
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
    String label = 'Thermostat Alternating With Fan (Child)'
    if (selectedThermostat && numMinutesOn && numMinutesOff && triggerSwitch) {
        label = "${selectedThermostat.label} on for ${numMinutesOn} and then fan only for  ${numMinutesOff} minutes"
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
    unschedule('switchOff')
    state.isRunning = false
    subscribe(triggerSwitch, 'switch', switchEvent)
}

/* groovylint-disable-next-line MethodParameterTypeRequired, NoDef, UnusedMethodParameter */
void switchEvent(def evt) {
    if (evt.value == 'on') {
        debugLog("${app.label} - started")
        selectedThermostat.setThermostatFanMode('on')
        runIn(Integer.parseInt(numMinutes, 16) * 60, switchOff)
    } else {
        switchOff()
    }
}

void switchOff() {
    triggerSwitch.off()
    debugLog("${app.label} completing.")
    selectedThermostat.setThermostatFanMode('auto')
    unschedule('switchOff')
}
