definition(
    name: 'Thermostat Fan Timer',
    namespace: 'mdo',
    singleInstance: true,
    author: "Mike O'Brian",
    description: '',
    category: 'HVAC',
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: '',
    importUrl: 'https://raw.githubusercontent.com/mdo77/hubitat/master/ThermostatFanTimerParent.groovy'
)

preferences {
    page(name: 'mainPage')
}

/* groovylint-disable-next-line MethodReturnTypeRequired, NoDef */
def mainPage() {
    return dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        /* groovylint-disable-next-line InvertedIfElse */
        if (!state.trsInstalled) {
            section('Hit Done to install fan timer App!') {
            }
        }
        else {
            /* groovylint-disable-next-line NoDef, UnusedVariable, VariableTypeRequired */
            def childApps = getAllChildApps()
            section('Create a new remote timer app.') {
                app(name: 'childApps', appName: 'Thermostat Fan Timer (Child)', namespace: 'mdo', title: 'New Thermostat Fan Timer', multiple: true)
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
