package ic.doc.forecast;

import static ic.doc.TestUtils.chooseRandomItem;
import static ic.doc.TestUtils.inspect;
import static ic.doc.TestUtils.makeItemList;

import ic.doc.util.Pair;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.bytebuddy.utility.RandomString;

/**
 * A set of static testing utility methods specifically for the weather forecast tests.
 */
public class ForecastTestUtils {

  // exposed helper constants
  static final Region[] REGIONS = Region.values();
  static final Day[] DAYS = Day.values();

  // constants, to eliminate magic numbers
  private static final int MIN_STRING_LENGTH = 8;
  private static final int MAX_STRING_LENGTH = 64;
  private static final int MIN_TEMPERATURE = -40;
  private static final int MAX_TEMPERATURE = 60;

  // exposed helper methods

  /**
   * Creates a proxy object for the target {@link com.weather.Forecaster} object, where
   * {@code forecastFor} method calls are is delegated to the target object; the arguments and
   * return value are collected and passed to the inspector lambda.
   *
   * @param forecaster the {@link com.weather.Forecaster} object to proxy
   * @param inspector  the lambda which is called whenever the {@code forecastFor} method is invoked
   *                   on the inspected target object
   * @return the inspected target object
   */
  static com.weather.Forecaster inspectWeatherForecaster(com.weather.Forecaster forecaster,
      BiConsumer<Pair<com.weather.Region, com.weather.Day>, com.weather.Forecast> inspector) {
    return inspect(forecaster, (m, a, r) -> {
      try {
        // ignore methods we are not interested in inspecting
        if (!m.equals(forecaster.getClass()
            .getMethod("forecastFor", com.weather.Region.class, com.weather.Day.class))) {
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

  /**
   * Creates a proxy object for the target {@link Forecaster} object, where {@code forecastFor}
   * method calls are is delegated to the target object; the arguments and return value are
   * collected and passed to the inspector lambda.
   *
   * @param forecaster the {@link Forecaster} object to proxy
   * @param inspector  the lambda which is called whenever the {@code forecastFor} method is invoked
   *                   on the inspected target object
   * @return the inspected target object
   */
  static Forecaster inspectForecaster(Forecaster forecaster,
      BiConsumer<Pair<Region, Day>, Forecast> inspector) {
    return inspect(forecaster, (m, a, r) -> {
      try {
        // ignore methods we are not interested in inspecting
        if (!m.equals(forecaster.getClass().getMethod("forecastFor", Region.class, Day.class))) {
          return;
        }

        // type-check the arguments and returned value at runtime
        if (a.length != 2 || a[0].getClass() != Region.class || a[1].getClass() != Day.class
            || r.isEmpty() || r.get().getClass() != Forecast.class) {
          throw new IllegalArgumentException("Invalid 'forecastFor' method interceptor detected");
        }

        // coerce the types and call supplied inspector
        inspector.accept(Pair.of((Region) a[0], (Day) a[1]), (Forecast) r.get());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Picks a random {@link Region} object.
   *
   * @return the randomly selected {@link Region} object
   */
  static Region randomRegion() {
    return chooseRandomItem(REGIONS);
  }

  /**
   * Makes a list of random {@link Region} objects, of  the supplied list size.
   *
   * @param amount the size of the list
   * @return the list of random {@link Region} objects
   */
  static List<Region> randomRegions(int amount) {
    return makeItemList(amount, i -> randomRegion());
  }

  /**
   * Picks a random {@link Day} object.
   *
   * @return the randomly selected {@link Day} object
   */
  static Day randomDay() {
    return chooseRandomItem(DAYS);
  }

  /**
   * Makes a list of random {@link Day} objects, of  the supplied list size.
   *
   * @param amount the size of the list
   * @return the list of random {@link Day} objects
   */
  static List<Day> randomDays(int amount) {
    return makeItemList(amount, i -> randomDay());
  }

  /**
   * Produces a random string which can serve as a weather forecast summary.
   *
   * @return the random string
   */
  static String randomSummary() {
    return RandomString.make(new Random().nextInt(MIN_STRING_LENGTH, MAX_STRING_LENGTH));
  }

  /**
   * Makes a list of random weather forecast summaries, of  the supplied list size.
   *
   * @param amount the size of the list
   * @return the list of random weather forecast summaries
   */
  static List<String> randomSummaries(int amount) {
    return makeItemList(amount, i -> randomSummary());
  }

  /**
   * Produces a random integer which can serve as random temperature.
   *
   * @return the random integer
   */
  static int randomTemperature() {
    return new Random().nextInt(MIN_TEMPERATURE, MAX_TEMPERATURE);
  }

  /**
   * Makes a list of random temperatures, of  the supplied list size.
   *
   * @param amount the size of the list
   * @return the list of random temperatures
   */
  static List<Integer> randomTemperatures(int amount) {
    return makeItemList(amount, i -> randomTemperature());
  }
}
