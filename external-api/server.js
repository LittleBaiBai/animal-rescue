const http = require('http');
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

const server = http.createServer(async (req, res) => {
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
