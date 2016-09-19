package org.jenkinsci.plugins.assembla;

import org.jenkinsci.plugins.assembla.api.AssemblaClient;
import org.jenkinsci.plugins.assembla.api.models.SpaceTool;
import org.junit.*;

import static org.junit.Assert.*;

import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

/**
 * Created by pavel on 26/2/16.
 */
public class AssemblaWebhookTest {

    StaplerRequest req = mock(StaplerRequest.class);
    AssemblaClient client = mock(AssemblaClient.class);

    AssemblaWebhook webhook;
    BufferedReader br;

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        webhook = spy(new AssemblaWebhook());
        given(client.setConfig(anyString(), anyString(), anyString(), anyBoolean())).willCallRealMethod();
        given(client.getRepoByUrl(anyString(), anyString())).willReturn(mock(SpaceTool.class));
    }

    @Test
    public void testGetUrlName() throws Exception {
        assertEquals(webhook.getUrlName(), "assembla-webhook");
    }

    @Test
    public void testProcessesChangesetEvent() throws Exception {
        setPayload(AssemblaTestUtil.CHANGESET_PAYLOAD);
        webhook.doIndex(req, null);
        verify(webhook, times(1)).processChangesetEvent(any(WebhookPayload.class));
    }

    @Test
    public void testProcessesMergeRequestEvent() throws Exception {
        setPayload(AssemblaTestUtil.MR_PAYLOAD);
        webhook.doIndex(req, null);
        verify(webhook, times(1)).processMergeRequestEvent(any(WebhookPayload.class));
    }

    @Test
    public void testIgnoresOtherEvents() throws Exception {
        setPayload(AssemblaTestUtil.TICKET_PAYLOAD);
        webhook.doIndex(req, null);
        verify(webhook, never()).processMergeRequestEvent(any(WebhookPayload.class));
        verify(webhook, never()).processChangesetEvent(any(WebhookPayload.class));
    }

    @Test
    public void testReadsPayload() throws Exception {
        setPayload(AssemblaTestUtil.MR_PAYLOAD);
        webhook.doIndex(req, null);
        verify(br, times(1)).close();
    }

    private void setPayload(String payload) throws Exception{
        InputStream is = new ByteArrayInputStream(payload.getBytes());
        br = spy(new BufferedReader(new InputStreamReader(is)));
        given(req.getReader()).willReturn(br);
    }
}