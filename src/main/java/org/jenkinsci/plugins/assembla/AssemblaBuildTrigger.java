package org.jenkinsci.plugins.assembla;

import hudson.Extension;
import hudson.model.*;
import hudson.model.queue.QueueTaskFuture;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.assembla.api.AssemblaClient;
import org.jenkinsci.plugins.assembla.api.models.MergeRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by pavel on 16/2/16.
 */
public class AssemblaBuildTrigger extends Trigger<AbstractProject<?, ?>> {
    private static final Logger LOGGER = Logger.getLogger(AssemblaBuildTrigger.class.getName());

    private final String spaceName;
    private final String repoName;

    @DataBoundConstructor
    public AssemblaBuildTrigger(String spaceName, String repoName) {
        this.spaceName = spaceName;
        this.repoName = repoName;
    }

    @Override
    public void start(AbstractProject<?, ?> project, boolean newInstance) {
        super.start(project, newInstance);

        String name = project.getFullName();

        LOGGER.info("Trigger started for " + project.toString() + ". Repo name: " + repoName);

        if (project.isDisabled()) {
            LOGGER.info("Project is disabled, not starting trigger for job " + name);
            return;
        }

        DESCRIPTOR.addRepoTrigger(repoName, super.job);
    }

    @Override
    public void stop() {
        LOGGER.info("Trigger stopped. Repo name: " + repoName);
        if (!StringUtils.isEmpty(repoName)) {
            DESCRIPTOR.removeRepoTrigger(repoName, super.job);
        }
        super.stop();
    }

    public QueueTaskFuture<?> startJob(AssemblaCause cause) {
        Map<String, ParameterValue> values = getDefaultParameters();

        values.put("assemblaMergeRequestId", new StringParameterValue("assemblaMergeRequestId", String.valueOf(cause.getMergeRequestId())));
        values.put("assemblaSourceSpaceId", new StringParameterValue("assemblaSourceSpaceId", cause.getSourceSpaceId()));
        values.put("assemblaSourceRepository", new StringParameterValue("assemblaSourceRepository", cause.getSourceRepository()));
        values.put("assemblaSourceBranch", new StringParameterValue("assemblaSourceBranch", cause.getSourceBranch()));
        values.put("assemblaTargetBranch", new StringParameterValue("assemblaTargetBranch", cause.getTargetBranch()));
        values.put("assemblaDescription", new StringParameterValue("assemblaDescription", cause.getDescription()));

        List<ParameterValue> listValues = new ArrayList<>(values.values());
        return job.scheduleBuild2(0, cause, new ParametersAction(listValues));
    }

    private Map<String, ParameterValue> getDefaultParameters() {
        Map<String, ParameterValue> values = new HashMap<>();
        ParametersDefinitionProperty definitionProperty = job.getProperty(ParametersDefinitionProperty.class);

        if (definitionProperty != null) {
            for (ParameterDefinition definition : definitionProperty.getParameterDefinitions()) {
                values.put(definition.getName(), definition.getDefaultParameterValue());
            }
        }

        return values;
    }

    @Extension
    public static final AssemblaBuildTriggerDescriptor DESCRIPTOR = new AssemblaBuildTriggerDescriptor();

    @Override
    public AssemblaBuildTriggerDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public String getRepoName() {
        return repoName;
    }

    public void handleMergeRequest(AssemblaCause cause) {
        LOGGER.info("Handling merge request");
        LOGGER.info("Space name: " + spaceName);
        LOGGER.info("Repo name: " + repoName);
        LOGGER.info("Job name: " + job.getFullDisplayName());
        startJob(cause);
    }

    public static AssemblaBuildTriggerDescriptor getDesc() {
        return DESCRIPTOR;
    }

    public static AssemblaClient getAssembla() {
        return new AssemblaClient(
                DESCRIPTOR.getBotApiKey(),
                DESCRIPTOR.getBotApiSecret()
        );
    }

    public static final class AssemblaBuildTriggerDescriptor extends TriggerDescriptor {
        private String botApiKey = "";
        private Secret botApiSecret;
        private String successMessage = "Build finished.  Tests PASSED.";
        private String unstableMessage = "Build finished.  Tests FAILED.";
        private String failureMessage = "Build finished.  Tests FAILED.";

        private transient final Map<String, Set<AbstractProject<?, ?>>> repoJobs;

        public AssemblaBuildTriggerDescriptor() {
            load();
            repoJobs = new ConcurrentHashMap<>();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof AbstractProject;
        }

        @Override
        public String getDisplayName() {
            return "Assembla Merge Requests Builder";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            botApiKey = formData.getString("botApiKey");
            botApiSecret = Secret.fromString(formData.getString("botApiSecret"));
            successMessage = formData.getString("successMessage");
            unstableMessage = formData.getString("unstableMessage");
            failureMessage = formData.getString("failureMessage");

            save();

            return super.configure(req, formData);
        }

        public FormValidation doCheckBotApiKey(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error("You must provide an API key for the Jenkins user");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckBotApiSecret(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error("You must provide an API secret for the Jenkins user");
            }

            return FormValidation.ok();
        }

        public String getBotApiKey() {
            return botApiKey;
        }

        public String getBotApiSecret() {
            if (botApiSecret == null) {
                return "";
            }
            return botApiSecret.getPlainText();
        }

        public String getSuccessMessage() {
            if (successMessage == null) {
                successMessage = "Build finished.  Tests PASSED.";
            }
            return successMessage;
        }

        public String getUnstableMessage() {
            if (unstableMessage == null) {
                unstableMessage = "Build finished.  Tests FAILED.";
            }
            return unstableMessage;
        }

        public String getFailureMessage() {
            if (failureMessage == null) {
                failureMessage = "Build finished.  Tests FAILED.";
            }
            return failureMessage;
        }

        public void addRepoTrigger(String repoName, AbstractProject<?, ?> project) {
            if (project == null || StringUtils.isEmpty(repoName)) {
                LOGGER.info("Not adding a trigger");
                LOGGER.info("project is: " + project);
                LOGGER.info("repo name is: " + repoName);
                return;
            }
            LOGGER.info("Adding trigger for repo: " + repoName);

            synchronized (repoJobs) {
                Set<AbstractProject<?, ?>> projects = repoJobs.get(repoName);

                if (projects == null) {
                    projects = new HashSet<AbstractProject<?, ?>>();
                    repoJobs.put(repoName, projects);
                }

                // TODO: Use tool ID instead of repo name, because it's not unique between projects
                 projects.add(project);
            }
        }

        public void removeRepoTrigger(String repoName, AbstractProject<?, ?> project) {
            Set<AbstractProject<?, ?>> projects = repoJobs.get(repoName);
            if (project == null || projects == null || StringUtils.isEmpty(repoName)) {
                return;
            }
            LOGGER.info("Removing trigger for repo: " + repoName);
            // TODO: Use tool ID instead of repo name, because it's not unique between projects
            projects.remove(repoName);
        }

        public Set<AbstractProject<?, ?>> getRepoTriggers(String repoName) {
            Set<AbstractProject<?, ?>> projects = repoJobs.get(repoName);

            if (projects == null) {
                projects = new HashSet<>();
            }

            return projects;
        }
    }

}