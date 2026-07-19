const sqlite3 = require('sqlite3');
const db = new sqlite3.Database('C:/Users/vinicius/AppData/Local/acerola/app.db');
db.all("PRAGMA table_info(comic_metadata)", (err, rows) => {
    if (err) console.error(err);
    else console.log(rows);
});
