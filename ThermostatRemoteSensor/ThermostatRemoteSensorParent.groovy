/*
 *    Hubitat Import URL: https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatRemoteSensorParent.groovy
 *
 *
 */

definition(
    name: "Thermostat Remote Sensor",
    namespace: "mdo",
    singleInstance: true,
    author: "Mike O'Brian",
    description: "",
    category: "HVAC",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatRemoteSensorParent.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if(!state.trsInstalled) {
            section("Hit Done to install TRS App!") {
            }
        }
        else {
            def childApps = getAllChildApps()
            def childVer = "Initial Setup - Version Unknown"
            section("Create a new remote sensor mapping.") {
                app(name: "childApps", appName: "Thermostat Remote Sensor (Child)", namespace: "mdo", title: "New Remote Thermostat Sensor", multiple: true)
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


