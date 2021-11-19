package org.wildfly.quickstarts.microprofile.config;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface ContentChecker {

    Result check(String data);
}
