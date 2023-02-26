import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HttpStatusTests {
    private final int WAIT_FOR_ELEMENT_TIMEOUT = 30;
    private ChromeDriver driver;
    private WebDriverWait webDriverWait;

    @BeforeAll
    public static void setUpClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_FOR_ELEMENT_TIMEOUT));
    }

    @Test
    public void simpleRequest_httpClient() throws URISyntaxException, IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://www.lambdatest.com/selenium-playground"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> reponse = client.send(request, HttpResponse.BodyHandlers.discarding());

        int statusCode = reponse.statusCode();
        System.out.println("Status Code: " + statusCode);
    }

    @Test
    public void simpleRequest_httpClientTimeouts() throws IOException {
        // Configure Timeouts Using the New 4.3. Builder
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(30))
                .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                .setResponseTimeout(Timeout.ofSeconds(30)).build();
        try (var client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
            var response = client.execute(new HttpHead("https://www.lambdatest.com/selenium-playground"));
            System.out.println("Status Code: " + response.getCode());
        }
    }

    @Test
    public void checkSeleniumPlayground_for_brokenLinks() throws IOException, URISyntaxException {
        int validLinks = 0;
        int brokenLinks = 0;
        driver.get("https://www.lambdatest.com/selenium-playground");
        List<WebElement> links = driver.findElements(By.tagName("a"));
        List<String> links1 = driver.findElements(By.tagName("a")).stream().map(a -> a.getAttribute("href")).collect(Collectors.toList());

        var client = HttpClient.newHttpClient();
        for(WebElement link : links) {
            String url = link.getAttribute("href");
            var request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            try {
                var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) {
                    System.out.println("Connection successfully to URL : " + url);
                    validLinks++;
                } else {
                    System.out.println("Connection failed to URL : " + url + " with response code : " + response.statusCode());
                    brokenLinks++;
                }
            } catch (IOException e) {
                System.out.println("Connection failed to URL : " + url + " with error : " + e.getMessage());
                brokenLinks++;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Detection of broken links completed with " + brokenLinks + " broken links and " + validLinks + " valid links\n");
        }
    }

    @Test
    public void checkSeleniumPlayground_for_brokenLinks_optimized() throws IOException {
        driver.navigate().to("https://www.lambdatest.com/selenium-playground/");
        driver.manage().window().maximize();
        List<WebElement> links = driver.findElements(By.tagName("a"));

        int validLinks = 0;
        int brokenLinks = 0;

        HttpClient client = HttpClient.newHttpClient();

        for (WebElement link : links) {
            String url = link.getAttribute("href");
            System.out.println(url);
            boolean isLinkedInPage = false;

            if ((url == null) || (url.isEmpty())) {
                System.out.println("URL is either not configured for anchor tag or it is empty");
                continue;
            }

            if ((url.startsWith("mailto")) || (url.startsWith("tel"))) {
                System.out.println("Email address or Telephone detected");
                continue;
            }

            if (url.startsWith("https://www.linkedin.com")) {
                System.out.println("URL starts with LinkedIn, expected status code is 999");
                isLinkedInPage = true;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            try {
                var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                int responseCode = response.statusCode();

                if (responseCode >= 400) {
                    if (isLinkedInPage && responseCode == 999) {
                        System.out.println(url + " is a LinkedIn Page and is not a broken link");
                        validLinks++;
                    } else {
                        System.out.println(url + " is a broken link");
                        brokenLinks++;
                    }
                } else {
                    System.out.println(url + " is a valid link");
                    validLinks++;
                }

            } catch (IOException e) {
                System.out.println("Connection failed to URL : " + url + " with error : " + e.getMessage());
                brokenLinks++;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("Detection of broken links completed with " + brokenLinks + " broken links and " + validLinks + " valid links\n");
    }

    @Test
    public void checkSeleniumPlaygroundWebsite_for_brokenLinks_using_sitemap() throws IOException, InterruptedException, ParserConfigurationException, SAXException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.lambdatest.com/sitemap.xml"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse sitemap and extract URLs
        var factory = DocumentBuilderFactory.newInstance();
        var builder = factory.newDocumentBuilder();
        var doc = builder.parse(new InputSource(new StringReader(response.body())));
        NodeList urlNodes = doc.getElementsByTagName("url");
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < urlNodes.getLength(); i++) {
            Node urlNode = urlNodes.item(i);
            if (urlNode.getNodeType() == Node.ELEMENT_NODE) {
                Element urlElement = (Element) urlNode;
                urls.add(urlElement.getElementsByTagName("loc").item(0).getTextContent());
            }
        }

        // Check broken links for each URL in parallel using ExecutorService
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        AtomicInteger validLinksCount = new AtomicInteger(0);
        AtomicInteger brokenLinksCount = new AtomicInteger(0);
        urls.forEach(url -> {
            executorService.submit(() -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("HEAD");
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        System.out.println(url + " is a valid link");
                        validLinksCount.getAndIncrement();
                    } else {
                        System.out.println(url + " is a broken link with response code: " + responseCode);
                        brokenLinksCount.getAndIncrement();
                    }
                } catch (IOException e) {
                    System.out.println("Exception while checking " + url + ": " + e.getMessage());
                }
            });
        });

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);

        System.out.println("Detection of broken links completed with " + brokenLinksCount.get() + " broken links and " + validLinksCount.get() + " valid links\n");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}