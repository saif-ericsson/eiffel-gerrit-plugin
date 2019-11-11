package com.ericsson.gerrit.plugins.eiffel;

public class TestConfig {
    public static final String EIFFEL_MESSAGE_VERSION = "3.21.55.0.1";
    public static final String GERRIT_URL = "localhost";
    public static final String ADMIN_USERNAME = "admin";
    public static final Boolean USE_HEADLESS = true;

    public static String RABBITMQ_URL = "localhost";
    public static final String RABBITMQ_DOCKER_URL = "rabbitmq";
    public static final String QUEUE_NAME = "gerrit-test-queue";
    public static final String EXCHANGE_NAME = "eiffel.poc";

    public static final int GERRIT_PORT = 8080;
    public static String GERRIT_BASE_URL = String.format("http://%s:%s", GERRIT_URL, GERRIT_PORT);

    public static final int TIME_OUT = 10;
    public static String FIREFOX_BINARY_PATH = "/tmp/firefox-download-dir/firefox/firefox";
}
