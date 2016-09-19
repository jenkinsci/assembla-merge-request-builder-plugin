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
import org.jenkinsci.plugins.assembla.api.models.Space;
import org.jenkinsci.plugins.assembla.api.models.SpaceTool;
import org.jenkinsci.plugins.assembla.api.models.User;
import org.jenkinsci.plugins.assembla.cause.AssemblaCause;
import org.jenkinsci.plugins.assembla.cause.AssemblaMergeRequestCause;
import org.jenkinsci.plugins.assembla.cause.AssemblaPushCause;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by pavel on 16/2/16.
 */
public class AssemblaBuildTrigger extends Trigger<AbstractProject<?, ?>> {
    @Extension
    public static final AssemblaBuildTriggerDescriptor DESCRIPTOR = new AssemblaBuildTriggerDescriptor();
    private static final Logger LOGGER = Logger.getLogger(AssemblaBuildTrigger.class.getName());
    private static AssemblaClient assemblaClient =  new AssemblaClient(
        DESCRIPTOR.getBotApiKey(),
        DESCRIPTOR.getBotApiSecret(),
        DESCRIPTOR.getAssemblaHost(),
        DESCRIPTOR.isIgnoreSSLErrors()
    );

    private final String spaceName;
    private final String repoName;

    private String buildDescriptionTemplate;
    private String buildStartedTemplate;
    private String buildResultTemplate;

    private boolean buildOnMergeRequestEnabled;
    private boolean mergeRequestCommentsEnabled;
    private boolean ticketCommentsEnabled;
    private boolean notifyOnStartEnabled;

    private boolean triggerOnPushEnabled;

    private String branchesToBuild;

    private transient AssemblaBuildReporter buildReporter;

    @DataBoundConstructor
    public AssemblaBuildTrigger(String spaceName, String repoName,
                                boolean buildOnMergeRequestEnabled,
                                boolean mergeRequestCommentsEnabled,
                                boolean ticketCommentsEnabled,
                                boolean notifyOnStartEnabled,
                                boolean triggerOnPushEnabled,
                                String branchesToBuild,
                                String buildDescriptionTemplate,
                                String buildStartedTemplate,
                                String buildResultTemplate) {
        this.buildDescriptionTemplate = buildDescriptionTemplate;
        this.buildStartedTemplate = buildStartedTemplate;
        this.buildResultTemplate = buildResultTemplate;
        this.spaceName = spaceName.trim();
        this.repoName = repoName.trim();
        this.buildOnMergeRequestEnabled = buildOnMergeRequestEnabled;
        this.mergeRequestCommentsEnabled = mergeRequestCommentsEnabled;
        this.ticketCommentsEnabled = ticketCommentsEnabled;
        this.notifyOnStartEnabled = notifyOnStartEnabled;
        this.triggerOnPushEnabled = triggerOnPushEnabled;
        this.branchesToBuild = branchesToBuild.trim();
    }

    @Override
    public void start(AbstractProject<?, ?> project, boolean newInstance) {
        super.start(project, newInstance);

        String name = project.getFullName();

        if (project.isDisabled()) {
            LOGGER.info("Project is disabled, not starting trigger for job " + name);
            return;
        }

        DESCRIPTOR.addRepoTrigger(this, super.job);
        LOGGER.info("Trigger started for " + project.toString() + ". Repo name: " + repoName);
    }

    @Override
    public void stop() {
        if (!StringUtils.isEmpty(repoName)) {
            DESCRIPTOR.removeRepoTrigger(this, super.job);
        }
        super.stop();
    }

    public QueueTaskFuture<?> handleMergeRequest(AssemblaMergeRequestCause cause) {
        if (!buildOnMergeRequestEnabled) {
            return null;
        }
        Map<String, ParameterValue> values = getDefaultParameters(cause);

        values.put("assemblaMergeRequestId", new StringParameterValue("assemblaMergeRequestId", String.valueOf(cause.getMergeRequestId())));
        values.put("assemblaSourceRepositoryName", new StringParameterValue("assemblaSourceRepositoryName", cause.getSourceRepositoryName()));
        values.put("assemblaTargetRepositoryUrl", new StringParameterValue("assemblaTargetRepositoryUrl", cause.getTargetRepositoryUrl()));
        values.put("assemblaTargetBranch", new StringParameterValue("assemblaTargetBranch", cause.getTargetBranch()));

        List<ParameterValue> listValues = new ArrayList<>(values.values());
        return job.scheduleBuild2(0, cause, new SafeParametersAction(listValues));
    }

