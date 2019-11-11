package com.ericsson.gerrit.plugins.eiffel;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/integrationtest/resources/features/test-message.feature", glue = { "com.ericsson.gerrit.plugins.eiffel" }, plugin = { "pretty",
        "html:target/cucumber-reports/TestMessageRunnerIT" })
public class TestMessageRunnerIT {
}
