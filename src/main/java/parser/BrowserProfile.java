package parser;

import java.util.List;
import java.util.Random;
import org.jsoup.Connection;

/**
 * An immutable browser identity profile that bundles a User-Agent string
 * with its matching {@code Sec-Ch-Ua} and {@code Sec-Ch-Ua-Platform}
 * headers. Rotating profiles across requests makes scraping traffic
 * look less uniform.
 */
public final class BrowserProfile {

  private static final List<BrowserProfile> PROFILES = List.of(
      new BrowserProfile(
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
              + "AppleWebKit/537.36 (KHTML, like Gecko) "
              + "Chrome/134.0.6998.166 Safari/537.36",
          "\"Chromium\";v=\"134\", \"Google Chrome\";v=\"134\", "
              + "\"Not:A-Brand\";v=\"24\"",
          "\"Windows\""
      ),
      new BrowserProfile(
          "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
              + "AppleWebKit/537.36 (KHTML, like Gecko) "
              + "Chrome/133.0.6943.142 Safari/537.36",
          "\"Chromium\";v=\"133\", \"Google Chrome\";v=\"133\", "
              + "\"Not:A-Brand\";v=\"24\"",
          "\"macOS\""
      ),
      new BrowserProfile(
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) "
              + "Gecko/20100101 Firefox/135.0",
          "",
          ""
      ),
      new BrowserProfile(
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
              + "AppleWebKit/537.36 (KHTML, like Gecko) "
              + "Chrome/134.0.6998.166 Safari/537.36 Edg/134.0.3124.93",
          "\"Chromium\";v=\"134\", \"Microsoft Edge\";v=\"134\", "
              + "\"Not:A-Brand\";v=\"24\"",
          "\"Windows\""
      ),
      new BrowserProfile(
          "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7_4) "
              + "AppleWebKit/605.1.15 (KHTML, like Gecko) "
              + "Version/18.3 Safari/605.1.15",
          "",
          ""
      )
  );

  private final String userAgent;
  private final String secChUa;
  private final String secChUaPlatform;

  /**
   * Primary constructor.
   *
   * @param userAgent The User-Agent header value
   * @param secChUa The Sec-Ch-Ua header value (empty string if not applicable)
   * @param secChUaPlatform The Sec-Ch-Ua-Platform header value
   *     (empty string if not applicable)
   */
  BrowserProfile(
      final String userAgent,
      final String secChUa,
      final String secChUaPlatform
  ) {
    this.userAgent = userAgent;
    this.secChUa = secChUa;
    this.secChUaPlatform = secChUaPlatform;
  }

  /**
   * Applies this profile's headers to a Jsoup connection.
   *
   * @param connection The Jsoup connection to configure
   * @return The same connection with User-Agent and client-hint headers set
   */
  Connection applyTo(final Connection connection) {
    connection.userAgent(this.userAgent);
    if (!this.secChUa.isEmpty()) {
      connection.header("Sec-Ch-Ua", this.secChUa);
    }
    if (!this.secChUaPlatform.isEmpty()) {
      connection.header("Sec-Ch-Ua-Platform", this.secChUaPlatform);
    }
    return connection;
  }

  /**
   * Returns the User-Agent string of this profile.
   *
   * @return The User-Agent value
   */
  String userAgent() {
    return this.userAgent;
  }

  /**
   * Returns a randomly selected browser profile from the built-in list.
   *
   * @param random The random number generator to use for selection
   * @return A browser profile with realistic, matching headers
   */
  static BrowserProfile random() {
    return PROFILES.get(new Random().nextInt(PROFILES.size()));
  }
}
