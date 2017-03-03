package org.jenkinsci.plugins.assembla;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.assembla.api.AssemblaClient;
import org.jenkinsci.plugins.assembla.api.models.MergeRequest;
import org.jenkinsci.plugins.assembla.api.models.MergeRequestVersion;
import org.jenkinsci.plugins.assembla.api.models.Ticket;
import org.jenkinsci.plugins.assembla.cause.AssemblaMergeRequestCause;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by pavel on 18/2/16.
 */
public class AssemblaBuildReporter {
    private AssemblaBuildTrigger trigger;
    private static final Logger LOGGER = Logger.getLogger(AssemblaBuildReporter.class.getName());

    public AssemblaBuildReporter(AssemblaBuildTrigger trigger) {
        this.trigger = trigger;
    }

    public void onStarted(AbstractBuild build, TaskListener listener) {
        AssemblaMergeRequestCause cause = getCause(build);

        if (cause != null) {
            AssemblaClient client = AssemblaBuildTrigger.getAssembla();
            MergeRequest mr = client.getMergeRequest(
                cause.getSourceSpaceId(),
                cause.getSourceRepositoryName(),
                cause.getMergeRequestId()
            );

            if (trigger.isNotifyOnStartEnabled()) {
                String startedMessage = processTemplate(
                        trigger.getBuildStartedTemplate(),
                        build,
                        listener,
                        getVariables(cause, build, mr)
                );

                if (trigger.isTicketCommentsEnabled()) {
                    for (Ticket ticket : client.getMergeRequestTickets(mr)) {
                        client.createTicketComment(ticket, startedMessage);
                    }
                }

                if (trigger.isMergeRequestCommentsEnabled()) {
                    client.commentMergeRequest(mr, client.getLatestVersion(mr), startedMessage);
                }
            }

            try {
                String description = processTemplate(
                        trigger.getBuildDescriptionTemplate(),
                        build,
                        listener,
                        getVariables(cause, build, mr)
                );

                build.setDescription(description);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to set build description", e);
            }
        }
    }

    public void onCompleted(AbstractBuild build, TaskListener listener) {
        AssemblaMergeRequestCause cause = getCause(build);
        if (cause == null) {
            return;
        }

        AssemblaClient client = AssemblaBuildTrigger.getAssembla();

        MergeRequest mr = client.getMergeRequest(
                cause.getSourceSpaceId(),
                cause.getSourceRepositoryName(),
                cause.getMergeRequestId()
        );

        if (mr == null) {
            LOGGER.info("Could not find Merge Request");
            return;
        }

        Result result = build.getResult();

        String message = processTemplate(
                trigger.getBuildResultTemplate(),
                build,
                listener,
                getVariables(cause, build, mr)
        );

        if (trigger.isMergeRequestCommentsEnabled()) {
            MergeRequestVersion mrVersion = client.getLatestVersion(mr);

            client.commentMergeRequest(mr, mrVersion, message);

            if (result == Result.SUCCESS) {
                client.upVoteMergeRequest(mr, mrVersion);
            } else if (result == Result.FAILURE || result == Result.UNSTABLE) {
                client.downVoteMergeRequest(mr, mrVersion);
            }
        }

        if (trigger.isTicketCommentsEnabled()) {
          for (Ticket ticket : client.getMergeRequestTickets(mr)) {
              client.createTicketComment(ticket, message);
          }
        }

        LOGGER.info("Build result: " + result);
    }

    private AssemblaMergeRequestCause getCause(AbstractBuild build) {
        Cause cause = build.getCause(AssemblaMergeRequestCause.class);

        if (cause == null || !(cause instanceof AssemblaMergeRequestCause)) {
            return null;
        }

        return (AssemblaMergeRequestCause) cause;
    }

    private String getBuildUrl(AbstractBuild build) {
        return Jenkins.getInstance().getRootUrl() + build.getUrl();
    }

    private static Map<String, String> getEnvVars(AbstractBuild<?, ?> build, TaskListener listener) {
        Map<String, String> messageEnvVars = new HashMap<>();
        if (build != null) {
            messageEnvVars.putAll(build.getCharacteristicEnvVars());
            messageEnvVars.putAll(build.getBuildVariables());
            try {
                messageEnvVars.putAll(build.getEnvironment(listener));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Couldn't get Env Variables: ", e);
            }
        }
        return messageEnvVars;
    }

    private static String replaceMacros(AbstractBuild<?, ?> build, TaskListener listener, String inputString) {
        String returnString = inputString;
        if (build != null && inputString != null) {
            try {
                returnString = TokenMacro.expandAll(build, listener, inputString);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Couldn't replace macros in message: ", e);
            }
        }
        return returnString;
    }

    private Map<String, String> getVariables(AssemblaMergeRequestCause c, AbstractBuild b, MergeRequest mr) {
        Map<String, String> vars = new HashMap<>();
        vars.put("mrTitle", c.getTitle());
        vars.put("mrUrl", AssemblaBuildTrigger.getAssembla().getMergeRequestWebUrl(mr));
        vars.put("mrId", Integer.toString(c.getMergeRequestId()));
        vars.put("mrAbbrTitle", c.getAbbreviatedTitle());
        vars.put("jobName", b.getProject().getDisplayName());
        vars.put("buildUrl", getBuildUrl(b));
        if (b.getResult() != null) {
            vars.put("buildStatus", b.getResult().toString());
        }
        return vars;
    }

    private String processTemplate(String template, AbstractBuild build, TaskListener listener, Map<String,String> vars) {
        String result = Util.replaceMacro(template, vars);
        return replaceMacros(build, listener, result);
    }
}
