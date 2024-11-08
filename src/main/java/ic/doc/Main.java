package ic.doc;

import static ic.doc.forecast.WeatherForecasterAdapter.adapt;

import ic.doc.forecast.Day;
import ic.doc.forecast.Forecast;
import ic.doc.forecast.Forecaster;
import ic.doc.forecast.Region;

public class Main {

  public static void main(String[] args) {

    Forecaster forecaster = adapt(new com.weather.Forecaster());

    Forecast londonForecast = forecaster.forecastFor(Region.LONDON, Day.MONDAY);

    System.out.println("London outlook: " + londonForecast.summary());
    System.out.println("London temperature: " + londonForecast.temperature());

    Forecast edinburghForecast = forecaster.forecastFor(Region.EDINBURGH, Day.MONDAY);

    System.out.println("Edinburgh outlook: " + edinburghForecast.summary());
    System.out.println("Edinburgh temperature: " + edinburghForecast.temperature());
  }
}
