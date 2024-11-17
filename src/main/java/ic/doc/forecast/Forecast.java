package ic.doc.forecast;

/**
 * A record representing what a weather forecast is, containing a summary and a temperature.
 *
 * @param summary     the summary
 * @param temperature the temperature
 */
public record Forecast(String summary, int temperature) {

}
