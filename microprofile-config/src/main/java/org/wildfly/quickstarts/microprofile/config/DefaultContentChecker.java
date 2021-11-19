package org.wildfly.quickstarts.microprofile.config;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ApplicationScoped
public class DefaultContentChecker implements ContentChecker {

    @Inject
    @ConfigProperty(name = "content.check.words")
    private List<String> words;


    @Override
    public Result check(final String data) {
        Result.Status status = Result.Status.ACCEPTED;
        String cleanedData = data;
        for (String word : words) {
            if (data.contains(word)) {
                cleanedData = cleanedData.replaceAll(word, "****");
                status = Result.Status.DENIED;
            }
        }
        return new Result(status, cleanedData);
    }
}
