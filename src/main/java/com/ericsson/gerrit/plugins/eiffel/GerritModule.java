/*
   Copyright 2019 Ericsson AB.
   For a full list of individual contributors, please see the commit history.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.ericsson.gerrit.plugins.eiffel;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.handlers.MessageQueueHandler;
import com.ericsson.gerrit.plugins.eiffel.listeners.ChangeMergedEventListener;
import com.ericsson.gerrit.plugins.eiffel.listeners.PatchsetCreatedEventListener;
import com.google.gerrit.common.EventListener;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.internal.UniqueAnnotations;

/**
 * This class that registers the plugin in gerrit.
 *
 * The name and path to the class must match the setting for the plugin maven-jar-plugin.
 *
 * The path and name is set in the option:
 * <configuration><archive><manifestEntries><Gerrit-Module>
 *
 */
public class GerritModule extends AbstractModule {

    @Override
    @CoberturaIgnore
    protected void configure() {
        bindMessageQueueHandler();
        bindGerritEventListeners();
        bindPluginConfiguration();
    }

    private void bindMessageQueueHandler() {
        bind(MessageQueueHandler.class).in(Scopes.SINGLETON);
        bind(LifecycleListener.class).annotatedWith(UniqueAnnotations.create())
                                     .to(MessageQueueHandler.class);
    }

    private void bindGerritEventListeners() {
        DynamicSet.bind(binder(), EventListener.class).to(ChangeMergedEventListener.class);
        DynamicSet.bind(binder(), EventListener.class).to(PatchsetCreatedEventListener.class);
    }

    private void bindPluginConfiguration() {
        bind(ProjectConfigEntry.class).annotatedWith(Exports.named(EiffelPluginConfiguration.ENABLED))
                .toInstance(new ProjectConfigEntry("Enable Eiffel messaging", false));
        bind(ProjectConfigEntry.class).annotatedWith(Exports.named(EiffelPluginConfiguration.FILTER))
                .toInstance(new ProjectConfigEntry("Filter branch", ""));
        bind(ProjectConfigEntry.class).annotatedWith(Exports.named(EiffelPluginConfiguration.FLOW_CONTEXT))
                .toInstance(new ProjectConfigEntry("Flow Context", ""));
        bind(ProjectConfigEntry.class).annotatedWith(Exports.named(EiffelPluginConfiguration.REMREM_PUBLISH_URL))
                .toInstance(new ProjectConfigEntry("REMReM Publish URL", ""));
        bind(ProjectConfigEntry.class).annotatedWith(Exports.named(EiffelPluginConfiguration.REMREM_USERNAME))
                .toInstance(new ProjectConfigEntry("REMReM Username", ""));

        // Currently the Gerrit has defined set of types that can be used. The Password is String type today, but will need some changes.
        bind(ProjectConfigEntry.class).annotatedWith(Exports.named(EiffelPluginConfiguration.REMREM_PASSWORD))
                .toInstance(new ProjectConfigEntry("REMReM Password", ""));
    }
}
