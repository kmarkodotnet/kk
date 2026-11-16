// Local Chromium CDP: deterministic frames and a real, silent WebM export.
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const pages = await (await fetch('http://127.0.0.1:9337/json')).json();
const ws = new WebSocket(pages.find(p=>p.type==='page').webSocketDebuggerUrl);
await new Promise(r => ws.addEventListener('open', r, {once:true}));
let id=0;const pending=new Map();
ws.addEventListener('message',e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(m.error):p.resolve(m.result)}});
ws.addEventListener('close',()=>{for(const p of pending.values())p.reject(Error('Preview browser connection closed'));pending.clear()});
const call=(method,params={})=>new Promise((resolve,reject)=>{pending.set(++id,{resolve,reject});ws.send(JSON.stringify({id,method,params}))});
ws.addEventListener('message',e=>{const m=JSON.parse(e.data);if(m.method==='Runtime.exceptionThrown')console.error(JSON.stringify(m.params))});
await call('Runtime.enable');
await call('Emulation.setDeviceMetricsOverride',{width:900,height:1600,deviceScaleFactor:1,mobile:false});
await call('Page.navigate',{url:'file:///F:/Claude/kyo/output/Kyokushin-Intro.html?t=0'});
await new Promise(r=>setTimeout(r,1000));
const ready=await call('Runtime.evaluate',{expression:`new Promise((resolve,reject)=>{const deadline=performance.now()+15000;function poll(){if(window.intro?.ready)return resolve(true);if(performance.now()>deadline)return reject(Error('Asset decode timeout'));setTimeout(poll,50)}poll()})`,awaitPromise:true,returnByValue:true});
assert.equal(ready.result?.value,true,'Both illustrated assets must decode before rendering');
const checks=await call('Runtime.evaluate',{expression:`(()=>{const failures=[];for(let t=0;t<=14;t+=.5){intro.renderAt(t);for(const [x,y] of [[899,0],[899,1599]]){const p=ctx.getImageData(x,y,1,1).data;if(p[0]!==255||p[1]!==255||p[2]!==255)failures.push('Background '+t)}}for(const t of [5.8,6.8,8]){intro.renderAt(t);const data=ctx.getImageData(0,0,900,1600).data,front=hit.x-(hit.x-leftLimit)*ramp(t,5.4,8.8);let n=0;for(let y=470;y<1060;y++)for(let x=0;x<900;x++){const i=(y*900+x)*4;if(data[i+2]>130&&data[i+2]>data[i]+65&&data[i+1]>data[i]+20){n++;if(x<front-3||x>hit.x+3)failures.push('Directional flame '+t+':'+x)}}if(!n)failures.push('Missing flame '+t)}intro.renderAt(2.5);for(const [x,y] of [[0,0],[0,1599],[400,0],[400,1599],[hit.x-80,hit.y],[hit.x+80,hit.y],[hit.x,hit.y-80],[hit.x,hit.y+80]]){const p=ctx.getImageData(x,y,1,1).data;if(!(p[0]>p[2]+35&&p[1]>p[2]+15))failures.push('Upright board face '+x+':'+y)}ctx.fillStyle='#fff';ctx.fillRect(0,0,900,1600);energy(5.2);const before=ctx.getImageData(0,0,900,1600).data;if(before.some((v,i)=>i%4!==3&&v!==255))failures.push('Fire before complete crack hold');if(intro.geometry(8.8).angle!==Math.PI/2||intro.geometry(14).angle!==0||!(intro.geometry(10).angle>intro.geometry(11).angle))failures.push('Orientation');return failures.slice(0,10)})()`,returnByValue:true});
assert.deepEqual(checks.result?.value,[],'White background, one-way propagation and rotation');
const crackCoverage=await call('Runtime.evaluate',{expression:`(()=>{
 ctx.setTransform(1,0,0,1,0,0);ctx.globalAlpha=1;ctx.fillStyle='#fff';ctx.fillRect(0,0,900,1600);cracks(4.2);
 const tr=logoTransform(0),missing=[];
 medial.forEach((chain,index)=>{
  const [px,py]=chain[Math.floor(chain.length/2)];
  const x=Math.round(tr.cx-(py-art.height/2)*tr.scale),y=Math.round(tr.cy+(px-art.width/2)*tr.scale);
  const pixels=ctx.getImageData(x-2,y-2,5,5).data;let dark=false;
  for(let i=0;i<pixels.length;i+=4)if(pixels[i]<170&&pixels[i+1]<130&&pixels[i+2]<100)dark=true;
  if(!dark)missing.push(index);
 });return {branches:medial.length,missing};
})()`,returnByValue:true});
assert.deepEqual(crackCoverage.result?.value?.missing,[],'Every base branch must already be a dark crack before the fire');
console.log(`Complete dark crack network before fire: ${crackCoverage.result.value.branches} branches PASS.`);
const fistCheck=await call('Runtime.evaluate',{expression:`(()=>{
 const bounds=t=>{ctx.setTransform(1,0,0,1,0,0);ctx.globalAlpha=1;ctx.fillStyle='#fff';ctx.fillRect(0,0,900,1600);fist(t);const d=ctx.getImageData(0,0,900,1600).data;let min=900;for(let y=500;y<1100;y++)for(let x=0;x<900;x++){const i=(y*900+x)*4;if(d[i]<245||d[i+1]<245||d[i+2]<245)min=Math.min(min,x)}return min};
 const hidden=[0,.5,1.8,2.8,3,5.4].every(t=>bounds(t)===900);
 const strike=bounds(3.25),retreat=bounds(4.5);
 return {hidden,strike,retreat,slowReturn:retreat>strike&&retreat<900};
})()`,returnByValue:true});
assert.equal(fistCheck.result?.value?.hidden,true,'No early peek or lingering fist');
assert.equal(fistCheck.result?.value?.slowReturn,true,'Fist remains visibly withdrawing after impact');
console.log('Fist: no early peek, impact visible, slower withdrawal, fully offscreen afterwards: PASS.');
console.log('White exposed background (29 frames), edge-to-edge board, crack-only hold, leftward flame (3 frames), rotation: PASS.');
for(const t of [1.8,3.15,3.35,4.5,7,8.8,10,11.5,13.8]){
 const result=await call('Runtime.evaluate',{expression:`window.intro.renderAt(${t});document.getElementById('controls').style.display='none';canvas.toDataURL('image/png')`,returnByValue:true});
 if(result.exceptionDetails)throw result.exceptionDetails;
 await fs.writeFile(`tmp/intro-${t}.png`,Buffer.from(result.result.value.split(',')[1],'base64'));
}
console.log('Nine animation frames rendered.');
if(process.argv.includes('--video')){
 const r=await call('Runtime.evaluate',{expression:`new Promise((resolve,reject)=>{const chunks=[];const stream=canvas.captureStream(30);const recorder=new MediaRecorder(stream,{mimeType:'video/webm;codecs=vp9',videoBitsPerSecond:5000000});recorder.onerror=reject;recorder.ondataavailable=e=>chunks.push(e.data);recorder.onstop=()=>{stream.getTracks().forEach(t=>t.stop());const reader=new FileReader();reader.onload=()=>{window.__introVideo=reader.result.split(',')[1];resolve(window.__introVideo.length)};reader.readAsDataURL(new Blob(chunks,{type:'video/webm'}))};recorder.start();window.intro.play();setTimeout(()=>recorder.stop(),14500)})`,awaitPromise:true,returnByValue:true});
 if(r.exceptionDetails)throw r.exceptionDetails;
 const pieces=[];
 for(let offset=0;offset<r.result.value;offset+=262144){const chunk=await call('Runtime.evaluate',{expression:`window.__introVideo.slice(${offset},${offset+262144})`,returnByValue:true});pieces.push(chunk.result.value)}
 await fs.writeFile('output/Kyokushin-Intro.webm',Buffer.from(pieces.join(''),'base64'));
 console.log('14-second WebM exported.');
}
ws.close();
