package ic.doc.forecast;

/**
 * An interface representing the capacity to provide weather forecasts for a given region and day.
 */
public interface Forecaster {

  /**
   * Provides a forecast for a given region and day.
   *
   * @param region the region
   * @param day    the day
   * @return the forecast for that {@code region} and {@code day}
   */
  Forecast forecastFor(Region region, Day day);
}