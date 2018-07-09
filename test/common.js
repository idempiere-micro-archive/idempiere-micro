var chakram = require('chakram');
var chai = require('chai');  
var assert = chai.assert;    // Using Assert style
var expect = chai.expect;    // Using Expect style

module.exports = {
    login: function() {
        return chakram.get("http://localhost:8008/gt2base/api/authentication?username=gardenuser&password=GardenUser")
        .then(function (request) {
            var body = request.body;
            //console.log("body", body);
            expect( body.logged ).to.equal(true);
            expect( body.token ).to.not.equal(null);
            expect( body.token ).to.not.equal("");
            return body.token;
        });
    }
}
