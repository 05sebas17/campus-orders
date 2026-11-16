package edu.uni.orders.soap;

import edu.uni.orders.orders.application.OrderService;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.ws.server.endpoint.annotation.*;
import org.w3c.dom.Element;

@Endpoint
public class OrderStatusEndpoint {
  public static final String NAMESPACE = "urn:uni:orders:v1";
  private final OrderService service;

  public OrderStatusEndpoint(OrderService service) {
    this.service = service;
  }

  @PayloadRoot(namespace = NAMESPACE, localPart = "GetOrderStatusRequest")
  @ResponsePayload
  public Element status(@RequestPayload Element request) throws Exception {
    var ids = request.getElementsByTagNameNS(NAMESPACE, "orderId");
    if (ids.getLength() != 1) throw new IllegalArgumentException("Identificador inválido.");
    String value = ids.item(0).getTextContent();
    if (!value.matches(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
      throw new IllegalArgumentException("Identificador inválido.");
    var order = service.get(UUID.fromString(value));
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    var doc = factory.newDocumentBuilder().newDocument();
    var result = doc.createElementNS(NAMESPACE, "GetOrderStatusResponse");
    doc.appendChild(result);
    String[][] fields = {
      {"number", order.number()},
      {"status", order.status().name()},
      {"updatedAt", order.updatedAt().toString()}
    };
    for (var field : fields) {
      var node = doc.createElementNS(NAMESPACE, field[0]);
      node.setTextContent(field[1]);
      result.appendChild(node);
    }
    return result;
  }
}
