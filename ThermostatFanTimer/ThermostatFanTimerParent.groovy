/*
 *    Hubitat Import URL: https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatFanTimer/ThermostatFanTimerParent.groovy
 *
 *
 */

definition(
    name: "Thermostat Fan Timer",
    namespace: "mdo",
    singleInstance: true,
    author: "Mike O'Brian",
    description: "",
    category: "HVAC",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatFanTimerParent.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if(!state.trsInstalled) {
            section("Hit Done to install Boost App!") {
            }
        }
        else {
            def childApps = getAllChildApps()
            def childVer = "Initial Setup - Version Unknown"
            section("Create a new remote sensor mapping.") {
                app(name: "childApps", appName: "Thermostat Fan Timer (Child)", namespace: "mdo", title: "New Thermostat Fan Timer", multiple: true)
            }
        }
    }
}

def installed() {
    state.trsInstalled = true
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
}