    public QueueTaskFuture<?> handlePush(AssemblaPushCause cause) {
        List<String> buildableBranches = Arrays.asList(branchesToBuild.split(","));

        if (triggerOnPushEnabled && (buildableBranches.contains(cause.getSourceBranch()) || branchesToBuild.isEmpty())) {
            Map<String, ParameterValue> values = getDefaultParameters(cause);

            List<ParameterValue> listValues = new ArrayList<>(values.values());
            return job.scheduleBuild2(0, cause, new SafeParametersAction(listValues));
        }

        return null;
    }

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

    public String getBuildDescriptionTemplate() {
        return buildDescriptionTemplate;
    }

    public String getBuildStartedTemplate() {
        return buildStartedTemplate;
    }

    public String getBuildResultTemplate() {
        return buildResultTemplate;
    }

    public boolean isMergeRequestCommentsEnabled() {
        return mergeRequestCommentsEnabled;
    }

    public boolean isTicketCommentsEnabled() {
        return ticketCommentsEnabled;
    }

    public boolean isNotifyOnStartEnabled() {
        return notifyOnStartEnabled;
    }

    public boolean isBuildOnMergeRequestEnabled() {
        return buildOnMergeRequestEnabled;
    }

    public boolean isTriggerOnPushEnabled() {
        return triggerOnPushEnabled;
    }

    public String getBranchesToBuild() {
        return branchesToBuild;
    }

    public AssemblaBuildReporter getBuildReporter() {
        if (buildReporter == null) {
            buildReporter = new AssemblaBuildReporter(this);
        }
        return buildReporter;
    }

    public void setBuildReporter(AssemblaBuildReporter reporter) {
        this.buildReporter = reporter;
    }

    public static AssemblaBuildTriggerDescriptor getDesc() {
        return DESCRIPTOR;
    }

    public static AssemblaClient getAssembla() {
        return assemblaClient.setConfig(
            getDesc().getBotApiKey(),
            getDesc().getBotApiSecret(),
            getDesc().getAssemblaHost(),
            getDesc().isIgnoreSSLErrors()
        );
    }

    public static void setAssembla(AssemblaClient client) {
        assemblaClient = client;
    }

    public static AssemblaBuildTrigger getTrigger(AbstractProject project) {
        Trigger trigger = project.getTrigger(AssemblaBuildTrigger.class);

        if (trigger == null || !(trigger instanceof AssemblaBuildTrigger)) {
            return null;
        }
        return (AssemblaBuildTrigger) trigger;
    }

    private Map<String, ParameterValue> getDefaultParameters(AssemblaCause cause) {
        Map<String, ParameterValue> values = new HashMap<>();
        ParametersDefinitionProperty definitionProperty = job.getProperty(ParametersDefinitionProperty.class);

        if (definitionProperty != null) {
            for (ParameterDefinition definition : definitionProperty.getParameterDefinitions()) {
                values.put(definition.getName(), definition.getDefaultParameterValue());
            }
        }

        values.put("assemblaRefName", new StringParameterValue("assemblaRefName", cause.getCommitId()));
        values.put("assemblaSourceSpaceId", new StringParameterValue("assemblaSourceSpaceId", cause.getSourceSpaceId()));
        values.put("assemblaSourceRepositoryUrl", new StringParameterValue("assemblaSourceRepositoryUrl", cause.getSourceRepositoryUrl()));
        values.put("assemblaSourceBranch", new StringParameterValue("assemblaSourceBranch", cause.getSourceBranch()));
        values.put("assemblaAuthorName", new StringParameterValue("assemblaAuthorName", cause.getAuthorName()));

        // The merge request description will be null if no description was entered
        String description = cause.getDescription();
        if (description == null) {
            values.put("assemblaDescription", new StringParameterValue("assemblaDescription", ""));
        } else {
            values.put("assemblaDescription", new StringParameterValue("assemblaDescription", description));
        }

        return values;
    }

    public static final class AssemblaBuildTriggerDescriptor extends TriggerDescriptor {
        private String assemblaHost = "https://www.assembla.com/";
        private String botApiKey = "";
        private Secret botApiSecret;

        private boolean ignoreSSLErrors;

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
            return "Assembla Build Triggers";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            botApiKey = formData.getString("botApiKey");
            botApiSecret = Secret.fromString(formData.getString("botApiSecret"));
            assemblaHost = formData.getString("assemblaHost");
            ignoreSSLErrors = formData.getBoolean("ignoreSSLErrors");

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

