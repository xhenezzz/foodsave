import test from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../src/api.ts';

test('API получает город покупателя в запросе каталога и nearby', async () => {
  const previousFetch=globalThis.fetch;
  const previousStorage=globalThis.localStorage;
  const seen=[];
  globalThis.localStorage={getItem:()=>null};
  globalThis.fetch=async url=>{seen.push(String(url));return new Response('[]',{status:200,headers:{'Content-Type':'application/json'}});};
  try {
    await api.getListings('ALMATY');
    await api.getNearbyListings(43.24,76.91,10,'ALMATY');
    assert.match(seen[0],/\/listings\?city=ALMATY$/);
    assert.match(seen[1],/latitude=43.24/);
    assert.match(seen[1],/longitude=76.91/);
    assert.match(seen[1],/radius=10/);
    assert.match(seen[1],/city=ALMATY$/);
  } finally {globalThis.fetch=previousFetch;globalThis.localStorage=previousStorage;}
});
