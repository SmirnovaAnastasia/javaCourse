import express from "express";
import pkg from "pg";

const pool = new pkg.Pool({
  host: "localhost",
  port: "5432",
  user: "mao",
  password: "qwe123",
  database: "database",
});


const app = express();
const port = 3001;

app.use(express.json());
app.use(function (req, res, next) {
  res.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
  res.setHeader(
    "Access-Control-Allow-Headers",
    "Content-Type, Access-Control-Allow-Headers"
  );
  next();
});

app.get("/", (req, res) => {
  fun()
    .then((response) => {
      res.status(200).send(response);
    })
    .catch((error) => {
      res.status(500).send(error);
    });
});

app.listen(port, () => {
  console.log(`Program performed`);
});

function fun() {
  return new Promise(function (resolve, reject) {
    pool.query(`SELECT * FROM data`, (error, results) => {
      if (error) {
        reject(error);
      }
      resolve(results.rows);
    });
  });
}
