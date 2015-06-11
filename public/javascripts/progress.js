
var myApp = angular.module('progressApp', [
           'mm.foundation'
    ])
    .controller('progressCtrl', ['$scope', function ($scope) {

        $scope.isReadonly = false;

        $scope.dynamicObject = {
            value: 0,
            text : "",
            type: ''
        };

        $scope.overwriteData = true;
        $scope.dataSourceUrl = '';

        $scope.loadData = function() {
            $scope.isReadonly = true;

            //using play framework we get the absolute URL
            var wsUrl = jsRoutes.controllers.api.ImportData.progress().absoluteURL();
            //replace the protocol to http ws
            wsUrl = wsUrl.replace("http", "ws");

            // Create our websocket object with the address to the websocket
            var ws = new WebSocket(wsUrl);

            ws.onopen = function () {
                console.log("Socket has been opened!");
                var msg = { url: $scope.dataSourceUrl,
                    overwriteData: $scope.overwriteData};
                ws.send(JSON.stringify(msg));
                console.log("Sent data to websocket: ", msg);
            };

            ws.onmessage = function (message) {
                listener(JSON.parse(message.data));
            };

            function listener(data) {
                var messageObj = data;
                console.log("Received data from websocket: ", messageObj);
                //update the progress bar
                $scope.dynamicObject.type = messageObj.success ? '' : 'alert';
                $scope.dynamicObject.text = messageObj.text;
                $scope.isReadonly = !messageObj.done;
                $scope.$apply()
            }
        }

    }]);



