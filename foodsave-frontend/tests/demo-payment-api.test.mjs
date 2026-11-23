import test from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../src/api.ts';

test('startDemoPayment creates a PENDING session; it does not immediately return SUCCESS', async () => {
  const originalFetch = globalThis.fetch;
  const originalStorage = globalThis.localStorage;
  const calls=[];
  globalThis.localStorage={getItem:()=> 'fake-test-jwt'};
  globalThis.fetch=async (url,opts)=>{
    calls.push({url:String(url),method:opts.method,auth:opts.headers.get('Authorization')});
    return new Response(JSON.stringify({id:'pay-1',orderId:'ord-1',amount:1200,status:'PENDING',createdAt:'2026-11-04T14:00:00'}),{status:200});
  };
  try {
    const payment=await api.startDemoPayment('ord-1');
    assert.equal(payment.status,'PENDING');
    assert.equal(calls[0].url,'http://localhost:8081/api/payments/ord-1/demo/start');
    assert.equal(calls[0].method,'POST');
    assert.equal(calls[0].auth,'Bearer fake-test-jwt');
  } finally {globalThis.fetch=originalFetch;globalThis.localStorage=originalStorage;}
});

test('confirmDemoPayment submits an explicit demo decision, never real bank card data', async () => {
  const previousFetch=globalThis.fetch;
  const previousStorage=globalThis.localStorage;
  globalThis.localStorage={getItem:()=>null};
  let request;
  globalThis.fetch=async (url,opts)=>{
    request={url:String(url),body:JSON.parse(opts.body)};
    return new Response(JSON.stringify({id:'p-1',orderId:'o-2',amount:1200,status:'FAILED'}),{status:200});
  };
  try {
    const result=await api.confirmDemoPayment('o-2','DECLINE');
    assert.equal(result.status,'FAILED');
    assert.match(request.url,/\/payments\/o-2\/demo\/confirm$/);
    assert.deepEqual(request.body,{decision:'DECLINE'});
  } finally {globalThis.fetch=previousFetch;globalThis.localStorage=previousStorage;}
});
