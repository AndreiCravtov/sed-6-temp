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
 *
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
   *
   * @param forecaster
   * @param inspector
   * @return
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
   *
   * @param forecaster
   * @param inspector
   * @return
   */
  static Forecaster inspectForecaster(Forecaster forecaster,
      BiConsumer<Pair<Region, Day>, Forecast> inspector) {
    return inspect(forecaster, (m, a, r) -> {
      try {
        // ignore methods we are not interested in inspecting
        if (!m.equals(forecaster.getClass()
            .getMethod("forecastFor", Region.class, Day.class))) {
          return;
        }

        // type-check the arguments and returned value at runtime
        if (a.length != 2 || a[0].getClass() != Region.class
            || a[1].getClass() != Day.class || r.isEmpty()
            || r.get().getClass() != Forecast.class) {
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
   *
   * @return
   */
  static Region randomRegion() {
    return chooseRandomItem(REGIONS);
  }

  /**
   *
   * @param amount
   * @return
   */
  static List<Region> randomRegions(int amount) {
    return makeItemList(amount, i -> randomRegion());
  }

  /**
   *
   * @return
   */
  static Day randomDay() {
    return chooseRandomItem(DAYS);
  }

  /**
   *
   * @param amount
   * @return
   */
  static List<Day> randomDays(int amount) {
    return makeItemList(amount, i -> randomDay());
  }

  /**
   *
   * @return
   */
  static String randomSummary() {
    return RandomString.make(new Random().nextInt(MIN_STRING_LENGTH, MAX_STRING_LENGTH));
  }

  /**
   *
   * @param amount
   * @return
   */
  static List<String> randomSummaries(int amount) {
    return makeItemList(amount, i -> randomSummary());
  }

  /**
   *
   * @return
   */
  static int randomTemperature() {
    return new Random().nextInt(MIN_TEMPERATURE, MAX_TEMPERATURE);
  }

  /**
   *
   * @param amount
   * @return
   */
  static List<Integer> randomTemperatures(int amount) {
    return makeItemList(amount, i -> randomTemperature());
  }
}
