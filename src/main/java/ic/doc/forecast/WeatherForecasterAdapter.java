package ic.doc.forecast;

/**
 * Adapts {@link com.weather.Forecaster} objects to the {@link Forecaster object} interface.
 */
public final class WeatherForecasterAdapter implements Forecaster {

  private final com.weather.Forecaster weatherForecaster;

  public WeatherForecasterAdapter(com.weather.Forecaster weatherForecaster) {
    if (weatherForecaster == null) {
      throw new NullPointerException("weatherForecaster cannot be null");
    }

    this.weatherForecaster = weatherForecaster;
  }

  /**
   * Creates a {@link Forecaster} object from a given {@link com.weather.Forecaster} object.
   *
   * @param weatherForecaster the {@link com.weather.Forecaster} object
   * @return the {@link Forecaster} object
   * @throws NullPointerException if {@code weatherPointer} is null
   */
  public static Forecaster adapt(com.weather.Forecaster weatherForecaster) {
    return new WeatherForecasterAdapter(weatherForecaster);
  }

  @Override
  public Forecast forecastFor(Region region, Day day) {
    return adaptForecast(weatherForecaster.forecastFor(adaptRegion(region), adaptDay(day)));
  }

  /**
   * Converts a {@link Region} object to a {@link com.weather.Region} object
   *
   * @param region the {@link Region} object
   * @return the {@link com.weather.Region} object
   * @throws NullPointerException if {@code region} is null
   */
  private com.weather.Region adaptRegion(Region region) {
    if (region == null) {
      throw new NullPointerException("region cannot be null");
    }

    return switch (region) {
      case BIRMINGHAM -> com.weather.Region.BIRMINGHAM;
      case EDINBURGH -> com.weather.Region.EDINBURGH;
      case GLASGOW -> com.weather.Region.GLASGOW;
      case LONDON -> com.weather.Region.LONDON;
      case MANCHESTER -> com.weather.Region.MANCHESTER;
      case NORTH_ENGLAND -> com.weather.Region.NORTH_ENGLAND;
      case SOUTH_WEST_ENGLAND -> com.weather.Region.SOUTH_WEST_ENGLAND;
      case SOUTH_EAST_ENGLAND -> com.weather.Region.SOUTH_EAST_ENGLAND;
      case WALES -> com.weather.Region.WALES;
    };
  }

  /**
   * Converts a {@link Day} object to a {@link com.weather.Day} object
   *
   * @param day the {@link Day} object
   * @return the {@link com.weather.Day} object
   * @throws NullPointerException if {@code day} is null
   */
  private com.weather.Day adaptDay(Day day) {
    if (day == null) {
      throw new NullPointerException("day cannot be null");
    }

    return switch (day) {
      case MONDAY -> com.weather.Day.MONDAY;
      case TUESDAY -> com.weather.Day.TUESDAY;
      case WEDNESDAY -> com.weather.Day.WEDNESDAY;
      case THURSDAY -> com.weather.Day.THURSDAY;
      case FRIDAY -> com.weather.Day.FRIDAY;
      case SATURDAY -> com.weather.Day.SATURDAY;
      case SUNDAY -> com.weather.Day.SUNDAY;
    };
  }

  /**
   * Converts a {@link com.weather.Forecast} object to a {@link Forecast} object
   *
   * @param forecast the {@link com.weather.Forecast} object
   * @return the {@link Forecast} object
   * @throws NullPointerException if {@code forecast} is null
   */
  private Forecast adaptForecast(com.weather.Forecast forecast) {
    if (forecast == null) {
      throw new NullPointerException("forecast cannot be null");
    }

    return new Forecast(forecast.summary(), forecast.temperature());
  }
}
