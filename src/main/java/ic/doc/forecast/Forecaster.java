package ic.doc.forecast;

public interface Forecaster {
  Forecast forecastFor(Region region, Day day);
}