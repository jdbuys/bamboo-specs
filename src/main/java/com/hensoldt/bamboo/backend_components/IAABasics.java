package com.hensoldt.bamboo.backend_components;

import com.atlassian.bamboo.specs.api.BambooSpec;
import com.atlassian.bamboo.specs.api.builders.AtlassianModule;
import com.atlassian.bamboo.specs.api.builders.BambooKey;
import com.atlassian.bamboo.specs.api.builders.BambooOid;
import com.atlassian.bamboo.specs.api.builders.Variable;
import com.atlassian.bamboo.specs.api.builders.applink.ApplicationLink;
import com.atlassian.bamboo.specs.api.builders.condition.AnyTaskCondition;
import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;
import com.atlassian.bamboo.specs.api.builders.permission.Permissions;
import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions;
import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.plan.branches.BranchCleanup;
import com.atlassian.bamboo.specs.api.builders.plan.branches.PlanBranchManagement;
import com.atlassian.bamboo.specs.api.builders.plan.configuration.ConcurrentBuilds;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.api.builders.repository.VcsChangeDetection;
import com.atlassian.bamboo.specs.api.builders.repository.VcsRepositoryIdentifier;
import com.atlassian.bamboo.specs.api.builders.task.AnyTask;
import com.atlassian.bamboo.specs.builders.repository.bitbucket.server.BitbucketServerRepository;
import com.atlassian.bamboo.specs.builders.repository.viewer.BitbucketServerRepositoryViewer;
import com.atlassian.bamboo.specs.builders.task.CheckoutItem;
import com.atlassian.bamboo.specs.builders.task.InjectVariablesTask;
import com.atlassian.bamboo.specs.builders.task.MavenTask;
import com.atlassian.bamboo.specs.builders.task.ScriptTask;
import com.atlassian.bamboo.specs.builders.task.VcsCheckoutTask;
import com.atlassian.bamboo.specs.builders.task.VcsCommitTask;
import com.atlassian.bamboo.specs.builders.task.VcsPushTask;
import com.atlassian.bamboo.specs.builders.trigger.BitbucketServerTrigger;
import com.atlassian.bamboo.specs.model.task.InjectVariablesScope;
import com.atlassian.bamboo.specs.model.task.ScriptTaskProperties;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.MapBuilder;

@BambooSpec
public class IAABasics {
    
