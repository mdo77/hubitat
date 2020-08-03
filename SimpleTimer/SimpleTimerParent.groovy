/*
 *    Hubitat Import URL: https://raw.githubusercontent.com/mdo77/hubitat/master/SimpleTimer/SimpleTimerParent.groovy
 *
 *
 */

definition(
    name: "Simple Timer",
    namespace: "mdo",
    singleInstance: true,
    author: "Mike O'Brian",
    description: "",
    category: "HVAC",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/mdo77/hubitat/master/SimpleTimer/SimpleTimerParent.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if(!state.trsInstalled) {
            section("Hit Done to install Simple Timer App!") {
            }
        }
        else {
            def childApps = getAllChildApps()
            def childVer = "Initial Setup - Version Unknown"
            section("Create a new simple timer.") {
                app(name: "childApps", appName: "Simple Timer (Child)", namespace: "mdo", title: "New Simple Timer", multiple: true)
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


