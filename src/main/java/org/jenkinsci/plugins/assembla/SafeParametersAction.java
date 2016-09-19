package org.jenkinsci.plugins.assembla;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by pavel on 9/19/16.
 */
@Restricted(NoExternalUse.class)
public class SafeParametersAction extends ParametersAction {

    private List<ParameterValue> parameters;

    public SafeParametersAction(List<ParameterValue> parameters) {
        this.parameters = parameters;
    }

    public SafeParametersAction(ParameterValue... parameters) {
        this(Arrays.asList(parameters));
    }

    @Override
    public List<ParameterValue> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public ParameterValue getParameter(String name) {
        for (ParameterValue parameter : parameters) {
            if (parameter != null && parameter.getName().equals(name)) {
                return parameter;
            }
        }

        return null;
    }

    @Extension
    public static final class SafeParametersActionEnvironmentContributor extends EnvironmentContributor {

        @Override
        public void buildEnvironmentFor(Run r, EnvVars envs,TaskListener listener) throws IOException, InterruptedException {
            SafeParametersAction action = r.getAction(SafeParametersAction.class);
            if (action != null) {
                for (ParameterValue p : action.getParameters()) {
                    envs.putIfNotNull(p.getName(), String.valueOf(p.getValue()));
                }
            }
        }
    }
}
