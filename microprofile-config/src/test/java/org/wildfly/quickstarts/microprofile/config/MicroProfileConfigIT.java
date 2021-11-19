package org.wildfly.quickstarts.microprofile.config;

import java.net.URL;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple tests for MicroProfile Config quickstart. Arquillian deploys an WAR archive to the application server, which
 * contains several endpoints exposing injected configuration values and verifies that they are correctly invoked.
 *
 * @author <a href="mstefank@redhat.com">Martin Stefanko</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MicroProfileConfigIT {

    @ArquillianResource
    private URL deploymentURL;

    private Client client;

    /**
     * Constructs a deployment archive
     *
     * @return the deployment archive
     */
    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addPackages(true, ContentChecker.class.getPackage())
                .addAsResource("META-INF/microprofile-config.properties")
                // enable CDI
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void before() {
        client = ClientBuilder.newClient();
    }

    @After
    public void after() {
        if (client != null) {
            client.close();
        }
    }

    /**
     * Tests that /config/value returns the value configured in microprofile-config.properties
     */
    @Test
    public void alphaTest() {
        Response response = client
                .target(deploymentURL.toString())
                .path("/check-content")
                .request()
                .post(Entity.text("This is an alpha test"));

        Assert.assertEquals(200, response.getStatus());
        final JsonObject expected = Json.createObjectBuilder()
                .add("status", "DENIED")
                .add("data", "This is an **** test")
                .build();
        Assert.assertEquals(expected, response.readEntity(JsonObject.class));

        response.close();
    }
}
