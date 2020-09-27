app.controller('indexController' ,function($scope,$controller   ,loginService) {

$scope.showLoginName=function () {
    loginService.showName().success(function (response) {
        $scope.showName=response.showName;
    })
}


});