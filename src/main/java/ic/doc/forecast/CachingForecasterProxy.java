package ic.doc.forecast;

import java.util.HashMap;

public class CachingForecasterProxy implements Forecaster {

  private static final int NO_MAX_CACHE_SIZE = 0;

  private final Forecaster forecaster;
  private final int maxCacheSize;
  private final HashMap<ForecastQuery, Forecast> cache;

  /**
   * Constructs a caching {@link Forecaster} proxy with a cache of limited size. Old entries are
   * evicted if the cache size reaches that limit.
   *
   * @param forecaster   the {@link Forecaster} object being proxied
   * @param maxCacheSize the maximum cache size. Must be greater than zero
   * @throws NullPointerException     if {@code forecaster} is null
   * @throws IllegalArgumentException if {@code maxCacheSize} is less than zero
   */
  public CachingForecasterProxy(Forecaster forecaster, int maxCacheSize) {

    if (forecaster == null) {
      throw new NullPointerException("Forecaster cannot be null");
    }

    if (maxCacheSize <= NO_MAX_CACHE_SIZE) {
      throw new IllegalArgumentException("Cache size must be greater than zero");
    }

    this.forecaster = forecaster;
    this.maxCacheSize = maxCacheSize;
    this.cache = new HashMap<>();
  }

  /**
   * Constructs a caching {@link Forecaster} proxy with a cache unlimited size. No old entry will
   * ever get evicted, so all repeated requests always hit the cache.
   *
   * @param forecaster the {@link Forecaster} object being proxied
   * @throws NullPointerException if {@code forecaster} is null
   */
  public CachingForecasterProxy(Forecaster forecaster) {

    if (forecaster == null) {
      throw new NullPointerException("Forecaster cannot be null");
    }

    this.forecaster = forecaster;
    this.maxCacheSize = NO_MAX_CACHE_SIZE;
    this.cache = new HashMap<>();
  }

  /**
   * Creates a caching {@link Forecaster} proxy with a cache of limited size. Old entries are
   * evicted if the cache size reaches that limit.
   *
   * @param forecaster   the {@link Forecaster} object being proxied
   * @param maxCacheSize the maximum cache size. Must be greater than zero
   * @throws NullPointerException     if {@code forecaster} is null
   * @throws IllegalArgumentException if {@code maxCacheSize} is less than zero
   */
  public static Forecaster withLimitedCache(Forecaster forecaster, int maxCacheSize)
      throws NullPointerException, IllegalArgumentException {
    return new CachingForecasterProxy(forecaster, maxCacheSize);
  }

  /**
   * Creates a caching {@link Forecaster} proxy with a cache unlimited size. No old entry will ever
   * get evicted, so all repeated requests always hit the cache.
   *
   * @param forecaster the {@link Forecaster} object being proxied
   * @throws NullPointerException if {@code forecaster} is null
   */
  public static Forecaster withUnlimitedCache(Forecaster forecaster) throws NullPointerException {
    return new CachingForecasterProxy(forecaster);
  }

  @Override
  public Forecast forecastFor(Region region, Day day) {
    // check if query is in cache
    ForecastQuery forecastQuery = new ForecastQuery(region, day);
    Forecast forecast = cache.get(forecastQuery);

    // if it is a cache hit, return early
    if (forecast != null) {
      return forecast;
    }

    // if it is a cache miss, call the internal forecast service
    forecast = forecaster.forecastFor(region, day);

    // if the cache size is limited, and that limit has already been reached,
    // evict an old entry to make space for the new one
    if (maxCacheSize != NO_MAX_CACHE_SIZE && cache.size() == maxCacheSize) {
      evictOldEntry();
    }

    // add the new entry to the cache and return the result
    cache.put(forecastQuery, forecast);
    return forecast;
  }

  private void evictOldEntry() {
    // this will do the job for now, but is incredibly crude
    ForecastQuery oldQuery = cache.keySet().stream().toList().getFirst();
    Forecast oldResponse = cache.remove(oldQuery);
  }

  private record ForecastQuery(Region region, Day day) {

  }
}
