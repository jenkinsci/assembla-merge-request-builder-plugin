package org.jenkinsci.plugins.assembla;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.assembla.api.AssemblaClient;
import org.jenkinsci.plugins.assembla.api.models.MergeRequest;
import org.jenkinsci.plugins.assembla.api.models.MergeRequestVersion;
import org.jenkinsci.plugins.assembla.api.models.Ticket;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by pavel on 28/2/16.
 */
public class AssemblaBuildReporterTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    AssemblaClient client = mock(AssemblaClient.class);
    FreeStyleProject project;
    AssemblaBuildTrigger trigger;
    AssemblaBuildReporter reporter;
    AbstractBuild build;

    MergeRequest mr;
    MergeRequestVersion mrVersion;
    Ticket ticket;

    @Before
    public void setUp() throws Exception {
        AssemblaBuildTrigger.setAssembla(client);
        project = jenkinsRule.createFreeStyleProject("testJob");
        trigger = spy(AssemblaTestUtil.getTrigger());
        reporter = new AssemblaBuildReporter(trigger);
        build = spy(project.scheduleBuild2(0, AssemblaTestUtil.getMergeRequestCause()).get());

        mr = mock(MergeRequest.class);
        mrVersion = mock(MergeRequestVersion.class);
        ticket = mock(Ticket.class);

        given(client.setConfig(anyString(), anyString(), anyString(), anyBoolean())).willCallRealMethod();
        given(client.getMergeRequest(anyString(), anyString(), anyInt())).willReturn(mr);
        given(client.getLatestVersion(any(MergeRequest.class))).willReturn(mrVersion);
        given(client.getMergeRequestTickets(any(MergeRequest.class))).willReturn(Arrays.asList(ticket));
    }

    @Test
    public void testOnStarted() throws Exception {
        reporter.onStarted(build, mock(TaskListener.class));
        verify(build, times(1)).setDescription(anyString());
        verify(client, times(1)).createTicketComment(any(Ticket.class), anyString());
        verify(client, times(1)).commentMergeRequest(eq(mr), eq(mrVersion), eq("testJob #1 build started"));
    }

    @Test
    public void testUpVotesOnSuccess() throws Exception {
        given(build.getResult()).willReturn(Result.SUCCESS);
        reporter.onCompleted(build, mock(TaskListener.class));
        verify(client, times(1)).createTicketComment(eq(ticket), anyString());
        verify(client, times(1)).commentMergeRequest(eq(mr), eq(mrVersion), eq("testJob #1 build finished with status: SUCCESS"));
        verify(client, times(1)).upVoteMergeRequest(eq(mr), eq(mrVersion));
    }

    @Test
    public void testDownVotesOnFailure() throws Exception {
        given(build.getResult()).willReturn(Result.FAILURE);
        reporter.onCompleted(build, mock(TaskListener.class));
        verify(client, times(1)).createTicketComment(eq(ticket), anyString());
        verify(client, times(1)).commentMergeRequest(eq(mr), eq(mrVersion), eq("testJob #1 build finished with status: FAILURE"));
        verify(client, times(1)).downVoteMergeRequest(eq(mr), eq(mrVersion));
    }
}