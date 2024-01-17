package io.jenkins.plugins.AIErrorAnalyzer;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class AIErrorAnalysisBuilder extends Builder implements SimpleBuildStep {
    private final String service;

    private final String key;

    private final String templates;

    @DataBoundConstructor
    public AIErrorAnalysisBuilder(String service, String key, String templates) {
        this.service = service;
        this.key = key;
        this.templates = templates;
    }

    public String getService() {
        return service;
    }

    public String getKey() {
        return key;
    }

    public String getTemplates() {
        return templates;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        List<String> paresedTemplates = Arrays.asList(templates.split("\\r?\\n|\\r"));
        run.addAction(new AIErrorAnalysisAction(service, key, paresedTemplates));
        listener.getLogger().println("Selected AI Service " + service);
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public ListBoxModel services = new ListBoxModel().add("BotHub");

        public ListBoxModel doFillServiceItems() {
            return services;
        }

        public FormValidation doCheckKey(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.AIErrorAnalysisBuilder_DescriptorImpl_errors_missingKey());
            return FormValidation.ok();
        }

        public FormValidation doCheckTemplate(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.AIErrorAnalysisBuilder_DescriptorImpl_errors_missingTemplate());
            if (value.length() < 4)
                return FormValidation.warning(Messages.AIErrorAnalysisBuilder_DescriptorImpl_warnings_tooShort());
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.AIErrorAnalysisBuilder_DescriptorImpl_DisplayName();
        }
    }
}
