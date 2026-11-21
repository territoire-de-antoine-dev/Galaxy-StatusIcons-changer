'use strict';
const el = id => document.getElementById(id);
const message = text => { el('status').textContent = text; };
const storeKey = 'galaxy.studio.packs.v1';
const effects = ['pixelBattery','chargingWave','satellite'];
const testKey = 'stat_sys_wifi_signal_4';
let catalog, library, variant, imageTarget, imagePack;
const draft = () => library.packs[library.selected];
const key = () => el('target').value;
function persist(next) {
    localStorage.setItem(storeKey, JSON.stringify(next));
    library = next;
}
function edit(change) {
    const next = JSON.parse(JSON.stringify(library));
    change(next);
    persist(next);
    render();
}
function render() {
    for (const id of effects) el(id).checked = draft().features?.[id] === true;
    const entries = draft().icons;
    const current = entries[key()];
    el('packs').replaceChildren(...library.packs.map((p,i) => new Option(p.name,i)));
    el('packs').value = library.selected;
    el('enabled').checked = Boolean(current && current.enabled);
    el('enabled').disabled = !current;
    el('night').disabled = !current;
    el('day').disabled = !key();
    el('detail').textContent = !key() ? 'Aucun état correspondant.' : key().startsWith('battery_')
        ? 'Rendu expérimental par tranche de 10 %. Le dessin importé remplace aussi le pourcentage intégré.'
        : 'Drawable SystemUI · ' + catalog.resources[key()];
    for (const mode of ['day','night']) {
        const visible = Object.keys(entries).filter(k => entries[k].enabled && k !== key()).slice(0,3);
        if (current) visible.push(key());
        el(mode + 'Preview').replaceChildren(...visible.map(k => {
            const image = new Image();
            image.src = 'data:image/png;base64,' + (entries[k][mode] || entries[k].day);
            image.alt = k;
            return image;
        }));
    }
}
function options(preferred) {
    const names = Object.keys(catalog.resources);
    if (catalog.build === 'CP2A.260605.016.S911U1UEU8GZIF')
        for (const state of ['normal','charging'])
            for (let i=0;i<=100;i+=10) names.push('battery_' + state + '_' + i);
    const filtered = names.filter(n => n.includes(el('search').value.trim().toLowerCase()));
    el('target').replaceChildren(...filtered.map(n => new Option(n,n)));
    if (filtered.includes(preferred)) el('target').value = preferred;
    render();
}
function makePack(name, icons, features={}) {
    edit(next => {
        next.selected = next.packs.length;
        next.packs.push({name, icons, features});
    });
}
async function raster(file) {
    if (!file || file.size > 4*1024*1024) throw Error('Fichier supérieur à 4 Mio.');
    if (!['image/png','image/svg+xml','image/webp'].includes(file.type)) throw Error('Format attendu PNG, WebP ou SVG.');
    const url = URL.createObjectURL(file);
    try {
        const image = new Image();
        image.src = url;
        await image.decode();
        if (!image.naturalWidth || !image.naturalHeight) throw Error('Dimensions invalides.');
        const scale = 128 / Math.max(image.naturalWidth,image.naturalHeight);
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1,Math.round(image.naturalWidth*scale));
        canvas.height = Math.max(1,Math.round(image.naturalHeight*scale));
        const ctx = canvas.getContext('2d');
        ctx.drawImage(image,0,0,canvas.width,canvas.height);
        const pixels = ctx.getImageData(0,0,canvas.width,canvas.height).data;
        if (!pixels.some((v,i) => i%4 === 3 && v >= 32)) throw Error('Icône transparente ou trop peu visible.');
        return canvas.toDataURL('image/png').split(',')[1];
    } finally { URL.revokeObjectURL(url); }
}
function listen(id,event,fn) {
    el(id).addEventListener(event,async () => {
        try { await fn(); }
        catch(e) { message(e.name === 'QuotaExceededError' ? 'Stockage plein. Le dernier enregistrement est conservé.' : e.message); }
    });
}
async function start() {
    catalog = await (await fetch('profile.json')).json();
    const saved = localStorage.getItem(storeKey);
    library = saved ? JSON.parse(saved) : {selected:0,packs:[{name:'Mon pack',icons:{}}]};
    options(testKey);
    listen('search','input',() => options(key()));
    listen('target','change',render);
    listen('packs','change',() => edit(n => { n.selected = Number(el('packs').value); }));
    for (const id of effects) listen(id,'change',() => edit(n => {
        const current = n.packs[n.selected];
        current.features = {...current.features, [id]:el(id).checked};
    }));
    listen('pixel','click',async () => {
        const pack = await (await fetch('pixel17.json')).json();
        makePack(pack.name,pack.icons,pack.features);
        message('Pack Pixel préparé. Utiliser ce pack pour le publier.');
    });
    listen('new','click',() => { el('naming').hidden=false; el('name').focus(); });
    listen('create','click',() => {
        const name = el('name').value.trim();
        if (!name) throw Error('Donne un nom au pack.');
        makePack(name,{});
        el('naming').hidden=true;
        el('name').value='';
    });
    for (const mode of ['day','night']) listen(mode,'click',() => {
        variant=mode; imageTarget=key(); imagePack=library.selected;
        el('imageFile').value=''; el('imageFile').click();
    });
    listen('imageFile','change',async () => {
        const img = await raster(el('imageFile').files[0]);
        edit(next => {
            const icons = next.packs[imagePack].icons;
            const entry = icons[imageTarget] || {enabled:true};
            entry[variant] = img; icons[imageTarget] = entry;
        });
        message('Image enregistrée dans le brouillon.');
    });
    listen('enabled','change',() => edit(n => { n.packs[n.selected].icons[key()].enabled=el('enabled').checked; }));
    listen('sample','click',async () => {
        const svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24"><path fill="white" d="M1 6h22l-4 4H5zm5 6h12l-4 4h-4zm4 6h4l-2 3z"/></svg>';
        const day = await raster(new File([svg],'wifi.svg',{type:'image/svg+xml'}));
        makePack('Essai Wi-Fi · 4',{[testKey]:{day,enabled:true}});
        el('search').value=''; options(testKey);
        message('Un seul état préparé. Aperçu disponible avant application.');
    });
    listen('apply','click',() => {
        const active = {};
        for (const [k,v] of Object.entries(draft().icons)) if (v.enabled)
            active[k] = v.night ? {day:v.day,night:v.night} : {day:v.day};
        if (effects.some(id => draft().features?.[id] === true)) active._features = draft().features;
        message(Studio.publish(JSON.stringify(active)));
    });
    listen('off','click',() => message(Studio.publish('{}')));
    listen('backup','click',() => Studio.backup(JSON.stringify({version:1,...draft()})));
    listen('restore','click',() => { el('packFile').value=''; el('packFile').click(); });
    listen('packFile','change',async () => {
        const file=el('packFile').files[0];
        if (!file || file.size>4*1024*1024) throw Error('Pack trop grand.');
        const item=JSON.parse(await file.text());
        if (item.version!==1 || typeof item.name!=='string' || !item.name.trim() || !item.icons || Array.isArray(item.icons) || typeof item.icons!=='object') throw Error('Format du pack inconnu.');
        const clean={};
        for (const [name,v] of Object.entries(item.icons)) {
            if (!Object.hasOwn(catalog.resources,name) && !/^battery_(normal|charging)_(100|[0-9]0|0)$/.test(name)) throw Error('Cible inconnue — '+name);
            const entry={enabled:v.enabled===true};
            for (const mode of ['day','night']) {
                if (mode==='night' && !v.night) continue;
                if (typeof v[mode]!=='string' || v[mode].length>100000) throw Error('Image invalide.');
                const bytes=Uint8Array.from(atob(v[mode]),c=>c.charCodeAt(0));
                entry[mode]=await raster(new File([bytes],'import.png',{type:'image/png'}));
            }
            clean[name]=entry;
        }
        makePack(item.name.slice(0,48),clean,Object.fromEntries(effects.map(id => [id,item.features?.[id] === true])));
        message('Pack importé comme nouveau brouillon.');
    });
}
start().catch(e => message(e.message));
