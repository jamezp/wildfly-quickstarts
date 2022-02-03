package org.jboss.as.quickstarts.rshelloworld;

import java.time.LocalDateTime;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/date")
public class DateResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response current() {
        return Response.ok(LocalDateTime.now()).build();
    }
}
