package ic.doc.forecast;

import static ic.doc.TestUtils.makeItemList;
import static ic.doc.forecast.CachingForecasterProxy.withLimitedCache;
import static ic.doc.forecast.CachingForecasterProxy.withUnlimitedCache;
import static ic.doc.forecast.ForecastTestUtils.randomDay;
import static ic.doc.forecast.ForecastTestUtils.randomDays;
import static ic.doc.forecast.ForecastTestUtils.randomRegion;
import static ic.doc.forecast.ForecastTestUtils.randomRegions;
import static ic.doc.forecast.ForecastTestUtils.randomSummary;
import static ic.doc.forecast.ForecastTestUtils.randomTemperature;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.junit5.JUnit5Mockery;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CachingForecasterProxyTest {

  private static final int CACHE_SIZE_LIMIT = 5;
  private static final Duration ONE_HOUR = Duration.ofHours(1);

  @RegisterExtension
  public JUnit5Mockery context = new JUnit5Mockery();
  Forecaster mockForecaster = context.mock(Forecaster.class);
  InstantSource mockInstantSource = context.mock(InstantSource.class);
  Forecaster unlimitedCachingProxy = withUnlimitedCache(mockForecaster, mockInstantSource);
  Forecaster limitedCachingProxy = withLimitedCache(mockForecaster, CACHE_SIZE_LIMIT,
      mockInstantSource);

  @Test
  public void subsequentLookupsAreCached() {
    // dummy cached query
    Region region = randomRegion();
    Day day = randomDay();
    Forecast forecast = new Forecast(randomSummary(), randomTemperature());

    context.checking(new Expectations() {{
      // mock regular system clock
      allowing(mockInstantSource).instant();
      will(returnValue(Instant.now()));
      // mock return value
      oneOf(mockForecaster).forecastFor(region, day);
      will(returnValue(forecast));
    }});

    // first time, no cache
    assertEquals(unlimitedCachingProxy.forecastFor(region, day), forecast);

    // subsequent times, hit cache
    for (int i = 0; i < 5; i++) {
      assertEquals(unlimitedCachingProxy.forecastFor(region, day), forecast);
    }
  }

  @Test
  public void limitedCacheMeansOldEntriesAreEvictedWhenMaximumSizeIsReached() {
    // dummy cached queries
    int numberOfQueries = CACHE_SIZE_LIMIT + 1;
    List<Region> regions = randomRegions(numberOfQueries);
    List<Day> days = randomDays(numberOfQueries);
    List<Forecast> forecasts = makeItemList(numberOfQueries,
        i -> new Forecast(randomSummary(), randomTemperature()));
    Sequence mockForecasterCallSequence = context.sequence("mock forecaster call sequence");


    context.checking(new Expectations() {{
      // mock regular system clock
      allowing(mockInstantSource).instant();
      will(returnValue(Instant.now()));

      // all queries should initially go through
      for (int i = 0; i < CACHE_SIZE_LIMIT; i++) {
        oneOf(mockForecaster).forecastFor(regions.get(i), days.get(i));
        inSequence(mockForecasterCallSequence);
        will(returnValue(forecasts.get(i)));
      }
    }});


    // populate cache
    // subsequent times, hit cache
    for (int i = 0; i < CACHE_SIZE_LIMIT; i++) {
      assertEquals(limitedCachingProxy.forecastFor(regions.get(i), days.get(i)), forecasts.get(i));
    }
    for (int i = 0; i < CACHE_SIZE_LIMIT; i++) {
      assertEquals(limitedCachingProxy.forecastFor(regions.get(i), days.get(i)), forecasts.get(i));
    }


    // now we can test the behavior of evicting the oldest item from cache
    context.assertIsSatisfied();
    context.checking(new Expectations() {{
      // mock regular system clock
      allowing(mockInstantSource).instant();
      will(returnValue(Instant.now()));

      // this new item, not in cache, should evict the old item
      oneOf(mockForecaster).forecastFor(regions.get(CACHE_SIZE_LIMIT), days.get(CACHE_SIZE_LIMIT));
      inSequence(mockForecasterCallSequence);
      will(returnValue(forecasts.get(CACHE_SIZE_LIMIT)));

      // then the evicted item should go through after
      oneOf(mockForecaster).forecastFor(regions.getFirst(), days.getFirst());
      inSequence(mockForecasterCallSequence);
      will(returnValue(forecasts.getFirst()));
    }});


    // add a new item to cache, which should evict the old one from cache
    // subsequent times, this item should hit cache
    assertEquals(
        limitedCachingProxy.forecastFor(regions.get(CACHE_SIZE_LIMIT), days.get(CACHE_SIZE_LIMIT)),
        forecasts.get(CACHE_SIZE_LIMIT));
    for (int i = 0; i < 5; i++) {
      assertEquals(limitedCachingProxy.forecastFor(regions.get(CACHE_SIZE_LIMIT),
          days.get(CACHE_SIZE_LIMIT)), forecasts.get(CACHE_SIZE_LIMIT));
    }


    // but the evicted item should now go through
    // subsequent times, this item should hit cache
    assertEquals(limitedCachingProxy.forecastFor(regions.getFirst(), days.getFirst()),
        forecasts.getFirst());
    for (int i = 0; i < 5; i++) {
      assertEquals(limitedCachingProxy.forecastFor(regions.getFirst(), days.getFirst()),
          forecasts.getFirst());
    }
  }

  @Test
  public void forecastsAreOnlyCachedForOneHour() {
    // dummy cached query
    Region region = randomRegion();
    Day day = randomDay();
    Forecast forecast = new Forecast(randomSummary(), randomTemperature());
    Sequence cacheInvalidationSequence = context.sequence("cache invalidated after an hour");


    // first time, no cache
    context.checking(new Expectations() {{
      // the item is first placed in cache, with its timestamp recorded
      oneOf(mockForecaster).forecastFor(region, day);
      inSequence(cacheInvalidationSequence);
      will(returnValue(forecast));

      oneOf(mockInstantSource).instant();
      inSequence(cacheInvalidationSequence);
      will(returnValue(Instant.now()));
    }});
    assertEquals(unlimitedCachingProxy.forecastFor(region, day), forecast);
    context.assertIsSatisfied();


    // subsequent times, hit cache
    context.checking(new Expectations() {{
      // mock regular system clock
      allowing(mockInstantSource).instant();
      will(returnValue(Instant.now()));
    }});
    for (int i = 0; i < 5; i++) {
      assertEquals(unlimitedCachingProxy.forecastFor(region, day), forecast);
    }
    context.assertIsSatisfied();


    // now test cache invalidation after one hour
    context.checking(new Expectations() {{
      // pretend an hour has passed, so cache should be invalidated
      oneOf(mockInstantSource).instant();
      inSequence(cacheInvalidationSequence);
      will(returnValue(Instant.now().plus(ONE_HOUR)));

      // and since it was invalidated, a call to the underlying object should be expected
      oneOf(mockForecaster).forecastFor(region, day);
      inSequence(cacheInvalidationSequence);
      will(returnValue(forecast));
    }});
    assertEquals(unlimitedCachingProxy.forecastFor(region, day), forecast);
  }
}
