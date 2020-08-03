/*
 *    Hubitat Import URL: https://raw.githubusercontent.com/mdo77/hubitat/master/SimpleTimer/SimpleTimerChild.groovy
 *
 *
 */

import hubitat.helper.RMUtils

definition(
    name: "Simple Timer (Child)",
    namespace: "mdo",
    author: "Mike O'Brian",
    description: "Temporarily adjust temp +/- for a set number of minutes, returning to previous setting after",
    category: "HVAC",
    parent: "mdo:Simple Timer",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/mdo77/hubitat/master/SimpleTimer/SimpleTimerChild.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
        section {
            input "selectedSwitch", "capability.switch", title: "Selected Switch", submitOnChange: true, required: true, multiple: false
            input "numMinutes", "integer", title: "Minutes to keep on", submitOnChange: true, required: true
            input "debugLogging", "bool", title: "Use Debug Logging", submitOnChange: true, required: false
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
    def label = "Switch (Child)"
    if (selectedSwitch && numMinutes) {
        label ="${selectedSwitch.label}: Turn off after ${numMinutes} minutes"
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
    unschedule("turnOff")
    state.isRunning = false
    subscribe(selectedSwitch, "switch", start)
}

def start(evt) {
    def b = false
    if (evt.value == "on") b = true
    if (b && !state.isRunning)
    {
        debugLog("${app.label} - timer started.")
        state.isRunning = true
        runIn(Integer.parseInt(numMinutes, 16) * 60, turnOff)
    } else {
        unschedule("turnOff")
        turnOff()
    }
}

def turnOff()
{
    if (state.isRunning) {
        debugLog("${app.label} - timer completed.")
    }
    selectedSwitch.off()
    state.isRunning = false
}
