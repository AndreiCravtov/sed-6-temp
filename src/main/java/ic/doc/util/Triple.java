package ic.doc.util;

public record Triple<T, U, F>(T first, U second, F third) {
  public static <T, U, F> Triple<T, U, F> of(T first, U second, F third) {
    return new Triple<>(first, second, third);
  }
}
