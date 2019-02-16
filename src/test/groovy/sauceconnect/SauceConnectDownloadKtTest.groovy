package sauceconnect

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class SauceConnectDownloadKtTest extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle.kts')
        testProjectDir.newFile("settings.gradle.kts")
        buildFile << """
            plugins {
                id("com.saucelabs.SauceConnectPlugin")
            }
        """
    }

    def "can download latest version"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('downloadSauceConnect')
                .withPluginClasspath()
                .build()

        then:
        result.task(":downloadSauceConnect").outcome == SUCCESS
        new File(testProjectDir.root, "build").listFiles().any {
            it.name.contains("sc-")
        }
    }

    def "can download version 4.5.1"() {
        given:
        buildFile << """
            sauceconnect {
                sauceConnectVersion = "4.5.1"
                digests = mapOf("osx" to "adb6c71c091a970a7126ccfa4157218a0e608174")
            }
"""

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('downloadSauceConnect')
                .withPluginClasspath()
                .build()

        then:
        result.task(":downloadSauceConnect").outcome == SUCCESS
        new File(testProjectDir.root, "build").listFiles().any {
            it.name.contains("sc-4.5.1")
        }
    }

}
