import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import breder.util.util.FileUtils;

import com.opera.core.systems.OperaDriver;

public class Main {

  public static void main(String[] args) throws IOException {
    final File dir = new File("out");
    dir.mkdirs();
    WebDriver client = firefox();
    try {
      Set<String> nodes = new HashSet<String>(1024);
      List<String> stack = new ArrayList<String>(1024);
      String domain = "http://en.wikipedia.org";
      String source = domain + "/wiki/Main_Page";
      stack.add(source);
      nodes.add(source);
      while (stack.size() > 0) {
        String parentUrl = stack.remove(0);
        try {
          client.get(parentUrl);
          List<WebElement> elems =
            client.findElements(By.xpath("//div[@id='bodyContent']//a"));
          for (WebElement elem : elems) {
            try {
              String url = getUrl(client, domain, elem);
              if (url != null) {
                final File file = new File(dir, fixUrl(url));
                if (!nodes.contains(url) && url.startsWith(domain)
                  && !file.exists()) {
                  nodes.add(url);
                  stack.add(url);
                  try {
                    client.get(url);
                    System.out.println("opening : " + url);
                    final String contentPage = client.getPageSource();
                    new Thread(url) {
                      @Override
                      public void run() {
                        try {
                          FileOutputStream foutput = new FileOutputStream(file);
                          foutput.write(contentPage.getBytes());
                          foutput.close();
                        }
                        catch (IOException e) {
                        }
                      }
                    }.start();
                    client.navigate().back();
                  }
                  catch (Throwable e) {
                    client.get(parentUrl);
                  }
                }
              }
            }
            catch (Throwable e) {
            }
          }
        }
        catch (Throwable e) {
        }
      }
    }
    finally {
      client.close();
    }
  }

  public static String fixUrl(String text) {
    if (text.startsWith("http://")) {
      text = text.substring("http://".length());
    }
    String[] splits = text.split("/");
    for (int n = 0; n < splits.length; n++) {
      splits[n] = FileUtils.fixFileName(splits[n]);
    }
    StringBuilder sb = new StringBuilder();
    for (int n = 0; n < splits.length; n++) {
      sb.append(splits[n]);
      if (n != splits.length - 1) {
        sb.append("/");
      }
    }
    text = sb.toString();
    if (text.endsWith(".htm")) {
      text = text.substring(0, text.length() - ".htm".length());
    }
    if (!text.endsWith(".html")) {
      text = text + ".html";
    }
    return text;
  }

  public static String getUrl(WebDriver client, String domain, WebElement elem) {
    String url = null;
    try {
      url = elem.getAttribute("href");
    }
    catch (Throwable e) {
    }
    if (url == null) {
      String toString = elem.toString();
      int index = toString.indexOf("href");
      if (index >= 0) {
        int begin = toString.indexOf("\"", index);
        if (begin < 0) {
          begin = toString.indexOf("'", index);
        }
        if (begin >= 0) {
          String str = "" + toString.charAt(begin);
          int end = toString.indexOf(str, begin + 1);
          if (end >= 0) {
            url = toString.substring(begin + 1, end);
            if (url.startsWith("/")) {
              url = domain + url;
            }
          }
        }
      }
    }
    return url;
  }

  private static FirefoxDriver firefox() {
    return new FirefoxDriver();
  }

  private static HtmlUnitDriver htmlunit() {
    HtmlUnitDriver driver = new HtmlUnitDriver();
    return driver;
  }

  private static OperaDriver opera() {
    return new OperaDriver();
  }

  private static WebDriver chrome() throws IOException {
    DesiredCapabilities capability = DesiredCapabilities.chrome();
    WebDriver driver =
      new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capability);
    return driver;
  }

}
