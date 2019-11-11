Feature: Sending an Eiffel SCC event, whenever a patch set is created.
    As a user I want the plugin to send an Eiffel SCC event every time I
    submit a patch set in Gerrit.

    Background:
        Given Eiffel plugin is installed, loaded and enabled for project X in Gerrit
        And I configured RabbitMQ settings for project X

   
    @test_matching_branch
    Scenario: Matching branch.
        Given I create a project X
        And I set a branch filter that matches branch B
        When I submit an approved change in branch B of project X
        Then plugin should send an Eiffel SCE message containing the commit id, project name, branch name B and repoU





