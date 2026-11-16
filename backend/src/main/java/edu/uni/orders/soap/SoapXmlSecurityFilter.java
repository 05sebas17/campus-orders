package edu.uni.orders.soap;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import javax.xml.stream.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validate before SAAJ parses the envelope. No DTD, external entities or unbounded bodies. */
@Component
public class SoapXmlSecurityFilter extends OncePerRequestFilter {
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/ws") || !request.getMethod().equals("POST");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    byte[] body = request.getInputStream().readNBytes(65537);
    try {
      if (body.length > 65536) throw new XMLStreamException("Too large");
      var factory = XMLInputFactory.newFactory();
      factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      factory.setXMLResolver(
          (publicId, systemId, base, namespace) -> {
            throw new XMLStreamException("External entity denied");
          });
      var reader = factory.createXMLStreamReader(new ByteArrayInputStream(body));
      try {
        while (reader.hasNext()) {
          int event = reader.next();
          if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE)
            throw new XMLStreamException("DTD denied");
        }
      } finally {
        reader.close();
      }
    } catch (XMLStreamException ex) {
      response.setStatus(400);
      response.setContentType("text/xml;charset=UTF-8");
      response
          .getWriter()
          .write(
              "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'><soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>XML inválido o no permitido.</faultstring></soap:Fault></soap:Body></soap:Envelope>");
      return;
    }
    chain.doFilter(
        new HttpServletRequestWrapper(request) {
          @Override
          public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
              public int read() {
                return input.read();
              }

              public boolean isFinished() {
                return input.available() == 0;
              }

              public boolean isReady() {
                return true;
              }

              public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
              }
            };
          }

          @Override
          public BufferedReader getReader() {
            return new BufferedReader(
                new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
          }
        },
        response);
  }
}
