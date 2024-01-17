package io.jenkins.plugins.AIErrorAnalyzer;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

@Extension
public class AIErrorAnalysisJobListener extends RunListener<Run<?, ?>> {
    @Override
    public void onCompleted(Run<?, ?> run, TaskListener listener) {
        System.out.println("Job completed: " + run.getFullDisplayName());

        AIErrorAnalysisAction action = run.getAction(AIErrorAnalysisAction.class);

        if (action != null) {
            try {
                action.requestAIAnalyze();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
