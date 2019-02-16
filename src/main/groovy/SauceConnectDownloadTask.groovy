package com.saucelabs

import groovy.json.JsonSlurper
import java.io.File
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.Architecture

class SauceConnectDownloadTask extends DefaultTask implements SauceConnectHelper{

    int KB = 1024
    int MB = 1024*KB
    def artifactName

    def previousVersion = [
        '4.5.1': [
                "Sauce Connect": [
                    "linux": [
                        "download_url": "https://saucelabs.com/downloads/sc-4.5.1-linux.tar.gz",
                        "sha1": "5ca9328724c5ff16b12ea49e7a748d44f7305be5"
                    ],
                    "linux32": [
                        "download_url": "https://saucelabs.com/downloads/sc-4.5.1-linux32.tar.gz",
                        "sha1": "ad0359956e1cbb6fd45ad691d050b50534bff765"
                    ],
                    "osx": [
                        "download_url": "https://saucelabs.com/downloads/sc-4.5.1-osx.zip",
                        "sha1": "adb6c71c091a970a7126ccfa4157218a0e608174"
                    ],
                    "win32": [
                        "download_url": "https://saucelabs.com/downloads/sc-4.5.1-win32.zip",
                        "sha1": "a74d632a8f90763a98759e82200c9f8dca08e45b"
                    ]
                ]
        ]
    ]

    Map getSauceVersionInformation() {
        def version = project.sauceconnect.sauceConnectVersion

        if (version != null) {
            def sauceData = previousVersion[version]

            if (sauceData == null) {
                throw new IllegalArgumentException("invalid version $version")
            }

            return sauceData
        }

        try {
            def sauceData = new URL("https://saucelabs.com/versions.json").getText()
            def jsonSlurper = new JsonSlurper()
            return jsonSlurper.parseText(sauceData)
        } catch(Exception e) {
            println "Couldn't connect to SauceLabs versions API. Exception:\n " + e.toString()
        }
    }

    String getArtifactName(String downloadURL) {
        String[] splitURL = downloadURL.split('/')
        return  splitURL[splitURL.size()-1]
    }

    String getCheckSum(File file) {
        def messageDigest = MessageDigest.getInstance("SHA1")
        file.eachByte(MB) { byte[] buf, int bytesRead ->
            messageDigest.update(buf, 0, bytesRead)
        }
        return new BigInteger(1, messageDigest.digest()).toString(16).padLeft( 40, '0' )
    }

    Boolean scArtifactExists(File scArtifact, String expectedHash) {
      Map sauceVersions = getSauceVersionInformation()
      if(scArtifact.isFile()) {
        // We need to make sure that downloaded file has correct hash
        def artifactCheckSum = getCheckSum(scArtifact)
        if(expectedHash == artifactCheckSum) {
          return true
        } else {
          return false
        }
      } else {
        return false
      }
    }

    @TaskAction
    def downloadSauceConnect() {
        def buildFolder = new File("$project.buildDir")
        def sauceConnectOSType = getOSType()
        Map sauceVersions = getSauceVersionInformation()
        String fileHash
        String sourceUrl
        String target
        if (!buildFolder.exists()){
            buildFolder.mkdirs()
        }
        sourceUrl = sauceVersions["Sauce Connect"][sauceConnectOSType].download_url
        fileHash = sauceVersions["Sauce Connect"][sauceConnectOSType].sha1
        artifactName = getArtifactName(sourceUrl)
        project.ext.set("artifactName", artifactName)
        target = "$buildFolder/" + artifactName
        def targetFile = new File(target)
        if (!scArtifactExists(targetFile, fileHash)) {
            println "Downloading SauceConnect..."
            ant.get(src: sourceUrl, dest: target)
            println "Downloaded SauceConnect to " + target
            def artifactCheckSum = getCheckSum(new File(target))
            assert artifactCheckSum == fileHash : "Incorrect SauceConnect artifact checksum."
        } else {
          println "SauceConnect already exists. Not downloading."
        }

    }
}
