package org.wildfly.quickstarts.microprofile.config;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class RootResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRootResponse(@Context UriInfo uriInfo) {
        final String text = "A test with alpha and delta";
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("sentData", text);
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            final Response response = client.target(uriInfo.getBaseUriBuilder().path("check-content"))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.text(text));
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                builder.add("result", response.readEntity(JsonObject.class));
            } else {
                throw new WebApplicationException("Failed to process request");
            }
        } finally {
            if (client != null) client.close();
        }
        return Response.ok(builder.build()).build();
    }
}
