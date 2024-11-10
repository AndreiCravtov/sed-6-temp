package ic.doc.forecast;

/**
 *
 */
public final class WeatherForecasterAdapter implements Forecaster {

  private final com.weather.Forecaster weatherForecaster;

  public WeatherForecasterAdapter(com.weather.Forecaster weatherForecaster) {
    this.weatherForecaster = weatherForecaster;
  }

  /**
   *
   * @param weatherForecaster
   * @return
   */
  public static Forecaster adapt(com.weather.Forecaster weatherForecaster) {
    return new WeatherForecasterAdapter(weatherForecaster);
  }

  /**
   *
   * @param region
   * @param day
   * @return
   */
  @Override
  public Forecast forecastFor(Region region, Day day) {
    return adaptForecast(weatherForecaster.forecastFor(adaptRegion(region), adaptDay(day)));
  }

  /**
   *
   * @param region
   * @return
   */
  private com.weather.Region adaptRegion(Region region) {
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
   *
   * @param day
   * @return
   */
  private com.weather.Day adaptDay(Day day) {
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
   *
   * @param forecast
   * @return
   */
  private Forecast adaptForecast(com.weather.Forecast forecast) {
    return new Forecast(forecast.summary(), forecast.temperature());
  }
}
