package parser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.jsoup.Connection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Unit tests for {@link BrowserProfile}.
 * Verifies header application and random profile selection.
 */
@Timeout(10)
final class BrowserProfileTest {

  @Test
  void appliesUserAgentToConnection() {
    final Connection connection = mock(Connection.class);
    when(connection.userAgent(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(connection);
    final BrowserProfile profile = new BrowserProfile(
        "TestAgent/1.0",
        "\"TestBrowser\";v=\"1\"",
        "\"TestOS\""
    );
    profile.applyTo(connection);
    verify(connection).userAgent("TestAgent/1.0");
  }

  @Test
  void appliesSecChUaHeaderToConnection() {
    final Connection connection = mock(Connection.class);
    when(connection.userAgent(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(connection);
    when(connection.header(
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString()
    )).thenReturn(connection);
    final BrowserProfile profile = new BrowserProfile(
        "TestAgent/1.0",
        "\"Chromium\";v=\"134\"",
        "\"Windows\""
    );
    profile.applyTo(connection);
    verify(connection).header("Sec-Ch-Ua", "\"Chromium\";v=\"134\"");
  }

  @Test
  void skipsEmptySecChUaHeaders() {
    final Connection connection = mock(Connection.class);
    when(connection.userAgent(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(connection);
    final BrowserProfile profile = new BrowserProfile(
        "Firefox/135.0", "", ""
    );
    profile.applyTo(connection);
    verify(connection).userAgent("Firefox/135.0");
    org.mockito.Mockito.verifyNoMoreInteractions(connection);
  }

  @Test
  void randomReturnsNonNullProfile() {
    assertThat(
        "cant return a non-null profile from random selection",
        BrowserProfile.random(),
        is(notNullValue())
    );
  }

  @Test
  void randomCanReturnDifferentProfiles() {
    final Set<String> agents = new HashSet<>();
    for (int idx = 0; idx < 100; idx++) {
      agents.add(BrowserProfile.random().userAgent());
    }
    assertThat(
        "cant produce more than one distinct profile over 100 draws",
        agents.size(),
        is(greaterThan(1))
    );
  }
}
