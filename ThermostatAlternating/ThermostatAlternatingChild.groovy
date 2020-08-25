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
    debugLog("switchEvent ${evt.value}")
    if (evt.value == 'on') {
        subscribe(selectedThermostat, 'thermostatOperatingState', operatingStateEvent)
        String opState = selectedThermostat.currentState('thermostatOperatingState').value
        if (opState == 'heating' || opState == 'cooling') {
            state.operatingState = opState
            debugLog("Switching to fan in ${numMinutesOn} minutes")
            runIn(Integer.parseInt(numMinutesOn) * 60, switchToFan)
        }
    } else {
        debugLog('Alternating fan exiting')
        unschedule('switchToFan')
        unschedule('switchBack')
        switchBack(true)
    }
}

/* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
void operatingStateEvent(def evt) {
    debugLog("operatingStateEvent ${evt.value}")
    if (triggerSwitch.value == 'off') { return }
    if (evt.value == 'heating' || evt.value == 'cooling') {
        runIn(Integer.parseInt(numMinutesOn) * 60, switchToFan)
        state.operatingState = evt.value
    } else {
        unschedule('switchToFan')
    }
}

void switchToFan() {
    debugLog('Switching to fan...')
    selectedThermostat.fanOn()
    pauseExecution(2000)
    selectedThermostat.off()
    debugLog("Switching back to ${state.operatingState} in ${numMinutesOff}")
    runIn(Integer.parseInt(numMinutesOff) * 60, switchBack)
}

void switchBack(boolean exiting = false) {
    debugLog("Switching back to ${state.operatingState}")
    if (state.operatingState == 'heating') {
        selectedThermostat.heat()
    } else {
        selectedThermostat.cool()
    }
    selectedThermostat.fanAuto()
    if (!exiting) {
        debugLog("Switching to fan in ${numMinutesOn}")
        runIn(Integer.parseInt(numMinutesOn) * 60, switchToFan)
    }
}
