import org.demo.core.examples.ex4.NioTcpServerMain;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProxyIntegrationTest {
    private final static Logger logger = Logger.getLogger(ProxyIntegrationTest.class.getName());

    @Test
    public void testProxy() throws IOException {
        String expectedUrl = "http://repo.she.pwj.com:8080/";
        String actualUrl = "http://repo.she.pwj.com:8080/";

        //1. Create request to retrieve test webpage (github.com)
        String expectedWebPage = getPageByUrl(expectedUrl);

        //2. start proxy server with proper configuration
        new NioTcpServerMain().main(new String[]{});

        //3. send Http request to localhost:port
        String actualWebPage = getPageByUrl(actualUrl);

        //4. validate expected response
        Assert.assertEquals(expectedWebPage, actualWebPage);


    }
    private static String getPageByUrl(String url) throws IOException {
        String webPage=null;
        try {
            URL u = new URL(url);
            HttpURLConnection uc = (HttpURLConnection) u.openConnection();

            try (InputStream raw = uc.getInputStream()) {
                webPage=downloadPage(raw);
            } catch (IOException ex) {
                throw ex;
            }
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, url + " is not a parseable URL", ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            throw ex;
        }
        return webPage;
    }

    private static String downloadPage(InputStream raw) throws IOException {
        try (InputStream buffer = new BufferedInputStream(raw)) {
            Reader reader = new InputStreamReader(buffer);
            int c;
            StringBuilder webPageBuilder = new StringBuilder();
            while ((c = reader.read()) != -1) {
                webPageBuilder.append((char) c);
            }
            return webPageBuilder.toString();
        }
    }
}
