package org.wildfly.quickstarts.microprofile.config;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Result {

    public enum Status {
        ACCEPTED,
        DENIED
    }

    private final Status status;
    private final String data;

    public Result(final Status status, final String data) {
        this.status = status;
        this.data = data;
    }

    public Status getStatus() {
        return status;
    }

    public String getData() {
        return data;
    }
}
