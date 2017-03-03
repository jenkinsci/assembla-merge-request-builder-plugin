package org.jenkinsci.plugins.assembla;

import net.sf.json.JSONObject;
import org.jenkinsci.plugins.assembla.cause.AssemblaMergeRequestCause;
import org.jenkinsci.plugins.assembla.cause.AssemblaPushCause;
import org.kohsuke.stapler.BindInterceptor;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.WebApp;
import org.mockito.Mockito;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

/**
 * Created by pavel on 1/3/16.
 */
public class AssemblaTestUtil {
    private static StaplerRequest req;

    public static String CHANGESET_PAYLOAD = "{" +
            "\"space\": \"pavel-test\", " +
            "\"action\": \"updated\", " +
            "\"object\": \"Changeset\", " +
            "\"title\": \"Commit title\", " +
            "\"body\": \"Event Body\", " +
            "\"author\": \"pavel.d\", " +
            "\"repository_suffix\": \"2\", " +
            "\"repository_url\": \"git@git.assembla.com:pavel-test.2.git\", " +
            "\"branch\": \"master\", " +
            "\"commit_id\": \"276dc190d87eff3d28fdfad2d1e6a08a672efe13\"" +
            "}";

    public static String MR_PAYLOAD = "{" +
            "\"space\": \"pavel-test\", " +
            "\"action\": \"updated\", " +
            "\"object\": \"Merge request\", " +
            "\"title\": \"Re: Merge Request 2945043: Redirect all old catalog pages to assembla.com/home\", " +
            "\"body\": \"Pavel Dotsulenko (pavel.d) updated Merge Request 2945043 (6): Redirect all old catalog pages to assembla.com/home [+0] [-0]\\n\\n    New Version (6) Created\\n\", " +
            "\"author\": \"pavel.d\", " +
            "\"repository_suffix\": \"2\", " +
            "\"repository_url\": \"git@git.assembla.com:pavel-test.2.git\", " +
            "\"branch\": \"master\", " +
            "\"commit_id\": \"276dc190d87eff3d28fdfad2d1e6a08a672efe13\"" +
     "}";

    public static String TICKET_PAYLOAD = "{" +
            "\"space\": \"pavel-test\", " +
            "\"action\": \"created\", " +
            "\"object\": \"Ticket\", " +
            "\"title\": \"Some title\", " +
            "\"body\": \"Content\", " +
            "\"author\": \"pavel.d\", " +
            "\"repository_suffix\": \"---\", " +
            "\"repository_url\": \"---\", " +
            "\"branch\": \"---\", " +
            "\"commit_id\": \"---\"" +
            "}";

    public static void setupAssemblaTriggerDescriptor() throws Exception {
        setupReq();

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("botApiKey", "xxx");
        jsonObject.put("botApiSecret", "yyy");
        jsonObject.put("buildDescriptionTemplate", "MR <a title=\"$mrTitle\" href=\"$mrUrl\">#$mrId</a>: $mrAbbrTitle\"");
        jsonObject.put("buildResultTemplate", "$jobName finished with status: $buildStatus");
        jsonObject.put("buildStartedTemplate", "Build started, monitor at $buildUrl");
        jsonObject.put("assemblaHost", "https://www.assembla.com");
        jsonObject.put("ignoreSSLErrors", false);

        AssemblaBuildTrigger.getDesc().configure(req, jsonObject);

    }

    public static AssemblaBuildTrigger getTrigger() throws Exception {
        return new AssemblaBuildTrigger("space-name", "git", true, true, true, true, true, "master", "", "$jobName #$BUILD_NUMBER build started", "$jobName #$BUILD_NUMBER build finished with status: $buildStatus");
    }

    public static AssemblaPushCause getPushCause() {
        return new AssemblaPushCause(
                "git@git.assembla.com:pavel-test.git",
                "git",
                "master",
                "276dc190d87eff3d28fdfad2d1e6a08a672efe13",
                "Title",
                "Description",
                "12345",
                "Author"
        );
    }

    public static AssemblaMergeRequestCause getMergeRequestCause() {
        return new AssemblaMergeRequestCause(
            12345,
            "git@git.assembla.com:pavel-fork.git",
            "git",
            "develop",
            "git@git.assembla.com:pavel-test.git",
            "master",
            "276dc190d87eff3d28fdfad2d1e6a08a672efe13",
            "Description",
            "12345",
            "Redirect all old catalog pages to assembla.com/home",
            "Author"
        );
    }
    
    public static AssemblaMergeRequestCause getMergeRequestCauseWithoutDescription() {
        return new AssemblaMergeRequestCause(
            12345,
            "git@git.assembla.com:pavel-fork.git",
            "git",
            "develop",
            "git@git.assembla.com:pavel-test.git",
            "master",
            "276dc190d87eff3d28fdfad2d1e6a08a672efe13",
            null,
            "12345",
            "Redirect all old catalog pages to assembla.com/home",
            "Author"
        );
    }

    @SuppressWarnings("unchecked")
    private static void setupReq() {
        req = Mockito.mock(RequestImpl.class);
        given(req.getWebApp()).willReturn(mock(WebApp.class));
        given(req.bindJSON(any(Class.class), any(JSONObject.class))).willCallRealMethod();
        given(req.bindJSON(any(Class.class), any(Class.class), any(JSONObject.class))).willCallRealMethod();
        given(req.setBindInterceptpr(any(BindInterceptor.class))).willCallRealMethod();
        req.setBindInterceptpr(BindInterceptor.NOOP);
    }
}
