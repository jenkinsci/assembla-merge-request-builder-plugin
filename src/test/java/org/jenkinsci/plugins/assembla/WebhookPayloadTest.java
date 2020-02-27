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
    private static final String REPO_URL = "git@git.assembla.com:pavel-test.2.git";

    private WebhookPayload setupPayload(String wikiName, String mrStatus) throws Exception {
        return new WebhookPayload(
            "pavel test",
            mrStatus,
            "Merge request",
            "Merge Request 2945043: Redirect all old catalog pages to assembla.com/home",
            "Pavel Dotsulenko (pavel.d) updated Merge Request 2945043 (6): Redirect all old catalog pages to assembla.com/home [+0] [-0]\\n\\n    New Version (6) Created\\n",
            "pavel.d",
            "master",
            wikiName,
            "276dc190d87eff3d28fdfad2d1e6a08a672efe13"
        );
    }

    private WebhookPayload setupPayload(String wikiName) throws Exception {
        return setupPayload(wikiName, "updated");
    }

    private WebhookPayload setupPayload() throws Exception {
        return setupPayload(REPO_URL, "updated");
    }

    @Before
    public void setUp() throws Exception {
        payload = setupPayload();
    }

    /**
     * Possible wiki-name examples:
     *
     * git@git.assembla.com:qwerty^master-space.git
     * git@git.assembla.com:tester2/tester2-first-space.git
     * git@git.assembla.com:tester1-spaces-two.2.git
     *
     * http://git.assembla.com/vf-por-snippet
     *
     * https://subversion.assembla.com/svn/gpcgames^apoc-auto
     * https://subversion.assembla.com/svn/clubpages
     * https://subversion.assembla.com/svn/paw5.rower
     *
     * http://perforce.assembla.com/breakout:11601
     * http://perforce-us-east.assembla.com/assembla-inc/damian-space2:12405
     * http://perforce.assembla.com/afconsult^afconsult_ue4.ue4test:11271
     */
    @Test
    public void testCaretGitWikiName() throws Exception {
        payload = setupPayload("git@git.assembla.com:pavelportfolio^pavel-test.2.git");
        assertEquals(payload.getSpaceWikiName(), "pavel-test");
    }

    @Test
    public void testSlashedGitWikiName() throws Exception {
        payload = setupPayload("git@git.assembla.com:pavelportfolio/pavel-test.2.git");
        assertEquals(payload.getSpaceWikiName(), "pavel-test");
    }


    @Test
    public void testNamespacedSpaceWikiName() throws Exception {
        payload = setupPayload("git@eu-git.assembla.com:pavelportfolio/pavel-test.2.git");
        assertEquals(payload.getSpaceWikiName(), "pavel-test");
    }

    @Test
    public void testHttpGitWikiName() throws Exception {
        payload = setupPayload("http://git.assembla.com/vf-por-snippet");
        assertEquals(payload.getSpaceWikiName(), "vf-por-snippet");
    }

    @Test
    public void testSvnWikiName() throws Exception {
        payload = setupPayload("https://subversion.assembla.com/svn/clubpages");
        assertEquals(payload.getSpaceWikiName(), "clubpages");
    }

    @Test
    public void testCaretSvnWikiNam() throws Exception {
        payload = setupPayload("https://subversion.assembla.com/svn/gpcgames^apoc-auto");
        assertEquals(payload.getSpaceWikiName(), "apoc-auto");
    }

    @Test
    public void testNamespacedNameSvnWikiName() throws Exception {
        payload = setupPayload("https://subversion.assembla.com/svn/paw5.rower");
        assertEquals(payload.getSpaceWikiName(), "paw5");
    }

    @Test
    public void testP4WikiName() throws Exception {
        payload = setupPayload("http://perforce.assembla.com/breakout:11601");
        assertEquals(payload.getSpaceWikiName(), "breakout");
    }

    @Test
    public void testCaretP4WikiName() throws Exception {
        payload = setupPayload("http://perforce.assembla.com/afconsult^afconsult_ue4:11271");
        assertEquals(payload.getSpaceWikiName(), "afconsult_ue4");
    }

    @Test
    public void testNamespaceP4WikiName() throws Exception {
        payload = setupPayload("http://perforce.assembla.com/afconsult^afconsult_ue4.ue4test:11271");
        assertEquals(payload.getSpaceWikiName(), "afconsult_ue4");
    }

    @Test
    public void testSlashedP4WikiName() throws Exception {
        payload = setupPayload("http://perforce-us-east.assembla.com/assembla-inc/damian-space2:12405");
        assertEquals(payload.getSpaceWikiName(), "damian-space2");
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
        WebhookPayload payload = setupPayload(REPO_URL, "reopened");
        assertTrue(payload.shouldTriggerBuild());
    }

    @Test
    public void testMergedShouldTriggerBuild() throws Exception {
        WebhookPayload payload = setupPayload(REPO_URL, "merged");
        assertTrue(payload.shouldTriggerBuild());
    }

    @Test
    public void testIgnoredShouldTriggerBuild() throws Exception {
        WebhookPayload payload = setupPayload(REPO_URL, "ignored");
        assertTrue(payload.shouldTriggerBuild());
    }
}
