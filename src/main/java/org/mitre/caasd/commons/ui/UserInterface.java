/*
 *    Copyright 2022 The MITRE Corporation
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.mitre.caasd.commons.ui;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import javax.swing.JFileChooser;

public class UserInterface {

    /**
     * Block the current thread until the User chooses a File.
     *
     * @param directory The directory where a JFileChooser's opens to
     *
     * @return The file selected by the user. This is returned as an {@literal Optional<File>}
     *     because it is possible for the user to cancel out of the JFileChooser.
     * @throws InterruptedException
     * @throws InvocationTargetException
     */
    public static Optional<File> haveUserChooseFile(String directory) throws InterruptedException, InvocationTargetException {

        class ShowFileChooserTask implements Runnable {

            Optional<File> usersFileSelection;

            @Override
            public void run() {

                final JFileChooser fc = new JFileChooser(directory);

                int returnValue = fc.showOpenDialog(null);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    usersFileSelection = Optional.of(fc.getSelectedFile());
                } else {
                    usersFileSelection = Optional.empty();
                }
            }
        }
        ShowFileChooserTask uiTask = new ShowFileChooserTask();

        /*
         * Must use EventQueue's invokeAndWait due to a swing threading issues (more severe on MAC)
         */
        EventQueue.invokeAndWait(uiTask);

        return uiTask.usersFileSelection;
    }

    /**
     * Block the current thread until the User chooses a File.
     *
     * @return The file selected by the user. This is returned as an {@literal Optional<File>}
     *     because it is possible for the user to cancel out of the JFileChooser.
     * @throws InterruptedException
     * @throws InvocationTargetException
     */
    public static Optional<File> haveUserChooseFile() throws InterruptedException, InvocationTargetException {
        return haveUserChooseFile(System.getProperty("user.dir"));
    }

}
