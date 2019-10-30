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

package com.ericsson.gerrit.plugins.eiffel.state;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.sql.SQLException;

import com.ericsson.gerrit.plugins.eiffel.events.EiffelEvent;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeSubmittedEvent;
import com.ericsson.gerrit.plugins.eiffel.handlers.NoSuchElementException;
import com.ericsson.gerrit.plugins.eiffel.handlers.Table;

public class SourceChangeSubmittedState extends State {

    public SourceChangeSubmittedState(File pluginDir) {
        super(pluginDir);
    }

    @Override
    public String getEventId(String project, String branch) throws NoSuchElementException, ConnectException, FileNotFoundException {
        return getLastSubmittedEiffelEvent(project, branch, Table.SCS_TABLE);
    }

    @Override
    public void setState(String eiffelEventId, EiffelEvent eiffelEvent)
            throws NoSuchElementException, ConnectException, SQLException {
        EiffelSourceChangeSubmittedEvent eiffelSourceChangeSubmittedEvent = (EiffelSourceChangeSubmittedEvent) eiffelEvent;
        String projectName = eiffelSourceChangeSubmittedEvent.eventParams.data.gitIdentifier.repoName;
        String branch = eiffelSourceChangeSubmittedEvent.eventParams.data.gitIdentifier.branch;

        setLastSubmittedEiffelEvent(projectName, branch, eiffelEventId, Table.SCS_TABLE);
    }
}
