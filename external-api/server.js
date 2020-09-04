const https = require('https');
const tls = require('tls');
const fs = require('fs');
const axios = require('axios');

const animalRescueBaseUrl = process.env.ANIMAL_RESCUE_BASE_URL;
const animalRescueUsername = process.env.ANIMAL_RESCUE_USERNAME || '';
const animalRescuePassword = process.env.ANIMAL_RESCUE_PASSWORD || '';

const requestAnimalsFromAnimalRescue = async () => {
    try {
        const response = await axios.get(`${animalRescueBaseUrl}/api/animals`);
        // TODO: Use this instead for making request with manual basic auth
        // const response = await axios.get(`${animalRescueBaseUrl}/api/animals`, {
        //     auth: {
        //         username: animalRescueUsername,
        //         password: animalRescuePassword
        //     }
        // });
        return { animals: response.data };
    } catch (e) {
        console.error(e);
        return { error: e };
    }
};

const config = {
    ca: '/var/run/autocert.step.sm/root.crt',
    key: '/var/run/autocert.step.sm/site.key',
    cert: '/var/run/autocert.step.sm/site.crt',
    ciphers: 'ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256',
    minVersion: 'TLSv1.2',
    maxVersion: 'TLSv1.2'
};

const createSecureContext = () => {
    return tls.createSecureContext({
        ca: fs.readFileSync(config.ca),
        key: fs.readFileSync(config.key),
        cert: fs.readFileSync(config.cert),
        ciphers: config.ciphers,
    });
};

let ctx = createSecureContext();

fs.watch(config.cert, (event, filename) => {
    if (event === 'change') {
        ctx = createSecureContext();
    }
});

const serverOptions = {
    requestCert: true,
    rejectUnauthorized: true,
    SNICallback: (servername, cb) => {
        cb(null, ctx);
    }
};

const server = https.createServer(serverOptions, async (req, res) => {
    if (req.url === '/') {
        res.writeHead(200, {'Content-Type': 'text/html'});

        const {animals, error} = await requestAnimalsFromAnimalRescue();
        if(error) {
            console.error(error);
            res.write(`<html><body><p>Failed to retrieve animals: ${error}</body></html>`);
        } else {
            const animalHtmlList = animals.map(animal => `<li>${animal.name}</li>`).join('');
            res.write(`<html><body><p>Animals available at Animal Rescue: ${animalHtmlList}</body></html>`);
        }

        res.end();
    }
});

server.listen(5000);
console.info('Partner Adoption Center web server is running on port 5000..');
