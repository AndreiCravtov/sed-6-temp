package ic.doc;

import ic.doc.util.TriConsumer;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.objenesis.ObjenesisStd;

/**
 * A set of static testing utility methods.
 */
public class TestUtils {

  /**
   * Chooses a random item from an array of items.
   *
   * @param items the array of items
   * @param <T>   the type of items
   * @return the randomly chosen item
   */
  public static <T> T chooseRandomItem(T[] items) {
    return items[new Random().nextInt(items.length)];
  }

  /**
   * Makes a list of items of some size, with each element being constructed based on its index with
   * the supplied factory lambda.
   *
   * @param size    the size of the list
   * @param factory the item factory lambda
   * @param <T>     the type of items
   * @return the list of items
   */
  public static <T> List<T> makeItemList(int size, Function<Integer, T> factory) {
    return IntStream.range(0, size).mapToObj(factory::apply).toList();
  }

  /**
   * Creates a proxy object for the target object, where each method call is delegated to the target
   * object; the method, arguments, and return value are collected and passed to the inspector
   * lambda. If the return value of a method was null, the empty {@link Optional} variant is passed
   * to the inspector lambda, otherwise the non-empty {@link Optional} variant is passed.
   *
   * @param target    the object to proxy
   * @param inspector the lambda which is called whenever a method is invoked on the inspected
   *                  target object
   * @param <T>       the type of the target object
   * @return the inspected target object
   */
  @SuppressWarnings("unchecked")
  public static <T> T inspect(final T target,
      TriConsumer<Method, Object[], Optional<Object>> inspector) {
    // perform null check on object
    if (target == null) {
      throw new NullPointerException("Target object cannot be null");
    }

    // make inspected proxy type, and instantiate without calling constructor
    try (DynamicType.Unloaded<?> inspectedProxy = new ByteBuddy().with(
            new NamingStrategy.PrefixingRandom("inspected")).subclass(target.getClass())
        .method(ElementMatchers.any())
        .intercept(InvocationHandlerAdapter.of((proxy, method, args) -> {
          Object returnValue = method.invoke(target, args);
          inspector.accept(method, args, Optional.ofNullable(returnValue));
          return returnValue;
        })).make()) {
      return (T) new ObjenesisStd().newInstance(
          inspectedProxy.load(target.getClass().getClassLoader()).getLoaded());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
