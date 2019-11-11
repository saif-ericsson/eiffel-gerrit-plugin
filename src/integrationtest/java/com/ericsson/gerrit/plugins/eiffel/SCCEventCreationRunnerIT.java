package com.ericsson.gerrit.plugins.eiffel;


import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/integrationtest/resources/features/scc-event-creation.feature", glue = {
        "com.ericsson.gerrit.plugins.eiffel" }, plugin = { "pretty",
                "html:target/cucumber-reports/SCCEventCreationRunnerIT" })
public class SCCEventCreationRunnerIT {
}
