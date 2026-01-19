import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javax.swing.event.HyperlinkEvent;
import java.net.URL;

public class TestMenuBarHyperlinkListener {
    @Test
    void testHyperlinkUpdateActivated() {
        MenuBarHyperlinkListener listener = new MenuBarHyperlinkListener();
        // Create a mock HyperlinkEvent
        try {
            URL url = new URL("https://example.com");
            HyperlinkEvent event = new HyperlinkEvent(this, HyperlinkEvent.EventType.ACTIVATED, url, "test");
            // Should not throw exception
            assertDoesNotThrow(() -> listener.hyperlinkUpdate(event));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testHyperlinkUpdateEntered() {
        MenuBarHyperlinkListener listener = new MenuBarHyperlinkListener();
        // Create a mock HyperlinkEvent for entered
        try {
            URL url = new URL("https://example.com");
            HyperlinkEvent event = new HyperlinkEvent(this, HyperlinkEvent.EventType.ENTERED, url, "test");
            // Should not do anything
            assertDoesNotThrow(() -> listener.hyperlinkUpdate(event));
        } catch (Exception e) {
            fail(e);
        }
    }
}