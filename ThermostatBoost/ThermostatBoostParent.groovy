/*
 *    Hubitat Import URL: https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatBoost/ThermostatBoostParent.groovy
 *
 *
 */

definition(
    name: "Thermostat Boost",
    namespace: "mdo",
    singleInstance: true,
    author: "Mike O'Brian",
    description: "",
    category: "HVAC",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatBoostParent.groovy"
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
            section("Create a new thermostat boost.") {
                app(name: "childApps", appName: "Thermostat Boost (Child)", namespace: "mdo", title: "New Thermostat Boost", multiple: true)
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


