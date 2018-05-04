"use strict";

// --------
// WARNING:
// --------ODUCTION!

// FOR SECURITY REASONS, USING A JAVASCRIPT WEB APP HOSTED VIA THE CORDA NODE IS
// NOT THE RECOMMENDED WAY TO INTERFACE WITH CORDA NODES! HOWEVER, FOR THIS
// PRE-ALPHA RELEASE IT'S A USEFUL WAY TO EXPERIMENT WITH THE PLATFORM AS IT ALLOWS
// YOU TO QUICKLY BUILD A UI FOR DEMONSTRATION PURPOSES.

// GOING FORWARD WE RECOMMEND IMPLEMENTING A STANDALONE WEB SERVER THAT AUTHORISES
// VIA THE NODE'S RPC INTERFACE. IN THE COMING WEEKS WE'LL WRITE A TUTORIAL ON
// HOW BEST TO DO THIS.

const app = angular.module('demoAppModule', ['ui.bootstrap']);

var groupBy = function(xs, keyFun) {
  return xs.reduce(function(rv, x) {
    (rv[keyFun(x)] = rv[keyFun(x)] || []).push(x);
    return rv;
  }, {});
};


// THIS CODE IS ONLY MADE AVAILABLE FOR DEMONSTRATION PURPOSES AND IS NOT SECURE!
// DO NOT USE IN PR
// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

