package org.jboss.as.quickstarts.rshelloworld;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonDatatypeJacksonProducer implements ContextResolver<ObjectMapper> {

    private final ObjectMapper json;

    public JacksonDatatypeJacksonProducer() throws Exception {
        this.json = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        return json;
    }
}