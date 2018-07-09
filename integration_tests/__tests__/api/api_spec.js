const frisby = require('frisby');
const Joi = frisby.Joi; // Frisby exports Joi for convenience on type assersions

it ('iDempiere login endpoint should return a status of 200 and user detail', function () {
    return frisby
      .setup({
        request: {
          headers: {
            'Content-Type': 'text/plain; charset=UTF-8',
          }
        }
      })
      .get('http://localhost:8008/idempiere/api/authentication?username=GardenUser&password=GardenUser')
      .expect('status', 200)
      .expect('json', 'logged', false);
});

it ('iDempiere login should work when orgId is sent too', function () {
    return frisby
      .setup({
        request: {
          headers: {
            'Content-Type': 'text/plain; charset=UTF-8',
          }
        }
      })
      .get('http://localhost:8008/idempiere/api/authentication?username=GardenUser&password=GardenUser&orgId=11')
      .expect('status', 200)
      .expect('json', 'logged', true);
});
