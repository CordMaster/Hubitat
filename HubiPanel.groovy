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
        
        path("/getDashboards/") {
            action: [
                GET: "getDashboards"
            ]
        }
        
        path("/getDashboardLayout/:dashboardId/") {
            action: [
                GET: "getDashboardLayout"
            ]
        }
        
        path("/getDevices/:dashboardId/") {
            action: [
                GET: "getDevices"
            ]
        }
        
        path("/sendCommand/:dashboardId/") {
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
    }
}

def mainPage() {
	dynamicPage(name: "mainPage", uninstall: true, install: true) {
		section() {
            if(state.endpoint) {
                input(type: "text", name: "dashboardToken", title: "Base Dashboard App Access Token - Can be obtained from any dashboard, try activating fullscreen and checking the url bar\nEx: (http://255.255.255.255/apps/api/1/menu?access_token=<span style='font-weight: bold;'>xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</span>))", required: true, submitOnChange: true)
                input(type: "number", name: "dashboardAppId", title: "Base Dashboard App Id - Can be obtained from any dashboard, try activating fullscreen and checking the url bar\nEx: (http://255.255.255.255/apps/api/<span style='font-weight: bold;'>1</span>/menu?access_token=xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx))", required: true, submitOnChange: true)
                input(type: "text", name: "openWeatherMapKey", title: "Open Weather Map api key (leave blank to disable weather)", required: false, submitOnChange: true)
                
                def accessStr = """${state.fullEndpoint}?access_token=${state.secret}&dashboardAppId=${dashboardAppId}&dashboardAccessToken=${dashboardToken}${openWeatherMapKey ? "&openWeatherToken=${openWeatherMapKey}" : ""}""".replace('http://', 'https://')
                
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

def getHubIP() {
    return "http://${location.hubs[0].getDataValue("localIP")}/";
}

def getMain() {
    def server = "https://cdn.plumpynuggets.com";
    
    def resp = "";
    httpGet([ "uri": server, "path": "/index.html", contentType: "text/plain" ]) { it ->
        resp = it.data.text
    }
    
    resp = resp.replaceAll("/static/js/", "${server}/static/js/");
    
    return render(contentType: "text/html", data: resp);
}

def getDashboards() {
    def resp;
    httpGet("http://127.0.0.1:8080/apps/api/${dashboardAppId}/menu?access_token=${dashboardToken}") { it ->
        //                                          //head        //script
        def script = it.getData().getAt(0).children()[0].children()[13].text();
        
        def dashboards = "";
        
        script.eachLine {
            def dashboardsIndex = it.indexOf("var dashboardsJson = ");
            if(dashboardsIndex != -1) dashboards = it.substring(dashboardsIndex + "var dashboardsJson = ".length(), it.lastIndexOf("]") + 1);
        }
        
        resp = [
            "dashboards": new JsonSlurper().parseText(dashboards)
        ]
    }
    return render(contentType: "application/json", data: JsonOutput.toJson(resp), headers: ["Access-Control-Allow-Origin": "*"]);
}

def getDashboardLayout() {
    def dashboardId = params.dashboardId;
    
    def resp;
    
    httpGet([ "uri": "http://127.0.0.1:8080", "path": "/apps/api/${dashboardAppId}/dashboard/${dashboardId}/layout", params: [ "access_token": dashboardToken ], contentType: "text/plain" ]) { it ->
        resp = it.data.text;
    }
    
    return render(contentType: "application/json", data: resp, headers: ["Access-Control-Allow-Origin": "*"]);
}

def getDevices() {
    def dashboardId = params.dashboardId;
    
    def resp;
    
    httpGet([ "uri": "http://127.0.0.1:8080", "path": "/apps/api/${dashboardAppId}/dashboard/${dashboardId}/devices2", params: [ "access_token": dashboardToken ], contentType: "text/plain", timeout: 30 ]) { it ->
        resp = it.data.text;
    }
            
    return render(contentType: "application/json", data: resp, headers: ["Access-Control-Allow-Origin": "*"]);
}

def sendCommand() {
    def body = request.body;
    def parsedBody = parseJson(body);
    log.debug(parsedBody);
    def dashboardId = params.dashboardId;
    
    def resp;
    
    httpPost([ "uri": "http://127.0.0.1:8080", "path": "/apps/api/${dashboardAppId}/dashboard/${dashboardId}/command", params: [ "access_token": dashboardToken ], requestContentType: 'application/json', body: body, timeout: 30 ]) { it ->
        resp = it.data.text;
    }
            
    return render(contentType: "application/json", data: resp, headers: ["Access-Control-Allow-Origin": "*"]);
}

def getOptions() {
    return render(contentType: "application/json", data: (state.options ? state.options : "{error: true}"), headers: ["Access-Control-Allow-Origin": "*"]); 
}

def postOptions() {
    state.options = request.body;
    return render(headers: ["Access-Control-Allow-Origin": "*"]);
}