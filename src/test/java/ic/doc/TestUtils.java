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
 *
 */
public class TestUtils {

  /**
   * @param items
   * @param <T>
   * @return
   */
  public static <T> T chooseRandomItem(T[] items) {
    return items[new Random().nextInt(items.length)];
  }

  /**
   *
   * @param size
   * @param factory
   * @return
   * @param <T>
   */
  public static <T> List<T> makeItemList(int size, Function<Integer, T> factory) {
    return IntStream.range(0, size).mapToObj(factory::apply).toList();
  }

  /**
   *
   * @param target
   * @param inspector
   * @return
   * @param <T>
   */
  @SuppressWarnings("unchecked")
  public static <T> T inspect(final T target,
      TriConsumer<Method, Object[], Optional<Object>> inspector) {
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
