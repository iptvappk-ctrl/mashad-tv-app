const { aggregator } = require('../src/lib/providers/aggregator.ts');

async function testAggregator() {
  console.log('--- TESTING MULTI-PROVIDER AGGREGATOR ---');
  console.log('Active Providers:', aggregator.providers.map(p => p.name).join(', '));

  console.log('\n1. Testing Parallel Search for "Batman"...');
  const t0 = Date.now();
  const searchResults = await aggregator.searchAll('Batman');
  console.log(`Search completed in ${Date.now() - t0}ms. Found ${searchResults.length} items.`);
  searchResults.slice(0, 5).forEach((item, idx) => {
    console.log(`  ${idx + 1}. [${item.providerName}] ${item.title}`);
  });

  console.log('\n2. Testing Ramadan Category Aggregation...');
  const t1 = Date.now();
  const ramadanResults = await aggregator.getCategory('ramadan', 1);
  console.log(`Ramadan category completed in ${Date.now() - t1}ms. Found ${ramadanResults.length} items.`);
  ramadanResults.slice(0, 5).forEach((item, idx) => {
    console.log(`  ${idx + 1}. [${item.providerName}] ${item.title}`);
  });
}

testAggregator();