    public Plan plan() {
        return new Plan(new Project()                
                .key(new BambooKey("BECCBB"))
                .name("GEW ENG_Project - Backend Components"),
            "iaa-basics",
            new BambooKey("IAABASICS"))            
            .pluginConfigurations(new ConcurrentBuilds())
            .stages(new Stage("Default Stage")
                    .jobs(new Job("Default Job",
                            new BambooKey("JOB1"))
                            .tasks(new VcsCheckoutTask()
                                    .description("Checkout Default Repository")
                                    .checkoutItems(new CheckoutItem().defaultRepository()
                                            .path("iaa-basics"))
                                    .cleanCheckout(true),
                                new AnyTask(new AtlassianModule("com.atlassian.bamboo.plugins.variable.updater.variable-updater-generic:variable-extractor"))
                                    .description("Extract POM Version")
                                    .configuration(new MapBuilder()
                                            .put("overrideCustomised", "")
                                            .put("branchVars", "")
                                            .put("variable", "pomVersion")
                                            .put("removeSnapshot", "")
                                            .put("includeGlobals", "")
                                            .put("variableScope", "RESULT")
                                            .put("pomFile", "iaa-basics/pom.xml")
                                            .build()),
                                new ScriptTask()
                                    .description("Initialise Script Linux")
                                    .enabled(false)
                                    .interpreter(ScriptTaskProperties.Interpreter.BINSH_OR_CMDEXE)
                                    .inlineBody("echo \"The version is: ${bamboo.pomVersion}\"\necho \"The branch: ${bamboo.planRepository.branch}\"\n\n# Copy formatter file\ncp \"${bamboo.working.directory}/bamboo-config-files/eclipse-formatter-profile.xml\" \"${bamboo.working.directory}/iaa-basics/formatter.xml\"\ncp /opt/java/openjdk/lib/tools.jar /opt/java/openjdk/jre/lib/tools.jar"),
                                new ScriptTask()
                                    .description("Initialize Script")
                                    .interpreter(ScriptTaskProperties.Interpreter.WINDOWS_POWER_SHELL)
                                    .inlineBody("echo \"The version is: ${bamboo.pomVersion}\"\necho \"The branch: ${bamboo.planRepository.branch}\"\nCopy-Item -Path \"$env:bamboo_build_working_directory\\bamboo-config-files\\eclipse-formatter-profile.xml\"  -Destination \"$env:bamboo_build_working_directory\\iaa-basics\\formatter.xml\"\n$propsFile = \"$env:bamboo_build_working_directory\\bamboo_variables.properties\"\n$mvnSettings = \"bamboo-config-files/snapshot-settings.xml\"\nif (\"${bamboo.planRepository.branch}\" -eq \"master\") { $mvnSettings = \"bamboo-config-files/release-settings.xml\" }\n\n$tagExists = git ls-remote --tags origin ${bamboo.pomVersion}\n\n$isSnapshot = $false\n$isSnapshot = if (\"${bamboo.pomVersion}\" -like \"*SNAPSHOT*\") { $true }\n\nif (\"${bamboo.planRepository.branch}\" -eq \"master\" -and $isSnapshot){\n    Write-Output \"Build failed because you are trying to build a release with a SNAPSHOT version\"\n    exit 1;\n}\n\nif (\"${bamboo.planRepository.branch}\" -ne \"master\" -and !$isSnapshot){\n     Write-Output \"Build failed because you are trying to build a release from a branch\"\n     exit 1;\n}\n\n\nWrite-Output \"This is a snapshot repo: $isSnapshot and the tagExists: $tagExists\"\n\n\nif (\"${bamboo.planRepository.branch}\" -eq \"master\"){\n    Write-Output \"Fetching all the tags\"\n    git fetch --tags\n    \n    Write-Output \"Checkout the tag: ${bamboo.pomVersion}\"\n    git checkout ${bamboo.pomVersion}\n}\n\n\"IS_SNAPSHOT=$isSnapshot\" | Out-File -FilePath $propsFile -Encoding UTF8 -Append\n\"MVN_SETTINGS_FILE=$mvnSettings\" | Out-File -FilePath $propsFile -Encoding UTF8 -Append"),
                                new InjectVariablesTask()
                                    .description("Load Variables")
                                    .path("bamboo_variables.properties")
                                    .namespace("inject")
                                    .scope(InjectVariablesScope.RESULT),
                                new MavenTask()
                                    .description("Clean, Format and Compile on Branch")
                                    .conditions(new AnyTaskCondition(new AtlassianModule("com.atlassian.bamboo.plugins.bamboo-conditional-tasks:variableCondition"))
                                            .configuration(new MapBuilder()
                                                    .put("operation", "not_equals")
                                                    .put("value", "master")
                                                    .put("variable", "bamboo.planRepository.branch")
                                                    .build()))
                                    .goal("-U -s ../${bamboo.inject.MVN_SETTINGS_FILE} clean spotless:apply compile test")
                                    .jdk("JDK 8")
                                    .executableLabel("Maven")
                                    .workingSubdirectory("iaa-basics"),
                                new MavenTask()
                                    .description("Clean, Format and Compile on Master")
                                    .conditions(new AnyTaskCondition(new AtlassianModule("com.atlassian.bamboo.plugins.bamboo-conditional-tasks:variableCondition"))
                                            .configuration(new MapBuilder()
                                                    .put("operation", "matches")
                                                    .put("value", "master")
                                                    .put("variable", "bamboo.planRepository.branch")
                                                    .build()))
                                    .goal("-U -s ../${bamboo.inject.MVN_SETTINGS_FILE}  -Denforcer.rules=requireReleaseDeps clean spotless:apply compile test enforcer:enforce")
                                    .jdk("JDK 8")
                                    .executableLabel("Maven")
                                    .workingSubdirectory("iaa-basics"),
                                new MavenTask()
                                    .description("Sonar Task")
                                    .goal("-s ../${bamboo.inject.MVN_SETTINGS_FILE}  -Dsonar.token=${bamboo.SONAR_TOKEN_PASSWORD} -D sonar.host.url=https://sonarqube.hensoldt.co.za -Dsonar.skipCompile=true  -Dsonar.qualitygate.wait=true -Dsonar.qualitygate.timeout=300 sonar:sonar")
                                    .jdk("JDK 17")
                                    .executableLabel("Maven")
                                    .workingSubdirectory("iaa-basics"),
                                new VcsCheckoutTask()
                                    .description("Clone Again After Spotless")
                                    .checkoutItems(new CheckoutItem()
                                            .repository(new VcsRepositoryIdentifier()
                                                    .name("iaa-basics"))
                                            .path("iaa-basics"))
                                    .cleanCheckout(true),
                                new MavenTask()
                                    .description("Package and Deploy")
                                    .goal("-s ../${bamboo.inject.MVN_SETTINGS_FILE} clean deploy")
                                    .environmentVariables("DT_API_TOKEN_PASSWORD=${bamboo.DT_API_TOKEN_PASSWORD} DT_API_URL=${bamboo.DT_API_URL}")
                                    .jdk("JDK 8")
                                    .executableLabel("Maven")
                                    .workingSubdirectory("iaa-basics"),
                                new ScriptTask()
                                    .description("Create Changelog")
                                    .interpreter(ScriptTaskProperties.Interpreter.WINDOWS_POWER_SHELL)
                                    .inlineBody("$jdk21Path = $env:bamboo_capability_system_jdk_JDK_21\n\n#Download the Changelog jar if it does not exist yet.\n\nif (Test-Path \"$env:TEMP\\git-changelog-command-line.jar\") {\n    Write-Output \"Changelog jar already exists\"\n} else {\n    curl https://repo1.maven.org/maven2/se/bjurr/gitchangelog/git-changelog-command-line/2.5.9/git-changelog-command-line-2.5.9.jar -OutFile \"$env:TEMP\\git-changelog-command-line.jar\"\n}\n\n& \"$jdk21Path/bin/java.exe\" -jar $env:TEMP\\git-changelog-command-line.jar -of Changelog.md -tec \"\n# Changelog\n{{#tags}}\n## {{name}} ({{tagDate .}})\n{{#issues}}\n{{#hasIssue}}\n{{#hasLink}}\n### {{name}} [{{issue}}]({{link}}) {{title}} {{#hasIssueType}} *{{issueType}}* {{/hasIssueType}} {{#hasLabels}} {{#labels}} *{{.}}* {{/labels}} {{/hasLabels}}\n{{/hasLink}}\n{{^hasLink}}\n### {{name}} {{issue}} {{title}} {{#hasIssueType}} *{{issueType}}* {{/hasIssueType}} {{#hasLabels}} {{#labels}} *{{.}}* {{/labels}} {{/hasLabels}}\n{{/hasLink}}\n{{/hasIssue}}\n{{#commits}}\n{{{messageTitle}}}\n\n{{/commits}}\n{{/issues}}\n{{/tags}}\n\"\n\nif ($LASTEXITCODE -ne 0) {\n    Write-Error \"Creating the changelog failed\"\n    exit 1\n}\n\nWrite-Host \"Script completed successfully.\"")
                                    .workingSubdirectory("iaa-basics"),
                                new VcsCommitTask()
                                    .defaultRepository()
                                    .description("Commit Changelog")
                                    .commitMessage("Changelog updated by Bamboo"),
                                new VcsPushTask()
                                    .defaultRepository()
                                    .description("Push changelog commit"))))
            .linkedRepositories("iaa-basics")
            .planRepositories(new BitbucketServerRepository()
                    .name("bamboo-config-files")                    
                    .repositoryViewer(new BitbucketServerRepositoryViewer())
                    .server(new ApplicationLink()
                            .name("Bitbucket")
                            .id("bd5eefc5-f23b-3226-85b7-d5394f3287dd"))
                    .projectKey("GEBAMBOO")
                    .repositorySlug("config-scripts")
                    .sshPublicKey("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIPHPuO9SWFxR9CLokXJVWVqiyRCFX9cejXJKOYn7RquA https://bamboo.hensoldt.co.za")
                    .sshPrivateKey("BAMSCRT@0@0@NF23lwyerwGClISOpCSJV2v8UWwG1YlzEDYDQrKwRMg6d1P7PzN550GmO7iyxDYR7igpF2stG9wqhcyIwQQq85n+NOgFjJ0lqz11iQk0f0AkAFdaMR/M4S3gRr0MCVtWsQ7s4N2v+/FzRKVieV4ZSyvm2uJcEbDnB/3k1MS41EOhUnla/wwZUwRNrrHXgXIWYJa9DPPfp4LK2HbrzUjxksLU0WoLFk4NVRPoXX1Y+il08L7LygjdHyc3vyuqXtwFZ91VucbYof2AJI2JW6Qp/ujJNXRsrW6FHg4kFDV6Q/XhVyeppw83RRK0/u1HYRcxwCxzaRE5iR3x/kd3BpLdBMgrrf8rbZUXvKW+zV/jVrrq20WQ+vXrliKXIOGGHAub29vBiWGjGwUG9VFFmX6QSQNjTfcYT8u0+5md3MCFeKA6YjW8oYvB8mdSsombFyaoQ8kJpka/mT2HLpxj36sO+4TXsPegaybh2MJw1Hsik/TeQxZLkOVeAOhWNspaJei02GbKykqGN877pBDgNA9IoA==")
                    .sshCloneUrl("ssh://git@bitbucket.hensoldt.co.za:7999/gebamboo/config-scripts.git")
                    .sshKeyAppliesToSubmodules(true)
                    .changeDetection(new VcsChangeDetection()))
            
            .triggers(new BitbucketServerTrigger())
            .variables(new Variable("DT_API_TOKEN_PASSWORD",
                    "BAMSCRT@0@0@cFlv9wuhGAB+0nipEFyRGKXLMrNoCaK0Z7Fnsm4l5AHHCXqp8JwGNwG7bHuRzB2B"),
                new Variable("DT_API_URL",
                    "http://dependencytrack.hensoldt.co.za:8082"),
                new Variable("SONAR_TOKEN_PASSWORD",
                    "BAMSCRT@0@0@Ly00ddkBN3Y00yFkUxmYVJMR4e6ja6raY69aKO7J2QgB4cuD+7StdvvEDwzeIILQ"))
            .planBranchManagement(new PlanBranchManagement()
                    .createForVcsBranch()
                    .delete(new BranchCleanup()
                        .whenRemovedFromRepositoryAfterDays(1)
                        .whenInactiveInRepositoryAfterDays(30)));        
    }
    
    public PlanPermissions planPermission() {
        return new PlanPermissions(new PlanIdentifier("BECCBB", "IAABASICS"))
            .permissions(new Permissions()
                    .userPermissions("HG132095", PermissionType.VIEW, PermissionType.EDIT, PermissionType.BUILD, PermissionType.CLONE, PermissionType.ADMIN, PermissionType.VIEW_CONFIGURATION, PermissionType.CREATE_PLAN_BRANCH)
                    .loggedInUserPermissions(PermissionType.VIEW)
                    .anonymousUserPermissionView());        
    }
    
    public static void main(String... argv) {
        //By default credentials are read from the '.credentials' file.
        BambooServer bambooServer = new BambooServer("https://bamboo.hensoldt.co.za");
        final XmlViewerPlanSpec planSpec = new XmlViewerPlanSpec();
        
        final Plan plan = planSpec.plan();
        bambooServer.publish(plan);
        
        final PlanPermissions planPermission = planSpec.planPermission();
        bambooServer.publish(planPermission);
    }
}