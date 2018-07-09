var chakram = require('chakram');
var common = require('./common');

var chai = require('chai');  
var assert = chai.assert;    // Using Assert style
var expect = chai.expect;    // Using Expect style

describe("GT2Base", function() {
    it("should login successfully", function () {
        common.login().done(function (token) {
            //console.log( "token", token );
        });
    });
    it("should get all actions", function () {
        common.login().done(function (token) {
            chakram.get("http://localhost:8008/gt2base/api/simpledata/cgs_all_actions_v?token=" + token ).
            done( function(response) {
                var body = response.body;
                expect( body.cgs_all_actions_v ).to.not.equal(null);
                var cgs_all_actions_v = body.cgs_all_actions_v;
                expect( cgs_all_actions_v).to.be.an('array');
                expect( cgs_all_actions_v[ 0 ] ).to.not.equal(null);
            })
        });
    });
    it("should get return standard Business Partner", function () {
        common.login().done(function (token) {
            chakram.get("http://localhost:8008/gt2base/api/simpledata/C_BPartner/118?token=" + token ).
            done( function(response) {
                var body = response.body;
                expect( body.C_BPartner ).to.not.equal(null);
                var C_BPartner = body.C_BPartner;
                expect( body.C_BPartner.ad_OrgBP_ID_Int ).to.equal(0);
                expect( C_BPartner.primaryC_BPartner_Location ).to.not.equal(null);
                expect( C_BPartner.primaryC_BPartner_Location.location ).to.not.equal(null);
                expect( C_BPartner.primaryC_BPartner_Location.location.c_Country_ID ).to.equal(100);
            })
        });
    });
    it("should get all users", function () {
        common.login().done(function (token) {
            chakram.get("http://localhost:8008/gt2base/api/data/cgs_users_v?token=" + token ).
            done( function(response) {
                var body = response.body;
                expect( body.tables ).to.not.equal(null);
                var tables = body.tables;
                expect( tables).to.be.an('array');
                expect( tables[ 0 ] ).to.not.equal(null);
                var cgs_users_v = tables[ 0 ];
                expect( cgs_users_v.tableName ).to.equal('cgs_users_v')
                expect( cgs_users_v.data ).to.be.an('array');
            })
        });
    });
    it("should get all countries", function () {
        common.login().done(function (token) {
            chakram.get("http://localhost:8008/gt2base/api/simpledata/c_country_v?orderBy=description&token=" + token ).
            done( function(response) {
                var body = response.body;
                expect( body.c_country_v ).to.not.equal(null);
                var c_country_v = body.c_country_v;
                expect( c_country_v).to.be.an('array');
                expect( c_country_v[ 0 ] ).to.not.equal(null);
                var af = c_country_v[ 0 ];
                expect( af.countrycode ).to.equal('AF')
            })
        });
    });    
});
