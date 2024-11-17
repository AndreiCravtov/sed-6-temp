package ic.doc.forecast;

import ic.doc.util.Pair;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * A proxy around {@link Forecaster} objects which caches responses for an hour, after which they
 * will be evicted from the cache. An optional cache size limit can be supplied.
 */
public final class CachingForecasterProxy implements Forecaster {

  private static final int NO_MAX_CACHE_SIZE = 0;
  private static final Duration ONE_HOUR = Duration.ofHours(1);

  private final Forecaster forecaster;
  private final int maxCacheSize;
  private final HashMap<Pair<Region, Day>, Pair<Forecast, Instant>> cache;
  private final Deque<Pair<Pair<Region, Day>, Instant>> cacheEvictionQueue = new LinkedList<>();
  private final InstantSource instantSource;

  /**
   * Constructs a caching {@link Forecaster} proxy with a cache of limited size. Old entries are
   * evicted if the cache size reaches that limit.
   *
   * @param forecaster    the {@link Forecaster} object being proxied
   * @param maxCacheSize  the maximum cache size. Must be greater than zero
   * @param instantSource optional {@link InstantSource} object to use for time-keeping operations
   * @throws NullPointerException     if {@code forecaster} is null
   * @throws IllegalArgumentException if {@code maxCacheSize} is less than zero
   */
  public CachingForecasterProxy(Forecaster forecaster, int maxCacheSize,
      InstantSource instantSource) {
    if (forecaster == null) {
      throw new NullPointerException("Forecaster cannot be null");
    }
    if (maxCacheSize <= NO_MAX_CACHE_SIZE) {
      throw new IllegalArgumentException("Cache size must be greater than zero");
    }

    this.forecaster = forecaster;
    this.maxCacheSize = maxCacheSize;
    this.cache = new HashMap<>();
    this.instantSource = instantSource == null ? InstantSource.system() : instantSource;
  }

  /**
   * Constructs a caching {@link Forecaster} proxy with a cache unlimited size. No old entry will
   * ever get evicted, so all repeated requests always hit the cache.
   *
   * @param forecaster    the {@link Forecaster} object being proxied
   * @param instantSource optional {@link InstantSource} object to use for time-keeping operations
   * @throws NullPointerException if {@code forecaster} is null
   */
  public CachingForecasterProxy(Forecaster forecaster, InstantSource instantSource) {
    if (forecaster == null) {
      throw new NullPointerException("Forecaster cannot be null");
    }

    this.forecaster = forecaster;
    this.maxCacheSize = NO_MAX_CACHE_SIZE;
    this.cache = new HashMap<>();
    this.instantSource = instantSource == null ? InstantSource.system() : instantSource;
  }

  /**
   * Creates a caching {@link Forecaster} proxy with a cache of limited size. Old entries are
   * evicted if the cache size reaches that limit.
   *
   * @param forecaster    the {@link Forecaster} object being proxied
   * @param maxCacheSize  the maximum cache size. Must be greater than zero
   * @param instantSource optional {@link InstantSource} object to use for time-keeping operations
   * @throws NullPointerException     if {@code forecaster} is null
   * @throws IllegalArgumentException if {@code maxCacheSize} is less than zero
   */
  public static Forecaster withLimitedCache(Forecaster forecaster, int maxCacheSize,
      InstantSource instantSource) throws NullPointerException, IllegalArgumentException {
    return new CachingForecasterProxy(forecaster, maxCacheSize, instantSource);
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
    return new CachingForecasterProxy(forecaster, maxCacheSize, null);
  }

  /**
   * Creates a caching {@link Forecaster} proxy with a cache unlimited size. No old entry will ever
   * get evicted, so all repeated requests always hit the cache.
   *
   * @param forecaster    the {@link Forecaster} object being proxied
   * @param instantSource optional {@link InstantSource} object to use for time-keeping operations
   * @throws NullPointerException if {@code forecaster} is null
   */
  public static Forecaster withUnlimitedCache(Forecaster forecaster, InstantSource instantSource)
      throws NullPointerException {
    return new CachingForecasterProxy(forecaster, instantSource);
  }

  /**
   * Creates a caching {@link Forecaster} proxy with a cache unlimited size. No old entry will ever
   * get evicted, so all repeated requests always hit the cache.
   *
   * @param forecaster the {@link Forecaster} object being proxied
   * @throws NullPointerException if {@code forecaster} is null
   */
  public static Forecaster withUnlimitedCache(Forecaster forecaster) throws NullPointerException {
    return new CachingForecasterProxy(forecaster, null);
  }

  @Override
  public Forecast forecastFor(Region region, Day day) {
    if (region == null) {
      throw new NullPointerException("region cannot be null");
    }
    if (day == null) {
      throw new NullPointerException("day cannot be null");
    }

    // check if query is in cache
    Forecast forecast = hitCache(region, day);

    // if it is a cache hit, return early
    if (forecast != null) {
      return forecast;
    }

    // if it is a cache miss, call the internal forecast service,
    // and record the time at which it completes
    forecast = forecaster.forecastFor(region, day);
    Instant timestamp = instantSource.instant();

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
   * Checks the cache for a {@link Forecast} associated with a given {@link Region} and {@link Day},
   * and returns it if found and not older than an hour. Otherwise, returns null.
   *
   * @param region the given {@link Region}
   * @param day    the given {@link Day}
   * @return the cached {@link Forecast}, or null if not found or older than one hour
   */
  private Forecast hitCache(Region region, Day day) {
    Pair<Region, Day> forecastQuery = Pair.of(region, day);
    Pair<Forecast, Instant> forecastEntry = cache.get(forecastQuery);

    // if it is a cache miss, return early
    if (forecastEntry == null) {
      return null;
    }

    // if the entry is old, then trigger cleanup and return early
    Instant now = instantSource.instant();
    if (forecastEntry.second().plus(ONE_HOUR).isBefore(now)) {
      while (!cacheEvictionQueue.isEmpty() && cacheEvictionQueue.peekFirst().second().plus(ONE_HOUR)
          .isBefore(now)) {
        cacheEvictionQueue.removeFirst();
      }
      return null;
    }

    // the cached entry is still fresh, so return it
    return forecastEntry.first();
  }
}
