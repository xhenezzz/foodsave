import test from 'node:test';
import assert from 'node:assert/strict';
import { listingCity, listingsForCity, coordinatesCity, listingAvailable, confirmedPayment, orderPaidByServer, kilometers } from '../src/utils.ts';

const listing = {
  id:'almaty-1',title:'Хлеб',description:'',originalPrice:2000,foodSavePrice:800,quantity:5,
  pickupStart:'2099-01-01T11:00:00',pickupEnd:'2099-01-01T14:00:00',expiresAt:'2099-01-01T15:00:00',
  status:'ACTIVE',latitude:43.24,longitude:76.91
};
test('каталог Алматы распознаёт Алматы, но не Астану',()=>{
  assert.equal(listingCity(listing),'ALMATY');
  assert.equal(listingCity({...listing,latitude:51.17,longitude:71.45}),'ASTANA');
});
test('без города и координат объявление не показываем',()=>{
  assert.equal(listingCity({...listing,latitude:undefined,longitude:undefined}),null);
  assert.equal(listingCity({...listing,latitude:60.0,longitude:80.0}),null);
});
test('явный город API принимаем без координат; противоречивые данные скрываем',()=>{
  assert.equal(listingCity({...listing,city:'ASTANA',latitude:undefined,longitude:undefined}),'ASTANA');
  assert.equal(listingCity({...listing,city:'ALMATY'}),'ALMATY');
  assert.equal(listingCity({...listing,city:'ASTANA'}),null);
  assert.equal(listingCity({...listing,city:'ALMATY',latitude:42.3,longitude:69.6}),null);
  assert.equal(listingCity({...listing,city:'ALMATY',businessCity:'ASTANA'}),null);
});
test('фильтр города не пропускает чужие объявления, даже если backend вернул все',()=>{
  const all=[listing,{...listing,id:'astana',latitude:51.17,longitude:71.45},{...listing,id:'unknown',latitude:undefined,longitude:undefined}];
  assert.deepEqual(listingsForCity(all,'ALMATY').map(l=>l.id),['almaty-1']);
  assert.deepEqual(listingsForCity(all,'ASTANA').map(l=>l.id),['astana']);
});
test('геолокация пользователя проверяется на выбранный город',()=>{
  assert.equal(coordinatesCity(43.24,76.91),'ALMATY');
  assert.equal(coordinatesCity(51.17,71.45),'ASTANA');
  assert.equal(coordinatesCity(0,0),null);
  assert.equal(coordinatesCity(120,250),null);
});
test('истёкшие/пустые/отменённые объявления не бронируются',()=>{
  assert.equal(listingAvailable(listing),true);
  assert.equal(listingAvailable({...listing,quantity:0}),false);
  assert.equal(listingAvailable({...listing,status:'CANCELLED'}),false);
  assert.equal(listingAvailable({...listing,expiresAt:'2020-01-01T00:00:00'}),false);
});
test('платёж подтверждается только успешным статусом',()=>{
  for(const status of ['SUCCESS','SUCCEEDED','PAID','COMPLETED','CAPTURED'])assert.equal(confirmedPayment(status),true);
  for(const status of ['PENDING','FAILED','CANCELLED',''])assert.equal(confirmedPayment(status),false);
  assert.equal(orderPaidByServer({paid:true}),true);
  assert.equal(orderPaidByServer({paymentStatus:'SUCCESS'}),true);
  assert.equal(orderPaidByServer({paymentStatus:'PENDING'}),false);
});
test('расчёт расстояния соответствует городам',()=>{
  assert.ok(kilometers(43.24,76.91,51.17,71.45)>900);
});