        public FormValidation doTestCredentials(@QueryParameter("botApiKey") String key,
                                                @QueryParameter("botApiSecret") String secret,
                                                @QueryParameter("ignoreSSLErrors") boolean ignoreSSLErrors,
                                                @QueryParameter("assemblaHost") String assemblaHost) {
            User user;
            try {
                user = new AssemblaClient(key, secret, assemblaHost, ignoreSSLErrors).getUser();
            } catch (AssemblaClient.UnauthorizedError ex) {
                return FormValidation.error("Invalid credentials");
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, "Failed to check credentials", ex);
                return FormValidation.error("Could not connect to Assembla API: " + ex.toString());
            }

            if (user == null) {
                return FormValidation.error("Could not perform test request to Assembla API");
            }

            return FormValidation.ok("Successfully logged in as: " + user.getLogin() + " (" + user.getName() + ")");
        }


        public FormValidation doCheckSettings(@QueryParameter("spaceName") String spaceName, @QueryParameter("repoName") String repoName) {
            if (spaceName == null || spaceName.isEmpty()) {
                return FormValidation.error("You must provide a space name");
            }

            if (repoName == null || repoName.isEmpty()) {
                return FormValidation.error("You must provide a space name");
            }

            Space space;
            try {
                space = getAssembla().getSpace(spaceName);
            } catch (AssemblaClient.UnauthorizedError ex) {
                return FormValidation.error("Unable to authenticate. Please check API credentials on Jenkins system configuration page");
            } catch (AssemblaClient.ForbiddenError ex) {
                return FormValidation.error("You do not have permissions to access this space. Are you a member?");
            } catch (AssemblaClient.NotFoundError ex) {
                return FormValidation.error("Could not find space  " + spaceName);
            }
            if (space == null) {
                return FormValidation.error("Failed to fetch space. Please check your connection settings");
            }

            SpaceTool spaceTool;
            try {
                spaceTool = getAssembla().getTool(spaceName, repoName);
            } catch (AssemblaClient.UnauthorizedError ex) {
                return FormValidation.error("Unable to authenticate. Please check API credentials on Jenkins system configuration page");
            } catch (AssemblaClient.ForbiddenError ex) {
                return FormValidation.error("You do not have permissions to access this tool");
            } catch (AssemblaClient.NotFoundError ex) {
                return FormValidation.error("Could not find repo with name " + repoName + " in space " + spaceName);
            }

            if (spaceTool == null) {
                return FormValidation.error("Failed to fetch space tool. Please check your connection settings");
            }

            return FormValidation.ok("It's all good!");
        }

        public void addRepoTrigger(AssemblaBuildTrigger trigger, AbstractProject<?, ?> project) {
            String projectKey = getProjectKey(trigger);
            if (project == null || StringUtils.isEmpty(projectKey)) {
                return;
            }
            LOGGER.info("Adding trigger for repo: " + projectKey);

            synchronized (repoJobs) {
                Set<AbstractProject<?, ?>> projects = repoJobs.get(projectKey);

                if (projects == null) {
                    projects = new HashSet<>();
                    repoJobs.put(projectKey, projects);
                }

                 projects.add(project);
            }
        }

        public void removeRepoTrigger(AssemblaBuildTrigger trigger, AbstractProject<?, ?> project) {
            String projectKey = getProjectKey(trigger);
            Set<AbstractProject<?, ?>> projects = repoJobs.get(projectKey);
            if (project == null || projects == null || StringUtils.isEmpty(projectKey)) {
                return;
            }
            LOGGER.info("Removing trigger for repo: " + projectKey);
            projects.remove(project);
        }

        public Set<AbstractProject<?, ?>> getRepoJobs(String spaceName, String repoName) {
            Set<AbstractProject<?, ?>> projects = repoJobs.get(getProjectKey(spaceName, repoName));

            if (projects == null) {
                projects = new HashSet<>();
            }

            return projects;
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

        private String getProjectKey(AssemblaBuildTrigger trigger) {
            return getProjectKey(trigger.getSpaceName(), trigger.getRepoName());
        }

        private String getProjectKey(String spaceName, String repoName) {
            return (spaceName + ":" + repoName).toLowerCase();
        }

        public String getAssemblaHost() {
            return assemblaHost;
        }

        public boolean isIgnoreSSLErrors() {
            return ignoreSSLErrors;
        }
    }
}
