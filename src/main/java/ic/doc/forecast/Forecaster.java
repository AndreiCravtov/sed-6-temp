package ic.doc.forecast;

/**
 *
 */
public interface Forecaster {

  /**
   *
   * @param region
   * @param day
   * @return
   */
  Forecast forecastFor(Region region, Day day);
}