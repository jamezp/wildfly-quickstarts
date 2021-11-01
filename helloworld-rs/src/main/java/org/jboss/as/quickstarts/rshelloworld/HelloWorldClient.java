package org.jboss.as.quickstarts.rshelloworld;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/client")
public class HelloWorldClient {

    @GET
    @Path("/json")
    @Produces(MediaType.APPLICATION_JSON)
    public String getHelloWorldJSON(@Context UriInfo uriInfo) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            return client.target(uriInfo.getBaseUriBuilder().path("json"))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(String.class);
        } finally {
            if (client != null) client.close();
        }
    }

    @GET
    @Path("/xml")
    @Produces(MediaType.APPLICATION_XML)
    public String getHelloWorldXML(@Context UriInfo uriInfo) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            return client.target(uriInfo.getBaseUriBuilder().path("xml"))
                    .request(MediaType.APPLICATION_XML_TYPE)
                    .get(String.class);
        } finally {
            if (client != null) client.close();
        }
    }
}
