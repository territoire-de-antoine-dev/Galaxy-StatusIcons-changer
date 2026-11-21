// Browser smoke test. The Android bridge is replaced by a recorder on localhost.
const assert = require('node:assert/strict');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const puppeteer = require('puppeteer-core');
const assets = path.join(__dirname,'../app/src/main/assets');
async function main() {
    const mime = {html:'text/html',js:'text/javascript',css:'text/css',json:'application/json'};
    const server = http.createServer((req,res) => {
        const name = req.url.slice(1) || 'index.html';
        if (!['index.html','style.css','editor.js','profile.json'].includes(name)) {
            res.writeHead(404).end(); return;
        }
        res.writeHead(200,{'Content-Type':mime[name.split('.').pop()] + '; charset=utf-8'});
        fs.createReadStream(path.join(assets,name)).pipe(res);
    });
    await new Promise(resolve => server.listen(0,'127.0.0.1',resolve));
    let browser;
    try {
        browser = await puppeteer.launch({executablePath:process.env.CHROME_BIN || '/usr/bin/google-chrome',
            headless:true,args:['--no-sandbox']});
        const page = await browser.newPage();
        await page.evaluateOnNewDocument(() => {
            window.Studio = {publish(text){window.sent=JSON.parse(text);return 'OK';},
                             backup(text){window.backed=JSON.parse(text);}};
        });
        await page.goto('http://127.0.0.1:' + server.address().port);
        await page.waitForSelector('#target option');
        await page.click('#sample');
        await page.waitForFunction(() => document.querySelector('#dayPreview img')?.naturalWidth > 0);
        await page.click('#apply');
        assert.deepEqual(await page.evaluate(() => Object.keys(window.sent)),['stat_sys_wifi_signal_4']);
        assert.ok(await page.evaluate(() => window.sent.stat_sys_wifi_signal_4.day.startsWith('iVBOR')));
        await page.click('#enabled'); await page.click('#apply');
        assert.deepEqual(await page.evaluate(() => window.sent),{});
        await page.click('#enabled'); await page.click('#backup');
        await page.evaluate(() => {
            const transfer=new DataTransfer();
            transfer.items.add(new File([JSON.stringify(window.backed)],'pack.json',{type:'application/json'}));
            const input=document.querySelector('#packFile');
            input.files=transfer.files;
            input.dispatchEvent(new Event('change'));
        });
        await page.waitForFunction(() => document.querySelector('#packs').options.length === 3);
        await page.reload();
        await page.waitForFunction(() => document.querySelector('#packs').value === '2');
        await page.click('#apply'); await page.click('#off');
        assert.deepEqual(await page.evaluate(() => window.sent),{});
        console.log('Essai Wi-Fi unique, conversion SVG en PNG, sélection, export/import, persistance et pack vide validés.');
    } finally { if (browser) await browser.close(); server.close(); }
}
main().catch(e => {console.error(e);process.exitCode=1;});
