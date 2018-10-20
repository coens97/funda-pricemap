const express = require('express')
const app = express()
const port = 3000

app.use('/funda-pricemap', express.static('../docs'))

app.listen(port, () => console.log(`Devserver reachable on http://0.0.0.0:${port}/funda-pricemap/`))