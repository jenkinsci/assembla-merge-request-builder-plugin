package org.jenkinsci.plugins.assembla;

import com.google.gson.annotations.SerializedName;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pavel on 16/2/16.
 * Updated by jlentink on 13/7/2018
 */
public class WebhookPayload {
    private static final Pattern mergeRequestIdPattern = Pattern.compile("Merge Request (\\d+)");
    private static final Pattern wikiNamePattern = Pattern.compile("^git@git\\.assembla\\.com:(?:[a-z0-9\\_-]+[\\^/])?([a-z0-9\\_-]+).*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern wikiAltNamePattern = Pattern.compile("^git@([a-z\\_-]*)git\\.assembla\\.com:(?:[a-z0-9\\_-]+[\\^/])?([a-z0-9\\_-]+).*$", Pattern.CASE_INSENSITIVE);
    private static final Logger LOGGER = Logger.getLogger(WebhookPayload.class.getName());

    private String space;
    private String action;
    private String object;
    private String title;
    private String body;
    private String author;
    private String branch;
    @SerializedName("repository_url")
    private String repositoryUrl;
    @SerializedName("commit_id")
    private String commitId;

    //  Payload example:
    //  {
    //    "space": "name",
    //    "action": "created",
    //    "object": "Merge request",
    //    "title": "Merge Request 2883693: Fix Helpdesk session issue",
    //    "body": "Pavel Dotsulenko (pavel.d) created Merge Request 2883693 (1): Fix Helpdesk session issue [+0] [-0]\n\n    some desc\n",
    //    "author": "pavel.d",
    //    "repository_suffix": "5",
    //    "repository_url": "git@git.assembla.com:232213.5.git",
    //    "branch": "assembla-2d6a0afde0",
    //    "commit_id": "2d6a0afde055f27fd0764b5fe525f926b4090360"
    //  }
    //
    //  {
    //    "space": "subpro",
    //    "action": "committed",
    //    "object": "Changeset",
    //    "title": "Changeset [2d77b15e83f]: Test",
    //    "body": "Test\n\nBranch: develop\nCommit from user: pavel.d",
    //    "author": "pavel.d",
    //    "repository_suffix": "origin",
    //    "repository_url": "git@git.assembla.com:subpro.git",
    //    "branch": "develop",
    //    "commit_id": "2d77b15e83f"
    //  }
    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public WebhookPayload(String space, String action, String object, String title, String body, String author, String branch, String repositoryUrl, String commitId) {
        this.space = space;
        this.action = action;
        this.object = object;
        this.title = title;
        this.body = body;
        this.author = author;
        this.branch = branch;
        this.repositoryUrl = repositoryUrl;
        this.commitId = commitId;
    }

    public String getSpaceName() {
        return space;
    }

    public String getSpaceWikiName() {
        Matcher m = wikiNamePattern.matcher(repositoryUrl);
        Matcher mAlt = wikiAltNamePattern.matcher(repositoryUrl);
        String wikiName = "";

        try {
            if (mAlt.matches()){
                LOGGER.info(mAlt.group(2));
                wikiName = mAlt.group(2);
            } else if (m.matches()) {
                LOGGER.info(m.group(1));
                wikiName = m.group(1);
            }

        } catch (IllegalStateException | NumberFormatException ex) {
            LOGGER.log(Level.SEVERE, "Failed to parse space wiki name", ex);
        }

        return wikiName;
    }

    public String getAction() {
        return action;
    }

    public String getObject() {
        return object;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getAuthor() {
        return author;
    }

    public String getBranch() {
        return branch;
    }

    public String getCommitId() {
        return commitId;
    }

    public Integer getMergeRequestId() {
        Matcher m = mergeRequestIdPattern.matcher(title);
        Integer mergeRequestId = null;

        try {
            if (m.find()) {
                mergeRequestId = Integer.parseInt(m.group(1));
            }

        } catch (IllegalStateException | NumberFormatException ex) {
            LOGGER.log(Level.SEVERE, "Failed to parse merge request ID", ex);
        }

        return mergeRequestId;
    }

    public boolean isMergeRequestEvent() {
        return object.equals("Merge request");
    }

    public boolean isChangesetEvent() {
        return object.equals("Changeset");
    }

    public boolean shouldTriggerBuild() {
        return isChangesetEvent() || isMergeRequestEvent();
    }

    @Override
    public String toString() {
        return "WebhookPayload{" +
            "space='" + space + '\'' +
            ", action='" + action + '\'' +
            ", object='" + object + '\'' +
            ", title='" + title + '\'' +
            ", body='" + body + '\'' +
            ", author='" + author + '\'' +
            ", branch='" + branch + '\'' +
            ", commitId='" + commitId + '\'' +
            '}';
    }
}
