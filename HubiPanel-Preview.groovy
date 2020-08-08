import groovy.json.*;

definition(
    name: "HubiPanel",
    namespace: "CordMaster",
    author: "Alden Howard",
    description: "Switch dashboards with ease",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2X.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2X.png"
)

preferences {
    page(name: "mainPage")
    page(name: "enableOAuthPage")
    
    mappings {
        path("/main/") {
            action: [
                GET: "getMain"
            ]
        }
        
        path("/main/:/") {
            action: [
                GET: "getMain"
            ]
        }
        
        path("/sendCommand/") {
            action: [
                POST: "sendCommand"
            ]
        }
        
        path("/options/") {
            action: [
                GET: "getOptions",
                POST: "postOptions"
            ]
        }
        
        path("/getDevices/") {
            action: [
                GET: "getDevices"
            ]
        }
        
        path("/getDevice/:device/") {
            action: [
                GET: "getDevice"
            ]
        }
        
        path("/test/") {
            action: [
                GET: "test"
            ]
        }
    }
}

def mainPage() {
	dynamicPage(name: "mainPage", uninstall: true, install: true) {
		section() {
            if(state.endpoint) {
                input(type: "text", name: "cacheLocation", title: "JS Location", defaultValue: "https://cdn.plumpynuggets.com/static/js/", required: true, submitOnChange: true)
                input(type: "text", name: "openWeatherMapKey", title: "Open Weather Map api key (leave blank to disable weather)", required: false, submitOnChange: true)
                
                input(type: "capability.*", name: "devices", title: "Select all devices", multiple: true, required: true, submitOnChange: true)
                
                def accessStr = """${state.fullEndpoint}?access_token=${state.secret}&dashboardAppId=${dashboardAppId}&dashboardAccessToken=${dashboardToken}${openWeatherMapKey ? "&openWeatherToken=${openWeatherMapKey}" : ""}""";
                
                paragraph("""<a href="${accessStr}" target="_blank">${accessStr}</a>""")
            }
            else paragraph("Click done to enable OAuth and return to the app to get the link.");
		}
        section(){
            input( type: "text", name: "app_name", title: "<b>Rename the Application?</b>", default: "HubiPanel", submitOnChange: true ) 
        }
	}
}


def initOAuth() {
    state.secret = createAccessToken();
    state.endpoint = fullLocalApiServerUrl("");
    state.fullEndpoint = fullLocalApiServerUrl("main/");
}

def installed() {
	log.debug("Installed")
    initOAuth()
    app.updateLabel(app_name);
}

def updated() {
	log.debug("Updated")
    app.updateLabel(app_name);
}

def uninstalled() {
	log.debug("Uninstalled")
}

def getMain() {
    def resp = "";
    log.debug(cacheLocation);
    httpGet([ "uri": cacheLocation, "path": "index.html", contentType: "text/plain" ]) { it ->
        resp = it.data.text
    }
    
    
    
    resp = resp.replaceAll("/static/js/", "${cacheLocation}").replaceAll("/icon512.png", "${cacheLocation}/icon512.png");
    
    return render(contentType: "text/html", data: resp);
}

def sendCommand() {
    def body = request.body;
    def parsedBody = parseJson(body);
    
    log.debug(parsedBody);
    
    def device = devices.find{ it -> it.idAsLong == parsedBody.deviceId };
        
        log.debug(device);
        
    device."${parsedBody.command}"();
            
    return render(contentType: "application/json", data: "{}", headers: ["Access-Control-Allow-Origin": "*"]);
}

def getOptions() {
    return render(contentType: "application/json", data: state.options ? state.options : "{}", headers: ["Access-Control-Allow-Origin": "*"]); 
}

def postOptions() {
    state.options = request.body;
    return render(headers: ["Access-Control-Allow-Origin": "*"]);
}

def getDevices() {
    def parsedDevices = devices.collect { device ->
        return [ id: device.idAsLong, label: device.label ? device.label : device.name ];
    }
    
    return render(contentType: "application/json", data: JsonOutput.toJson(parsedDevices), headers: ["Access-Control-Allow-Origin": "*"]); 
}

def getDevice() {
    def deviceId = params.device;
    def device = devices.find{ it -> it.id == deviceId };
    
    def parsedAttributes = device.supportedAttributes.collect { attribute ->
        def state = device.currentState(attribute.name);
        return state ? [ name: attribute.name, type: attribute.dataType, values: attribute.values, currentState: state.value, unit: state.unit ] : null;
    }.findAll { it -> it != null }
    
    return render(contentType: "application/json", data: JsonOutput.toJson(parsedAttributes), headers: ["Access-Control-Allow-Origin": "*"]); 
}

def test() {
    
}
