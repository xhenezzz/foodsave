import test from 'node:test';
import assert from 'node:assert/strict';
import { listingDistance, recommendationScore, recommendationReason, sortListings } from '../src/recommendations.ts';

const mk = (id, override={}) => ({
  id,title:'Хлеб',description:'',originalPrice:2000,foodSavePrice:1000,quantity:5,
  pickupStart:'2099-01-01T11:00:00',pickupEnd:'2099-01-01T14:00:00',expiresAt:'2099-01-01T15:00:00',
  status:'ACTIVE',businessRating:4.5,latitude:43.24,longitude:76.9,...override
});

test('лучше скидка — выше score при одинаковых остальных параметрах',()=>{
  assert.ok(recommendationScore(mk('high',{foodSavePrice:500}))>recommendationScore(mk('low',{foodSavePrice:1500})));
});
test('ближайшее объявление выше при доступной геолокации',()=>{
  const user={lat:43.24,lon:76.9};
  assert.ok(recommendationScore(mk('near'),user)>recommendationScore(mk('far',{latitude:43.3,longitude:76.95}),user));
});
test('без геолокации близость не учитывается и не выдумывается',()=>{
  assert.equal(listingDistance(mk('a'),null),null);
  assert.equal(recommendationScore(mk('a')),recommendationScore(mk('b',{latitude:51,longitude:71})));
});
test('нет рейтинга: score остаётся корректным и нормализованным',()=>{
  const score=recommendationScore(mk('unknown',{businessRating:undefined}));
  assert.ok(score>=0&&score<=100);
  assert.ok(score>0);
});
test('все значения score в диапазоне 0–100 даже при странных ценах',()=>{
  for(const listing of [mk('bad',{originalPrice:0}),mk('neg',{foodSavePrice:-100}),mk('high',{foodSavePrice:3000})]){
    assert.ok(recommendationScore(listing)>=0&&recommendationScore(listing)<=100);
  }
});
test('рекомендации сортируются, не изменяя исходный массив',()=>{
  const listings=[mk('cheap',{foodSavePrice:300}),mk('expensive',{foodSavePrice:1500})];
  const sorted=sortListings(listings,'recommended');
  assert.equal(sorted[0].id,'cheap');
  assert.equal(listings[0].id,'cheap');
  assert.notEqual(sorted,listings);
  assert.equal(sortListings(listings,'price')[0].id,'cheap');
});
test('дистанционная сортировка с координатами использует фактическую дистанцию',()=>{
  const listings=[mk('far',{latitude:43.35,longitude:76.99}),mk('near')];
  assert.equal(sortListings(listings,'distance',{lat:43.24,lon:76.9})[0].id,'near');
});
test('объяснение рекомендации соответствует фактическим критериям',()=>{
  assert.match(recommendationReason(mk('sale',{foodSavePrice:600})),/Скидка|скидка/);
  assert.match(recommendationReason(mk('close',{foodSavePrice:1800}),{lat:43.24,lon:76.9}),/Рядом/);
});

test('recommendationScore с backend имеет приоритет над локальным алгоритмом', async () => {
  const {backendRecommendationScore, effectiveRecommendationScore} = await import('../src/recommendations.ts');
  const expensive = mk('expensive', {foodSavePrice:1800, recommendationScore:95});
  const cheap = mk('cheap', {foodSavePrice:200, recommendationScore:15});
  assert.ok(recommendationScore(cheap) > recommendationScore(expensive));
  assert.equal(backendRecommendationScore(expensive), 95);
  assert.equal(effectiveRecommendationScore(expensive), 95);
  assert.deepEqual(sortListings([cheap, expensive], 'recommended').map(l=>l.id), ['expensive','cheap']);
});

test('невалидный backend score заменяется локальным рейтингом', async () => {
  const {backendRecommendationScore, effectiveRecommendationScore} = await import('../src/recommendations.ts');
  for (const invalid of [undefined, null, -1, 101, NaN, Infinity]) {
    const item = mk('x', {recommendationScore:invalid});
    assert.equal(backendRecommendationScore(item), null);
    assert.equal(effectiveRecommendationScore(item), recommendationScore(item));
  }
  assert.equal(effectiveRecommendationScore(mk('valid',{recommendationScore:0})),0);
});

test('другие режимы сортировки не перезаписываются backend рейтингом',()=>{
  const high=mk('higher',{foodSavePrice:1600,recommendationScore:90});
  const low=mk('lower',{foodSavePrice:300,recommendationScore:10});
  assert.equal(sortListings([high,low],'discount')[0].id,'lower');
  assert.equal(sortListings([high,low],'price')[0].id,'lower');
});
