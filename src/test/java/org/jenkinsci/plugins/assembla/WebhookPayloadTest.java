package org.jenkinsci.plugins.assembla;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by pavel on 28/2/16.
 */
public class WebhookPayloadTest {

    WebhookPayload payload;

    @Before
    public void setUp() throws Exception {
        payload = new WebhookPayload(
            "pavel test",
            "updated",
            "Merge request",
            "Merge Request 2945043: Redirect all old catalog pages to assembla.com/home",
            "Pavel Dotsulenko (pavel.d) updated Merge Request 2945043 (6): Redirect all old catalog pages to assembla.com/home [+0] [-0]\\n\\n    New Version (6) Created\\n",
            "pavel.d",
            "master",
            "git@git.assembla.com:pavel-test.2.git",
            "276dc190d87eff3d28fdfad2d1e6a08a672efe13"
        );
    }

    @Test
    public void testCaretSpaceWikiName() throws Exception {
        payload = new WebhookPayload(
            "pavel test",
            "updated",
            "Merge request",
            "Merge Request 2945043: Redirect all old catalog pages to assembla.com/home",
            "Pavel Dotsulenko (pavel.d) updated Merge Request 2945043 (6): Redirect all old catalog pages to assembla.com/home [+0] [-0]\\n\\n    New Version (6) Created\\n",
            "pavel.d",
            "master",
            "git@git.assembla.com:pavelportfolio^pavel-test.2.git",
            "276dc190d87eff3d28fdfad2d1e6a08a672efe13"
        );

        assertEquals(payload.getSpaceWikiName(), "pavel-test");
    }

    @Test
    public void testSlashSpaceWikiName() throws Exception {
        payload = new WebhookPayload(
            "pavel test",
            "updated",
            "Merge request",
            "Merge Request 2945043: Redirect all old catalog pages to assembla.com/home",
            "Pavel Dotsulenko (pavel.d) updated Merge Request 2945043 (6): Redirect all old catalog pages to assembla.com/home [+0] [-0]\\n\\n    New Version (6) Created\\n",
            "pavel.d",
            "master",
            "git@git.assembla.com:pavelportfolio/pavel-test.2.git",
            "276dc190d87eff3d28fdfad2d1e6a08a672efe13"
        );

        assertEquals(payload.getSpaceWikiName(), "pavel-test");
    }


    @Test
    public void testNamespacedSpaceWikiName() throws Exception {
        payload = new WebhookPayload(
            "pavel test",
            "updated",
            "Merge request",
            "Merge Request 2945043: Redirect all old catalog pages to assembla.com/home",
            "Pavel Dotsulenko (pavel.d) updated Merge Request 2945043 (6): Redirect all old catalog pages to assembla.com/home [+0] [-0]\\n\\n    New Version (6) Created\\n",
            "pavel.d",
            "master",
            "git@eu-git.assembla.com:pavelportfolio/pavel-test.2.git",
            "276dc190d87eff3d28fdfad2d1e6a08a672efe13"
        );

        assertEquals(payload.getSpaceWikiName(), "pavel-test");
    }

    @Test
    public void testGetRepositoryUrl() throws Exception {
        assertEquals("git@git.assembla.com:pavel-test.2.git", payload.getRepositoryUrl());
    }

    @Test
    public void testGetSpace() throws Exception {
        assertEquals("pavel test", payload.getSpaceName());
    }

    @Test
    public void testGetMergeRequestId() throws Exception {
        assertEquals(2945043, (int)payload.getMergeRequestId());
    }

    @Test
    public void testIsMergeRequestEvent() throws Exception {
        assertTrue(payload.isMergeRequestEvent());
    }

    @Test
    public void testIsChangesetEvent() throws Exception {
        assertFalse(payload.isChangesetEvent());
    }

    @Test
    public void testShouldTriggerBuild() throws Exception {
        assertTrue(payload.shouldTriggerBuild());
    }

    @Test
    public void testGetSpaceWikiName() throws Exception {
        assertEquals(payload.getSpaceWikiName(), "pavel-test");
    }

    @Test
    public void testReopenShouldTriggerBuild() throws Exception {
        WebhookPayload reopenPayload = new WebhookPayload(
                "pavel test",
                "reopened",
                "Merge request",
                "Merge Request 2945043: Redirect all old catalog pages to assembla.com/home",
                "Pavel Dotsulenko (pavel.d) updated Merge Request 2945043 (6): Redirect all old catalog pages to assembla.com/home [+0] [-0]\\n\\n    New Version (6) Created\\n",
                "pavel.d",
                "master",
                "git@git.assembla.com:pavel-test.2.git",
                "276dc190d87eff3d28fdfad2d1e6a08a672efe13"
        );
        assertTrue(reopenPayload.shouldTriggerBuild());
    }

    @Test
    public void testMergedShouldTriggerBuild() throws Exception {
        WebhookPayload payload = new WebhookPayload(
                "pavel test",
                "merged",
                "Merge request",
                "Merge Request 2945043: Redirect all old catalog pages to assembla.com/home",
                "Pavel Dotsulenko (pavel.d) updated Merge Request 2945043 (6): Redirect all old catalog pages to assembla.com/home [+0] [-0]\\n\\n    New Version (6) Created\\n",
                "pavel.d",
                "master",
                "git@git.assembla.com:pavel-test.2.git",
                "276dc190d87eff3d28fdfad2d1e6a08a672efe13"
        );
        assertTrue(payload.shouldTriggerBuild());
    }

    @Test
    public void testIgnoredShouldTriggerBuild() throws Exception {
        WebhookPayload payload = new WebhookPayload(
                "pavel test",
                "ignored",
                "Merge request",
                "Merge Request 2945043: Redirect all old catalog pages to assembla.com/home",
                "Pavel Dotsulenko (pavel.d) updated Merge Request 2945043 (6): Redirect all old catalog pages to assembla.com/home [+0] [-0]\\n\\n    New Version (6) Created\\n",
                "pavel.d",
                "master",
                "git@git.assembla.com:pavel-test.2.git",
                "276dc190d87eff3d28fdfad2d1e6a08a672efe13"
        );
        assertTrue(payload.shouldTriggerBuild());
    }
}
