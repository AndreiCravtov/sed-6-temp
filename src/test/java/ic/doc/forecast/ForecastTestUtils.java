package ic.doc.forecast;

import ic.doc.util.TriConsumer;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

import java.lang.reflect.Method;
import org.objenesis.ObjenesisStd;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.RandomString;

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
   * @param items
   * @return
   * @param <T>
   */
  static <T> T chooseRandomItem(T[] items) {
    return items[new Random().nextInt(items.length)];
  }

  /**
   *
   * @param size
   * @param factory
   * @return
   * @param <T>
   */
  static <T> List<T> makeItemList(int size, Function<Integer, T> factory) {
    return IntStream.range(0, size).mapToObj(factory::apply).toList();
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

  /**
   *
   * @param target
   * @param inspector
   * @return
   * @param <T>
   */
  @SuppressWarnings("unchecked")
  static <T> T inspect(final T target, TriConsumer<Method, Object[], Optional<Object>> inspector) {
    // perform null check on object
    if (target == null) {
      throw new NullPointerException("Target object cannot be null");
    }

    // make inspected proxy type, and instantiate without calling constructor
    try (DynamicType.Unloaded<?> inspectedProxy = new ByteBuddy()
        .with(new NamingStrategy.PrefixingRandom("inspected"))
        .subclass(target.getClass())
        .method(ElementMatchers.any()).intercept(InvocationHandlerAdapter.of(
            (proxy, method, args) -> {
              Object returnValue = method.invoke(target, args);
              Optional<Object> wrappedReturnValue = returnValue == null
                  ? Optional.empty()
                  : Optional.of(returnValue);
              inspector.accept(method, args, wrappedReturnValue);
              return returnValue;
            })).make()) {
      return (T) new ObjenesisStd().newInstance(inspectedProxy
          .load(target.getClass().getClassLoader())
          .getLoaded());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
