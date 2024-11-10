package ic.doc.util;

import java.util.Objects;

/**
 *
 * @param <T>
 * @param <U>
 * @param <F>
 */
@FunctionalInterface
public interface TriConsumer<T, U, F> {

  void accept(T var1, U var2, F var3);

  default TriConsumer<T, U, F> andThen(TriConsumer<? super T, ? super U, ? super F> after) {
    Objects.requireNonNull(after);
    return (t, u, f) -> {
      this.accept(t, u, f);
      after.accept(t, u, f);
    };
  }
}
