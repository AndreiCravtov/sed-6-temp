package ic.doc.forecast;

public class CacheDecorator implements Forecaster {

  private final Forecaster forecaster;

  public CacheDecorator(Forecaster forecaster) {
    this.forecaster = forecaster;
  }

  public static Forecaster decorate(Forecaster forecaster) {
    return new CacheDecorator(forecaster);
  }

  @Override
  public Forecast forecastFor(Region region, Day day) {
    return forecaster.forecastFor(region, day);
  }
}
