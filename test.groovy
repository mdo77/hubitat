definition(
    name: "Thermostat Remote Sensor",
    namespace: "hubitat",
    author: "Mike O'Brian",
    description: "Use remote sensor for thermostat",
    category: "HVAC",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: true
    )

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this app", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "thermostat", "capability.thermostat", title: "Select Thermostat", submitOnChange: true, required: true, multiple: false
			input "sensors", "capability.temperatureMeasurement", title: "Select Remote Sensor(s)", submitOnChange: true, required: true, multiple: true
            input "switch", "capability.switch", title: "Activation Switch", submitOnChange: true, required: true, multiple: false
		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
    //	def averageDev = getChildDevice("AverageHumid_${app.id}")
	//if(!averageDev) averageDev = addChildDevice("hubitat", "Virtual Humidity Sensor", "AverageHumid_${app.id}", null, [label: thisName, name: thisName])
	//averageDev.setHumidity(averageHumid())
	//subscribe(humidSensors, "humidity", handler)
}

def handler(evt) {
	// def averageDev = getChildDevice("AverageHumid_${app.id}")
	// def avg = averageHumid()
	// averageDev.setHumidity(avg)
	// log.info "Average humidity = $avg%"
}