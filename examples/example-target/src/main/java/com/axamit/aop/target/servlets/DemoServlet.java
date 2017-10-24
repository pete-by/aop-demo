package com.axamit.aop.target.servlets;

import com.axamit.aop.target.exported.ConsumerService;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import javax.servlet.ServletException;
import java.io.IOException;


@SlingServlet(
        paths = "/bin/demo/servlet",
        extensions = "html",
        methods = { "GET", "POST" })
@Properties(
        @Property(name = "sling.auth.requirements", value = "-/bin/demo/servlet")
)
public class DemoServlet extends SlingAllMethodsServlet {

    @Reference
    private ConsumerService consumerService;

    ConsumerService getConsumerService() {
        return consumerService;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        printPage(response);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        String title = String.valueOf(request.getParameter("title"));
        getConsumerService().updateTitle(title);

        printPage(response);
    }

    private void printPage(SlingHttpServletResponse response) throws IOException {

        response.setContentType("application/xhtml+xml");
        response.setCharacterEncoding("utf-8");
        response.getWriter().write(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                        "<head>\n" +
                        "<title>AOP Demo</title>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "<div>\n" +
                        "<form action=\"/bin/demo/servlet\" method=\"post\" style=\"text-align:center;margin-top:200px;\">\n" +
                        "      <label for=\"title\">Title</label>\n"+
                        "      <input id=\"title\" type=\"text\" name=\"title\" />\n" +
                        "      <input type=\"submit\" value=\"Submit\" />\n" +
                        "</form>\n" +
                        "</div>\n" +
                        "</body>\n" +
                        "</html>");

        response.getWriter().flush();
    }

}
