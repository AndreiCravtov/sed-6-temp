package ic.doc.forecast;

import ic.doc.util.Pair;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 *
 */
public class CachingForecasterProxy implements Forecaster {

  private static final int NO_MAX_CACHE_SIZE = 0;
  private static final Duration ONE_HOUR = Duration.ofHours(1);

  private final Forecaster forecaster;
  private final int maxCacheSize;
  private final HashMap<Pair<Region, Day>, Pair<Forecast, Instant>> cache;
  private final Deque<Pair<Pair<Region, Day>, Instant>> cacheEvictionQueue = new LinkedList<>();

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

  /**
   * @param region
   * @param day
   * @return
   */
  @Override
  public Forecast forecastFor(Region region, Day day) {
    // check if query is in cache
    Forecast forecast = hitCache(region, day);

    // if it is a cache hit, return early
    if (forecast != null) {
      return forecast;
    }

    // if it is a cache miss, call the internal forecast service,
    // and record the time at which it completes
    forecast = forecaster.forecastFor(region, day);
    Instant timestamp = Instant.now();

    // if the cache size is limited, and that limit has already been reached,
    // evict an old entry to make space for the new one
    if (maxCacheSize != NO_MAX_CACHE_SIZE && cache.size() == maxCacheSize) {
      cache.remove(cacheEvictionQueue.removeFirst());
    }

    // add the new entry to the cache and return the result
    cache.put(Pair.of(region, day), Pair.of(forecast, timestamp));
    cacheEvictionQueue.addLast(Pair.of(Pair.of(region, day), timestamp));
    return forecast;
  }

  /**
   * @param region
   * @param day
   * @return
   */
  private Forecast hitCache(Region region, Day day) {
    Pair<Region, Day> forecastQuery = Pair.of(region, day);
    Pair<Forecast, Instant> forecastEntry = cache.get(forecastQuery);

    // if it is a cache miss, return early
    if (forecastEntry == null) {
      return null;
    }

    // if the entry is old, then trigger cleanup and return early
    if (forecastEntry.second().plus(ONE_HOUR).isBefore(Instant.now())) {
      while (!cacheEvictionQueue.isEmpty() && cacheEvictionQueue.peekFirst()
          .second().plus(ONE_HOUR).isBefore(Instant.now())) {
        cacheEvictionQueue.removeFirst();
      }
      return null;
    }

    // the cached entry is still fresh, so return it
    return forecastEntry.first();
  }
}
