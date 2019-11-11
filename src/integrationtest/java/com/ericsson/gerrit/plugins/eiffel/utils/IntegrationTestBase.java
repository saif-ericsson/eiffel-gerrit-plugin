package com.ericsson.gerrit.plugins.eiffel.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.TestConfig;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.GetResponse;

import io.restassured.RestAssured;

public class IntegrationTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTestBase.class);

    protected static final String BRANCH_NAME = "master";
    protected static final String NON_MATCHING_BRANCH_NAME = "nonMatching";
    protected static final String DOMAIN_NAME = "test";

    protected FirefoxDriver driver;
    protected String projectName;
    protected String selector;
    protected String adminPassword;
    protected String changeID = "";
    protected List<GerritCommitResponse> gerritCommitResponses = new ArrayList<>();
    protected String changeStr = "";

    protected List<String> childProjectNames = new ArrayList<>();
    protected List<String> parentProjectNames = new ArrayList<>();

    protected void connectRMQ() throws IOException, TimeoutException {
        com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
        LOGGER.info("RabbitMQ URL: {}", TestConfig.RABBITMQ_URL);
        factory.setHost(TestConfig.RABBITMQ_URL);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        final AMQP.Exchange.DeclareOk exchangeOK = channel.exchangeDeclare(TestConfig.EXCHANGE_NAME, "topic", true);
        final AMQP.Queue.DeclareOk queueOK = channel.queueDeclare(TestConfig.QUEUE_NAME, true, false, false, null);
        final AMQP.Queue.BindOk bindOK = channel.queueBind(TestConfig.QUEUE_NAME, TestConfig.EXCHANGE_NAME, "#");
        assertEquals(true, exchangeOK != null);
        assertEquals(true, queueOK != null);
        assertEquals(true, bindOK != null);
    }

    protected List<String> consumeMessages(int messageCount, long timeout) throws IOException, TimeoutException {
        List<String> messages = new ArrayList<String>();
        com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
        LOGGER.info("RabbitMQ URL: {}", TestConfig.RABBITMQ_URL);
        factory.setHost(TestConfig.RABBITMQ_URL);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        long stopTime = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() < stopTime) {
            try {
                GetResponse response = channel.basicGet(TestConfig.QUEUE_NAME, true);
                if (response != null) {
                    messages.add(new String(response.getBody(), "UTF-8"));
                }

                if (messages.size() == messageCount) {
                    return messages;
                }
            } catch (Exception e) {
                LOGGER.error("RabbitMQ failed to get from queue", e.getMessage(), e);
            }
        }

        return messages;
    }

    protected void initFirefoxDriver() throws Exception {
        FirefoxOptions firefoxOptions = new FirefoxOptions().setHeadless(TestConfig.USE_HEADLESS)
                                                            .setLogLevel(FirefoxDriverLogLevel.ERROR);

        if (SystemUtils.IS_OS_LINUX) {
            LOGGER.debug("Using Firefox binary path {}", TestConfig.FIREFOX_BINARY_PATH);
            System.setProperty("webdriver.firefox.bin", TestConfig.FIREFOX_BINARY_PATH);
            System.setProperty("webdriver.gecko.driver", "src/integrationtest/resources/geckodriver");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            System.setProperty("webdriver.gecko.driver", "src/integrationtest/resources/geckodriver.exe");
        } else {
            LOGGER.error(SystemUtils.OS_NAME + " currently not supported.");
            throw new Exception();
        }

        driver = new FirefoxDriver(firefoxOptions);
    }

    protected String getChangeString(String projectName, String branch, String commitMessage) {
        return "  {\n" + "    \"project\" : \"" + projectName + "\",\n" + "    \"subject\" : \"" + commitMessage
                + "\",\n" + "    \"branch\" : \"" + branch + "\",\n" + "    \"topic\" : \"create-change-in-browser\",\n"
                + "    \"status\" : \"DRAFT\"\n" + "  }";
    }

    protected GerritCommitResponse createChange(String commitMsg) throws UnsupportedEncodingException {
        // First create a change and obtain changeId to use for later on
        io.restassured.response.Response response = io.restassured.RestAssured.given()
                                       .body(commitMsg)
                                       .contentType("application/json; charset=UTF-8")
                                       .filter(new JsonPrefixFilter())
                                       .urlEncodingEnabled(false)
                                       .auth()
                                       .basic(TestConfig.ADMIN_USERNAME, adminPassword)
                                       .baseUri(TestConfig.GERRIT_BASE_URL)
                                       .port(Integer.valueOf(TestConfig.GERRIT_PORT))
                                       .post("/a/changes/")
                                       .andReturn();

        GerritCommitResponse gerritCommitResponse = new GerritCommitResponse(response.getBody());
        gerritCommitResponses.add(gerritCommitResponse);
        changeID = gerritCommitResponse.getChangeID();

        return gerritCommitResponse;
    }

    protected void submitChange(String projectName, String branch, String commitMsg, GerritCommitResponse gerritCommitResponse) throws Exception {
        String urlEncodedProjectName = URLEncoder.encode(projectName, "UTF-8");
        String legacyID = gerritCommitResponse.getLegacyID();

        // First Publish
        RestAssured.given()
                   .body(commitMsg)
                   .contentType("application/json; charset=UTF-8")
                   .filter(new JsonPrefixFilter())
                   .urlEncodingEnabled(false)
                   .auth()
                   .basic(TestConfig.ADMIN_USERNAME, adminPassword)
                   .baseUri(TestConfig.GERRIT_BASE_URL)
                   .port(Integer.valueOf(TestConfig.GERRIT_PORT))
                   .post("/a/changes/" + urlEncodedProjectName + "~" + branch + "~" + changeID + "/publish")
                   .andReturn();

        // Then review with +2
        driver.get(TestConfig.GERRIT_BASE_URL + "/#/c/" + legacyID + "/");
        selector = "button[title='Apply score with one click']";
        WebElement reviewElement = new WebDriverWait(driver, TestConfig.TIME_OUT).until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        reviewElement.click();

        // Finally submit change
        io.restassured.response.Response responseSubmit = io.restassured.RestAssured.given()
                                             .body(commitMsg)
                                             .contentType("application/json; charset=UTF-8")
                                             .filter(new JsonPrefixFilter())
                                             .urlEncodingEnabled(false)
                                             .auth()
                                             .basic(TestConfig.ADMIN_USERNAME, adminPassword)
                                             .baseUri(TestConfig.GERRIT_BASE_URL)
                                             .port(Integer.valueOf(TestConfig.GERRIT_PORT))
                                             .post("/a/changes/" + urlEncodedProjectName + "~" + branch + "~" + changeID
                                                     + "/submit")
                                             .andReturn();
    }

    protected void submitChange(String projectName, String branch, String changeStr) throws Exception {
        GerritCommitResponse gerritCommitResponse = createChange(changeStr);
        submitChange(projectName, branch, changeStr, gerritCommitResponse);
    }

    protected void createProject(String projectName, String parent) throws UnsupportedEncodingException {
        String projectInit;
        boolean isChildProject = parent != null;
        if (isChildProject) {
            projectInit = "{\"parent\": " + parent + ", \"create_empty_commit\": true }";
            this.childProjectNames.add(projectName);
        } else {
            projectInit = "{\"create_empty_commit\": true}";
            this.parentProjectNames.add(projectName);
        }

        String urlEncodedProjectName = URLEncoder.encode(projectName, "UTF-8");

        RestAssured.given()
                   .body(projectInit)
                   .filter(new JsonPrefixFilter())
                   .urlEncodingEnabled(false)
                   .baseUri(TestConfig.GERRIT_BASE_URL)
                   .port(Integer.valueOf(TestConfig.GERRIT_PORT))
                   .auth()
                   .basic(TestConfig.ADMIN_USERNAME, adminPassword)
                   .contentType("application/json; charset=UTF-8")
                   .put("/a/projects/" + urlEncodedProjectName)
                   .then()
                   .log()
                   .ifError();

        // Configure Eiffel related info on Plugin

        driver.get(TestConfig.GERRIT_BASE_URL + "/#/admin/projects/" + projectName);
        selector = ".gwt-CheckBox > input";
        WebElement inputElement = new WebDriverWait(driver, TestConfig.TIME_OUT).until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        boolean isSelected = driver.findElement(By.cssSelector(selector)).isSelected();
        if (!isSelected) {
            inputElement.click();
        }

        selector = "#gerrit_body > div > div > div > div > div:nth-child(5) > table:nth-child(1) > tbody > tr:nth-child(5) > td:nth-child(2) > input";
        WebElement rmqDomainName = new WebDriverWait(driver, TestConfig.TIME_OUT).until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        rmqDomainName.clear();
        rmqDomainName.sendKeys(DOMAIN_NAME);

        selector = "#gerrit_body > div > div > div > div > div:nth-child(5) > table:nth-child(1) > tbody > tr:nth-child(6) > td:nth-child(2) > input";
        WebElement rmqExchangeName = new WebDriverWait(driver, TestConfig.TIME_OUT).until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        rmqExchangeName.clear();
        rmqExchangeName.sendKeys(TestConfig.EXCHANGE_NAME);

        selector = "#gerrit_body > div > div > div > div > div:nth-child(5) > table:nth-child(1) > tbody > tr:nth-child(7) > td:nth-child(2) > input";
        WebElement rmqServerAddr = new WebDriverWait(driver, TestConfig.TIME_OUT).until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        rmqServerAddr.clear();
        // rmqServerAddr.sendKeys(rabbitmqDockerURL);
        rmqServerAddr.sendKeys(TestConfig.RABBITMQ_DOCKER_URL);

        selector = "#gerrit_body > div > div > div > div > div:nth-child(5) > table:nth-child(1) > tbody > tr:nth-child(3) > td:nth-child(2) > input";
        WebElement filterBranch = new WebDriverWait(driver, TestConfig.TIME_OUT).until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        filterBranch.clear();
        filterBranch.sendKeys("");

        selector = "#gerrit_body > div > div > div > div > button";
        WebElement rmqSave = new WebDriverWait(driver, TestConfig.TIME_OUT).until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        rmqSave.click();
    }

    protected void setSubmitType(String type, String projectName) {
        driver.get(TestConfig.GERRIT_BASE_URL + "/#/admin/projects/" + projectName);

        selector = "#gerrit_body > div > div > div > div > table:nth-child(4) > tbody > tr:nth-child(3) > td:nth-child(2) > select";
        Select submitTypes = new Select(new WebDriverWait(driver, TestConfig.TIME_OUT).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector))));
        submitTypes.selectByVisibleText(type);

        selector = "#gerrit_body > div > div > div > div > button";
        WebElement saveButton = new WebDriverWait(driver, TestConfig.TIME_OUT).until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        saveButton.click();
    }

    protected void loginToGerrit() {
        driver.get(TestConfig.GERRIT_BASE_URL + "/login?account_id=1000000");

        try {
            WebElement skipIntroButton = new WebDriverWait(driver, 2).until(
                    ExpectedConditions.presenceOfElementLocated(By.className("navbar-brand")));
            skipIntroButton.click();
        } catch (Exception e) {
            LOGGER.debug("Gerrit was accessed without any intro page, that is not a bad thing.");
        }

        new WebDriverWait(driver, TestConfig.TIME_OUT).until(
                ExpectedConditions.presenceOfElementLocated(By.className("gwt-InlineLabel")));
        driver.get(TestConfig.GERRIT_BASE_URL + "/#/settings/http-password");
        WebElement passwordGenerateButton = (new WebDriverWait(driver, TestConfig.TIME_OUT)).until(
                ExpectedConditions.elementToBeClickable(By.className("gwt-Button")));
        passwordGenerateButton.click();
        selector = ".accountPassword > span";
        Boolean isGenerated = (new WebDriverWait(driver, TestConfig.TIME_OUT)).until(
                ExpectedConditions.invisibilityOfElementWithText(By.cssSelector(selector), "Generate Password"));
        if (isGenerated) {
            selector = ".accountPassword > span";
            WebElement passwordGenerateText = driver.findElement(By.cssSelector(selector));
            adminPassword = passwordGenerateText.getText();
        }
    }

    protected String getFirstMergeCommitID() {
        driver.get("http://localhost:8080/plugins/gitiles/always_merge");
        selector = "body > div > div > div.RepoShortlog > div.RepoShortlog-log > ol > li:nth-child(1) > a:nth-child(2)";

        WebElement commitInformationButton = new WebDriverWait(driver, TestConfig.TIME_OUT).until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        commitInformationButton.click();

        selector = "body > div > div > div.u-monospace.Metadata > table > tbody > tr:nth-child(1) > td:nth-child(2)";
        WebElement commitIDTextBox = new WebDriverWait(driver, TestConfig.TIME_OUT).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));

        return commitIDTextBox.getText();
    }
    protected void deleteProjects(List<String> projectNames) throws Exception {
        for (String projectName : projectNames) {

            String urlEncodedProjectName = URLEncoder.encode(projectName, "UTF-8");
            RestAssured.given()
                       .filter(new JsonPrefixFilter())
                       .urlEncodingEnabled(false)
                       .baseUri(TestConfig.GERRIT_BASE_URL)
                       .port(Integer.valueOf(TestConfig.GERRIT_PORT))
                       .auth()
                       .basic(TestConfig.ADMIN_USERNAME, adminPassword)
                       .delete("/a/projects/" + urlEncodedProjectName)
                       .then()
                       .log()
                       .ifError()
                       .assertThat()
                       .statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }
}
