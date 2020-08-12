definition(
    name: 'Thermostat Remote Sensor',
    namespace: 'mdo',
    singleInstance: true,
    author: "Mike O'Brian",
    description: '',
    category: 'HVAC',
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: '',
    importUrl: 'https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatRemoteSensorParent.groovy'
)

preferences {
    page(name: 'mainPage')
}

/* groovylint-disable-next-line MethodReturnTypeRequired, NoDef */
def mainPage() {
    return dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        /* groovylint-disable-next-line InvertedIfElse */
        if (!state.trsInstalled) {
            section('Hit Done to install TRS App!') {
            }
        }
        else {
            /* groovylint-disable-next-line NoDef, UnusedVariable, VariableTypeRequired */
            def childApps = getAllChildApps()
            section('Create a new remote sensor mapping.') {
                app(name: 'childApps', appName: 'Thermostat Remote Sensor (Child)', namespace: 'mdo', title: 'New Remote Thermostat Sensor', multiple: true)
            }
        }
    }
}

void installed() {
    state.trsInstalled = true
    initialize()
}

void updated() {
    unsubscribe()
    initialize()
}

/* groovylint-disable-next-line EmptyMethod */
void initialize() {
}
