
var myApp = angular.module('progressApp', [
           'mm.foundation'
    ])
    .controller('progressCtrl', function ($rootScope) {

        $rootScope.isReadonly = false;

        $rootScope.dynamicObject = {
            value: 0,
            text : "",
            type: ''
        };

        $rootScope.loadData = function() {
            $rootScope.isReadonly = true;

            //using play framework we get the absolute URL
            var wsUrl = jsRoutes.controllers.api.ImportData.progress().absoluteURL();
            //replace the protocol to http ws
            wsUrl = wsUrl.replace("http", "ws");

            // Create our websocket object with the address to the websocket
            var ws = new WebSocket(wsUrl);

            ws.onopen = function () {
                console.log("Socket has been opened!");
                var msg = { url: angular.element(document.querySelector('#url')).val() };
                ws.send(JSON.stringify(msg))
                console.log("Sent data to websocket: ", msg);
            };

            ws.onmessage = function (message) {
                listener(JSON.parse(message.data));
            };

            function listener(data) {
                var messageObj = data;
                console.log("Received data from websocket: ", messageObj);
                //update the progress bar
                $rootScope.dynamicObject.type = messageObj.success ? '' : 'alert';
                $rootScope.dynamicObject.text = messageObj.text;
                $rootScope.isReadonly = !messageObj.done;
                $rootScope.$apply()
            }
        }

    })



