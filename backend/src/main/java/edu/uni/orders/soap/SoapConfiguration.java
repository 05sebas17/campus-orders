package edu.uni.orders.soap;

import java.util.*;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.*;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;
import org.springframework.ws.soap.server.endpoint.SoapFaultMappingExceptionResolver;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.*;

@EnableWs
@Configuration
public class SoapConfiguration implements WsConfigurer {
  @Bean
  ServletRegistrationBean<MessageDispatcherServlet> soapServlet(ApplicationContext context) {
    var servlet = new MessageDispatcherServlet();
    servlet.setApplicationContext(context);
    servlet.setTransformWsdlLocations(true);
    return new ServletRegistrationBean<>(servlet, "/ws/*");
  }

  @Bean
  XsdSchema orderSchema() {
    return new SimpleXsdSchema(new ClassPathResource("soap/orders.xsd"));
  }

  @Bean(name = "orders")
  DefaultWsdl11Definition orderWsdl(XsdSchema orderSchema) {
    var definition = new DefaultWsdl11Definition();
    definition.setPortTypeName("OrdersPort");
    definition.setLocationUri("/ws");
    definition.setTargetNamespace(OrderStatusEndpoint.NAMESPACE);
    definition.setSchema(orderSchema);
    return definition;
  }

  @Override
  public void addInterceptors(List<EndpointInterceptor> interceptors) {
    var validation = new PayloadValidatingInterceptor();
    validation.setXsdSchema(orderSchema());
    validation.setValidateRequest(true);
    validation.setValidateResponse(true);
    interceptors.add(validation);
  }

  @Bean
  SoapFaultMappingExceptionResolver soapFaults() {
    var resolver = new SoapFaultMappingExceptionResolver();
    var defaultFault = new SoapFaultDefinition();
    defaultFault.setFaultCode(SoapFaultDefinition.SERVER);
    defaultFault.setFaultStringOrReason("No se pudo completar la consulta.");
    resolver.setDefaultFault(defaultFault);
    var mappings = new Properties();
    mappings.setProperty("java.lang.IllegalArgumentException", "CLIENT,Identificador inválido");
    mappings.setProperty("edu.uni.orders.shared.NotFoundException", "CLIENT,Pedido no encontrado");
    resolver.setExceptionMappings(mappings);
    resolver.setOrder(1);
    return resolver;
  }
}
