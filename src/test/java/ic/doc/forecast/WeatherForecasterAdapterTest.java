package ic.doc.forecast;

import static ic.doc.forecast.ForecastTestUtils.DAYS;
import static ic.doc.forecast.ForecastTestUtils.REGIONS;
import static ic.doc.forecast.ForecastTestUtils.inspect;
import static ic.doc.forecast.ForecastTestUtils.makeItemList;
import static ic.doc.forecast.ForecastTestUtils.randomDay;
import static ic.doc.forecast.ForecastTestUtils.randomDays;
import static ic.doc.forecast.ForecastTestUtils.randomRegion;
import static ic.doc.forecast.ForecastTestUtils.randomRegions;
import static ic.doc.forecast.ForecastTestUtils.randomSummaries;
import static ic.doc.forecast.ForecastTestUtils.randomTemperatures;
import static ic.doc.forecast.WeatherForecasterAdapter.adapt;

import static org.jmock.AbstractExpectations.any;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ic.doc.util.Pair;
import ic.doc.util.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.jmock.junit5.JUnit5Mockery;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class WeatherForecasterAdapterTest {

  // constants, to eliminate magic numbers
  private static final int BIG_BATCH_SIZE = 50;
  private static final int SMALL_BATCH_SIZE = 5;

  // a manually created map which shows the expected mapping behavior
  // between `com.weather.*` and `id.doc.forecast.*` domain model objects
  private static final Map<Region, com.weather.Region> WEATHER_REGION = new HashMap<>() {{
    put(Region.BIRMINGHAM, com.weather.Region.BIRMINGHAM);
    put(Region.EDINBURGH, com.weather.Region.EDINBURGH);
    put(Region.GLASGOW, com.weather.Region.GLASGOW);
    put(Region.LONDON, com.weather.Region.LONDON);
    put(Region.MANCHESTER, com.weather.Region.MANCHESTER);
    put(Region.NORTH_ENGLAND, com.weather.Region.NORTH_ENGLAND);
    put(Region.SOUTH_WEST_ENGLAND, com.weather.Region.SOUTH_WEST_ENGLAND);
    put(Region.SOUTH_EAST_ENGLAND, com.weather.Region.SOUTH_EAST_ENGLAND);
    put(Region.WALES, com.weather.Region.WALES);
  }};
  private static final Map<Day, com.weather.Day> WEATHER_DAY = new HashMap<>() {{
    put(Day.MONDAY, com.weather.Day.MONDAY);
    put(Day.TUESDAY, com.weather.Day.TUESDAY);
    put(Day.WEDNESDAY, com.weather.Day.WEDNESDAY);
    put(Day.THURSDAY, com.weather.Day.THURSDAY);
    put(Day.FRIDAY, com.weather.Day.FRIDAY);
    put(Day.SATURDAY, com.weather.Day.SATURDAY);
    put(Day.SUNDAY, com.weather.Day.SUNDAY);
  }};

  // helper constants
  private static final Matcher<com.weather.Day> ANY_WEATHER_DAY = any(com.weather.Day.class);
  private static final Matcher<com.weather.Region> ANY_WEATHER_REGION = any(
      com.weather.Region.class);

  // objects to help with mocking
  @RegisterExtension
  public JUnit5Mockery context = new JUnit5Mockery() {{
    setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
  }};
  com.weather.Forecaster forecaster = context.mock(com.weather.Forecaster.class);
  Forecaster adapted = adapt(forecaster);

  /**
   * The domain models in {@code com.weather.*} and {@code ic.doc.forecast.*} may grow, shrink,
   * change, diverge, etc. However, some tests rely on the exact mappings between these domain
   * models. So if the manual mappings between these domain models ever becomes <i>stale</i>, so
   * will the tests that rely on it.
   * <p>
   * This will check that the manual mappings between these domain models has not become stale.
   */
  @BeforeClass
  public static void checkThatDomainModelMappingsAreNotStale() {
    // check region mapping
    for (Region region : REGIONS) {
      assertTrue(WEATHER_REGION.containsKey(region));
    }

    // check day mapping
    for (Day day : DAYS) {
      assertTrue(WEATHER_DAY.containsKey(day));
    }
  }

  /**
   *
   * @param forecaster
   * @param inspector
   * @return
   */
  private static com.weather.Forecaster inspectWeatherForecaster(com.weather.Forecaster forecaster,
      BiConsumer<Pair<com.weather.Region, com.weather.Day>, com.weather.Forecast> inspector) {
    return inspect(forecaster, (m, a, r) -> {
      try {
        // ignore methods we are not interested in inspecting
        if (!m.equals(
            com.weather.Forecaster.class.getMethod("forecastFor", com.weather.Region.class,
                com.weather.Day.class))) {
          return;
        }

        // type-check the arguments and returned value at runtime
        if (a.length != 2 || a[0].getClass() != com.weather.Region.class
            || a[1].getClass() != com.weather.Day.class || r.isEmpty()
            || r.get().getClass() != com.weather.Forecast.class) {
          throw new IllegalArgumentException("Invalid 'forecastFor' method interceptor detected");
        }

        // coerce the types and call supplied inspector
        inspector.accept(Pair.of((com.weather.Region) a[0], (com.weather.Day) a[1]),
            (com.weather.Forecast) r.get());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void adaptsRegionsCorrectly() {
    Sequence regions = context.sequence("Correctly adapts the regions, in order");

    // expect a sequence of calls to underlying object, with correctly translated `Region`s
    context.checking(new Expectations() {{
      for (Region region : REGIONS) {
        oneOf(forecaster).forecastFor(with(equal(WEATHER_REGION.get(region))),
            with(ANY_WEATHER_DAY));
        inSequence(regions);
      }
    }});

    for (Region region : REGIONS) {
      adapted.forecastFor(region, randomDay());
    }
  }

  @Test
  public void adaptsDaysCorrectly() {
    Sequence days = context.sequence("Correctly adapts the days, in order");

    // expect a sequence of calls to underlying object, with correctly translated `Day`s
    context.checking(new Expectations() {{
      for (Day day : DAYS) {
        oneOf(forecaster).forecastFor(with(ANY_WEATHER_REGION), with(equal(WEATHER_DAY.get(day))));
        inSequence(days);
      }
    }});

    for (Day day : DAYS) {
      adapted.forecastFor(randomRegion(), day);
    }
  }

  @Test
  public void adaptsReportsCorrectly() {
    Sequence reports = context.sequence("Correctly adapts the reports, in order");

    // create a batch of forecast contents
    List<String> summaries = randomSummaries(BIG_BATCH_SIZE);
    List<Integer> temperatures = randomTemperatures(BIG_BATCH_SIZE);

    // make the underlying mock object to produce `Reports` which need to be adapted
    context.checking(new Expectations() {{
      for (int i = 0; i < BIG_BATCH_SIZE; i++) {
        oneOf(forecaster).forecastFor(with(ANY_WEATHER_REGION), with(ANY_WEATHER_DAY));
        inSequence(reports);
        will(returnValue(new com.weather.Forecast(summaries.get(i), temperatures.get(i))));
      }
    }});

    // produce a list of adapted forecasts, and check that their internal contents are correct
    for (int i = 0; i < BIG_BATCH_SIZE; i++) {
      Forecast forecast = adapted.forecastFor(randomRegion(), randomDay());
      assertEquals(forecast.summary(), summaries.get(i));
      assertEquals((Integer) forecast.temperature(), temperatures.get(i));
    }
  }

  @Test
  public void correctlyIntegratesWithActualWeatherForecaster() {
    // create an adapted inspector, where the `forecastFor` method call to the inner object
    // is inspected at runtime, and logged to a list
    List<Triple<com.weather.Region, com.weather.Day, com.weather.Forecast>> weatherForecasterLog =
        new ArrayList<>();
    Forecaster inspectedAndAdapted = adapt(
        inspectWeatherForecaster(new com.weather.Forecaster(), (args, forecast) -> {
          weatherForecasterLog.add(Triple.of(args.first(), args.second(), forecast));
        }));

    // create a list of forecast queries, and the list of results they produced
    List<Region> regions = randomRegions(SMALL_BATCH_SIZE);
    List<Day> days = randomDays(SMALL_BATCH_SIZE);
    List<Forecast> forecasts = makeItemList(SMALL_BATCH_SIZE,
        i -> inspectedAndAdapted.forecastFor(regions.get(i), days.get(i)));

    // match up the contests of both the list of queries/results, and the list of logs
    for (int i = 0; i < SMALL_BATCH_SIZE; i++) {
      Triple<com.weather.Region, com.weather.Day, com.weather.Forecast> log =
          weatherForecasterLog.get(i);
      Region region = regions.get(i);
      Day day = days.get(i);
      Forecast forecast = forecasts.get(i);

      assertEquals(WEATHER_REGION.get(region), log.first());
      assertEquals(WEATHER_DAY.get(day), log.second());
      assertEquals(forecast.summary(), log.third().summary());
      assertEquals(forecast.temperature(), log.third().temperature());
    }
  }
}