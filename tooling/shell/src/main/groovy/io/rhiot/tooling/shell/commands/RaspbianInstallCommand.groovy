/**
 * Licensed to the Rhiot under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.rhiot.tooling.shell.commands

import io.rhiot.tooling.shell.DownloadManager
import io.rhiot.utils.WithLogger
import io.rhiot.utils.process.ProcessManager
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import java.util.zip.ZipInputStream

@Component
class RaspbianInstallCommand implements WithLogger {

    private final String devicesDirectory

    private final DownloadManager downloadManager

    private final ProcessManager processManager

    @Autowired
    RaspbianInstallCommand(@Value('${devices.directory:/dev}') String devicesDirectory,
                           DownloadManager downloadManager, ProcessManager processManager) {
        this.devicesDirectory = devicesDirectory
        this.downloadManager = downloadManager
        this.processManager = processManager
    }

    List<String> execute(String device) {
        downloadManager.download(
                new URL('http://director.downloads.raspberrypi.org/raspbian/images/raspbian-2015-11-24/2015-11-21-raspbian-jessie.zip'),
                '2015-11-21-raspbian-jessie.zip')

        def imageZip = downloadManager.downloadedFile('2015-11-21-raspbian-jessie.zip')
        def image = downloadManager.downloadedFile('2015-11-21-raspbian-jessie.img')
        if(!image.exists()) {
            def tmpImageFile = File.createTempFile('rhiot', 'raspbian')
            def zip = new ZipInputStream(new FileInputStream(imageZip))
            zip.nextEntry
            log().debug('Extracting Raspbian ZIP {} to {}', imageZip.absolutePath, tmpImageFile.absolutePath)
            IOUtils.copyLarge(zip, new FileOutputStream(image))
            zip.close()
        }
        def output = processManager.executeAndJoinOutput("dd", "bs=4M", "if=${image}", "of=${devicesDirectory}/${device}")
        output + processManager.executeAndJoinOutput('sync')
    }

}