var outDemoAPp ;
app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;
    outDemoAPp = demoApp;
    // We identify the node.
    const apiBaseURL = "/api/example/";
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openModal = () => {
            const modalInstance = $uibModal.open({
                templateUrl: 'demoAppModal.html',
                controller: 'ModalInstanceCtrl',
                controllerAs: 'modalInstance',
                resolve: {
                    apiBaseURL: () => apiBaseURL,
                    peers: () => peers
                }
            });
               modalInstance.result.then(() => {}, () => {});
         }


     demoApp.openSubscribeModal = () => {
                    const subModalInstance = $uibModal.open({
                        templateUrl: 'subscribe.html',
                        controller: 'SubModalInstanceCtrl',
                        controllerAs: 'subModalInstance',
                        resolve: {
                            apiBaseURL: () => apiBaseURL,
                            peers: () => peers
                        }
                    });

                  subModalInstance.result.then(() => {}, () => {});

            };
    // forward message to agent d
    demoApp.forward2 = (sender1, recipient1, value1, reference1, recordDate1, exDate1, payDate1, stock1, option1, type1) => {
            const forwardToAgentEndpoint =
                apiBaseURL +
                "AgentD" +
                "/updateWithScramble";
            alert("Forwarding to agent...");

            const iou = {
                value: value1,
                reference: reference1,
                recordDate: recordDate1,
                exDate: exDate1,
                payDate: payDate1,
                stock: stock1,
                option: option1,
                type: type1,
                };

            // Create PO and handle success / fail responses.
            $http.put(forwardToAgentEndpoint, angular.toJson(iou));
     };

     demoApp.getIousView = (ious) => {
        var res1 = "";
        var res2 = "";
        var res3 = "";
        var res4 = "";
        var res5 = "";
        var res6 = "";
        var res7 = "";
        var res8 = "";
        var res9 = "";
        var res10 = "";


        for (var i= 0 ; i < ious.length ; i++) {
            res1 += ious[i].sender + "&nbsp;&nbsp;&nbsp;";
            res2 += ious[i].recipient + "&nbsp;&nbsp;&nbsp;";
            res3 += ious[i].iou.value + "&nbsp;&nbsp;&nbsp;";
            res4 += ious[i].iou.reference + "&nbsp;&nbsp;&nbsp;";
            res5 += ious[i].iou.recordDate + "&nbsp;&nbsp;&nbsp;";
            res6 += ious[i].iou.exDate + "&nbsp;&nbsp;&nbsp;";
            res7 += ious[i].iou.payDate + "&nbsp;&nbsp;&nbsp;";
            res8 += ious[i].iou.stock + "&nbsp;&nbsp;&nbsp;";
            res9 += ious[i].iou.option + "&nbsp;&nbsp;&nbsp;";
            res10 += ious[i].iou.type + "&nbsp;&nbsp;&nbsp;";
        }

        var retStr = "<li>Sender: " + res1 + "</li>";
        retStr += "<li>Recipient: " + res2 + "</li>";
        retStr += "<li>Value: " + res3 + "</li>";
        retStr += "<li>Reference: " + res4 + "</li>";
        retStr += "<li>Record Date: " + res5 + "</li>";
        retStr += "<li>Ex-Date: " + res6 + "</li>";
        retStr += "<li>Income Pay Date: " + res7 + "</li>";
        retStr += "<li>Stock: " + res8 + "</li>";
        retStr += "<li>Option: " + res9 + "</li>";
        retStr += "<li>Type: " + res10 + "</li>";
          return resStr;
     };

    demoApp.getIOUs = () => $http.get(apiBaseURL + "ious")
        .then((response) => {
           var list = Object.keys(response.data)
                    .map((key) => response.data[key].state.data)
                    .reverse();
           var grouped = groupBy(  list, (o) => {return  o.iou.reference });
           grouped.length = list.length;
            demoApp.ious = grouped;
            demoApp.CA_Ref = '';
                });

    demoApp.getIOUs();

    demoApp.getSubscribed = () => $http.get(apiBaseURL + "subscribed")
        .then((response) => {

            demoApp.subscribed = response.data

                });
    demoApp.getSubscribed()

    demoApp.Select_CA_Ref = (reference1) => {
        demoApp.CA_Ref=reference1;
    };

});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {
        counterparties : []
    };
    modalInstance.formError = false;

    // Validate and create IOU.
    modalInstance.onChangeStock = () => {
        if(modalInstance.form.stock){
             $http.get(apiBaseURL + "subscribers?stock=" + modalInstance.form.stock
//             {
//                 url: ,
//                 method: "GET",
//              //   params: {'stock': modalInstance.form.stock}
//              }
              ).then((response) => modalInstance.form.counterparties = response.data);

        }else{
        //DEFAULT : BankA
            modalInstance.form.counterparties = ["BankA"]
        }
    }
    modalInstance.create = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;


            const iou = {
                value: modalInstance.form.value,
                reference: modalInstance.form.reference,
                recordDate: modalInstance.form.recordDate,
                exDate: modalInstance.form.exDate,
                payDate: modalInstance.form.payDate,
                stock: modalInstance.form.stock,
                option: modalInstance.form.option,
                type: modalInstance.form.type,
            };


            $uibModalInstance.close();

            for(var i = 0 ; i < modalInstance.form.counterparties.length ; i++){

            var  counterparty = modalInstance.form.counterparties[i]
            const createIOUEndpoint =
                apiBaseURL +
                 counterparty +
                "/create-iou";

            console.log('counterparty' + createIOUEndpoint)

            // Create PO and handle success / fail responses.
            $http.put(createIOUEndpoint, angular.toJson(iou)).then(
                (result) => {
                    outDemoAPp.getIOUs();
                    modalInstance.displayMessage(result) ;
                },
                (result) => modalInstance.displayMessage(result)
            );
            }
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create IOU modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    modalInstance.isChecked  =  ( peer ) => {
        return  modalInstance.form.counterparties.indexOf(peer) >= 0
    }
    modalInstance.updateSelection  =  ( peer ) => {

                   var checked =  modalInstance.form.counterparties.indexOf(peer);
                   if(checked){
                       modalInstance.form.counterparties.push(peer) ;
                   }else{
                       var idx = modalInstance.form.counterparties.indexOf(peer) ;
                       modalInstance.form.counterparties.splice(idx,1) ;
                   }
    }

    // Validate the IOU.
    function invalidFormInput() {
        return isNaN(modalInstance.form.value) //|| (modalInstance.form.counterparty === undefined);
    }
});

app.controller('SubModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const subModalInstance = this;

    subModalInstance.peers = peers;
    subModalInstance.form = {   };
    subModalInstance.formError = false;

    // Validate and create IOU.
    subModalInstance.create = () => {
        if (invalidFormInput()) {
            subModalInstance.formError = true;
        } else {
            subModalInstance.formError = false;

            const iou = {
                stock: subModalInstance.form.stock

            };

            $uibModalInstance.close();



            const createIOUEndpoint =
                apiBaseURL +
                "subscribe";


            // Create PO and handle success / fail responses.
            $http.put(createIOUEndpoint, angular.toJson(iou)).then(
                (result) => {

                outDemoAPp.getSubscribed();
                subModalInstance.displayMessage(result) ;
                },
                (result) => subModalInstance.displayMessage(result)
            );
        }
    };

    subModalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        subModalInstance.result.then(() => {}, () => {});
    };

    // Close create IOU modal dialogue.
    subModalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the IOU.
    function invalidFormInput() {
        return  !subModalInstance.form.stock  //|| (modalInstance.form.counterparty === undefined);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});