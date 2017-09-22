import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import breder.util.util.FileUtils;
import breder.util.util.StringUtils;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class Web {

  public static void main(String[] args) throws IOException {
    final File dir = new File("out");
    dir.mkdirs();
    WebClient client = htmlunit();
    Set<String> nodes = new HashSet<String>(8 * 1024);
    List<String> stack = new ArrayList<String>(1024);
    String domain = "http://en.wikipedia.org";
    String source = domain + "/wiki/Main_Page";
    stack.add(source);
    nodes.add(source);
    while (stack.size() > 0) {
      String parentUrl = stack.remove(0);
      try {
        HtmlPage page = client.getPage(parentUrl);
        List<?> tags = page.getByXPath("//div[@id='bodyContent']//a");
        for (Object tag : tags) {
          if (tag instanceof HtmlAnchor) {
            HtmlAnchor elem = (HtmlAnchor) tag;
            try {
              String url = getUrl(domain, elem);
              if (url != null) {
                final File file = buildFile(dir, url);
                if (!nodes.contains(url) && url.startsWith(domain)) {
                  nodes.add(url);
                  stack.add(url);
                  try {
                    HtmlPage childPage = client.getPage(url);
                    HtmlElement elementById =
                      childPage.getElementById("bodyContent");
                    if (elementById != null) {
                      final String content = buildContent(elementById);
                      System.out.println("opening : " + url);
                      new Thread(url) {
                        @Override
                        public void run() {
                          try {
                            FileOutputStream foutput =
                              new FileOutputStream(file);
                            foutput.write(content.getBytes());
                            foutput.close();
                          }
                          catch (IOException e) {
                          }
                        }
                      }.start();
                    }
                  }
                  catch (Throwable e) {
                  }
                }
              }
            }
            catch (Throwable e) {
            }
          }
        }
      }
      catch (Throwable e) {
      }
    }
  }

  private static String buildContent(HtmlElement elementById) {
    String text = elementById.asText();
    text = StringUtils.removeAccents(text).replaceAll("[^\\w]", " ");
    while (text.indexOf("  ") >= 0) {
      text = text.replace("  ", " ");
    }
    return text;
  }

  private static File buildFile(final File dir, String url) {
    return new File(dir, Main.fixUrl(url));
  }

  private static String getUrl(String domain, HtmlAnchor elem) {
    String url = null;
    if (url == null) {
      try {
        url = elem.getHrefAttribute();
        if (url.startsWith("/") || url.startsWith("#") || url.startsWith("?")) {
          url = domain + url;
        }
      }
      catch (Throwable e) {
      }
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

  private static WebClient htmlunit() {
    WebClient client = new WebClient();
    return client;
  }

}
