import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
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
import utilities.Wait;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public void simpleRequest() throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection)new URL("https://www.lambdatest.com/selenium-playground").openConnection();
        httpURLConnection.setRequestMethod("HEAD");
        httpURLConnection.connect();
        int responseCode = httpURLConnection.getResponseCode();

        System.out.println("Status Code: " + responseCode);
    }

    @Test
    public void simpleRequest_httpClient() throws IOException {
        CloseableHttpClient httpClient1 = HttpClients.createDefault();

        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpHead("https://www.lambdatest.com/selenium-playground"));

        System.out.println("Status Code: " + response.getCode());
    }

    @Test
    public void simpleRequest_httpClientTimeouts() throws IOException {
        // Configure Timeouts Using the New 4.3. Builder
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(30))
                .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                .setResponseTimeout(Timeout.ofSeconds(30)).build();
        try (var client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
            HttpResponse response = client.execute(new HttpHead("https://www.lambdatest.com/selenium-playground"));
            System.out.println("Status Code: " + response.getCode());
        }
    }

    @Test
    public void testHttpStatusCodes() throws IOException {
        int successfulConnection = 0;
        int failedConnection = 0;
        driver.get("https://www.lambdatest.com/selenium-playground");

        List<WebElement> formHref = driver.findElements(By.tagName("a"));

        for(int i = 0; i < formHref.size(); i++)
        {
            String url = formHref.get(i).getAttribute("href");
            HttpURLConnection httpURLConnection = (HttpURLConnection)new URL(url).openConnection();
            httpURLConnection.setRequestMethod("HEAD");
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();

            if(responseCode == 200) {
                System.out.println("Connection successfully to URL : " + url);
                successfulConnection++;
            }
            else {
                System.out.println("Connection failed to URL : " + url + " with response code : " + responseCode);
                failedConnection++;
            }
        }

        System.out.println("Number of Successful connections : " + successfulConnection);
        System.out.println("Number of Failed connections : " + failedConnection);
    }

    @Test
    public void createStaleElementReferenceException() {
        driver.navigate().to("https://www.lambdatest.com/selenium-playground/");

        WebElement pageLink = driver.findElement(By.linkText("Table Data Search"));
        pageLink.click();
        WebElement filterByField = driver.findElement(By.id("task-table-filter"));

        filterByField.sendKeys("in progress");

        driver.navigate().back();

        pageLink.click();
        filterByField.sendKeys("completed");
    }

    @Test
    public void test_Selenium_Broken_Links() throws InterruptedException {
        driver.navigate().to("https://www.lambdatest.com/selenium-playground/");
        driver.manage().window().maximize();
        List<WebElement> links = driver.findElements(By.tagName("a"));
        Iterator<WebElement> link = links.iterator();

        String url = "";
        HttpURLConnection urlconnection = null;
        int responseCode = 200;
        /* For skipping email address */
        String mail_to = "mailto";
        String tel ="tel";
        String LinkedInPage = "https://www.linkedin.com";
        int valid_links = 0;
        int broken_links = 0;
        Boolean bLinkedIn = false;
        int LinkedInStatus = 999;

        Pattern pattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
        Matcher mat;

        while (link.hasNext())
        {
            url = link.next().getAttribute("href");
            System.out.println(url);
            bLinkedIn = false;

            if ((url == null) || (url.isEmpty()))
            {
                System.out.println("URL is either not configured for anchor tag or it is empty");
                continue;
            }

            /* String str="mailto:support@LambdaTest.com"; */
            if ((url.startsWith(mail_to)) || (url.startsWith(tel)))
            {
                System.out.println("Email address or Telephone detected");
                continue;
            }

            if(url.startsWith(LinkedInPage))
            {
                System.out.println("URL starts with LinkedIn, expected status code is 999");
                bLinkedIn = true;
            }

            try {
                urlconnection = (HttpURLConnection) (new URL(url).openConnection());
                urlconnection.setRequestMethod("HEAD");
                urlconnection.connect();
                responseCode = urlconnection.getResponseCode();
                if (responseCode >= 400)
                {
                    /* https://stackoverflow.com/questions/27231113/999-error-code-on-head-request-to-linkedin */
                    if ((bLinkedIn == true) && (responseCode == LinkedInStatus))
                    {
                        System.out.println(url + " is a LinkedIn Page and is not a broken link");
                        valid_links++;
                    }
                    else
                    {
                        System.out.println(url + " is a broken link");
                        broken_links++;
                    }
                }
                else
                {
                    System.out.println(url + " is a valid link");
                    valid_links++;
                }
            } catch (MalformedURLException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("Detection of broken links completed with " + broken_links + " broken links and " + valid_links + " valid links\n");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}