package io.jenkins.plugins.AIErrorAnalyzer;

import hudson.model.Run;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.model.RunAction2;

public class AIErrorAnalysisAction implements RunAction2 {

    private transient Run run;

    private static Logger logger = Logger.getLogger(AIErrorAnalysisAction.class.getName());

    private AIIntegration aiIntegration;

    private List<String> errorTemplates;

    private ArrayList<String> analyzerResponses;

    public boolean isResponseRead = false;

    public AIErrorAnalysisAction(String serviceName, String apiKey, List<String> templates) {
        this.aiIntegration = new AIIntegration(serviceName, apiKey);
        this.errorTemplates = templates;
        this.analyzerResponses = new ArrayList<String>();
    }

    public void readResponse() {
        isResponseRead = true;
    }

    public String getAnalyzerResponse() {
        if (analyzerResponses.size() > 0) {
            return analyzerResponses.get(0);
        }
        return "Pipiline is still working. Please wait until the result is ready.";
    }

    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @Override
    public String getDisplayName() {
        return "AI assistant";
    }

    @Override
    public String getUrlName() {
        return "ai_assistant";
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    public Run getRun() {
        return run;
    }

    public void requestAIAnalyze() throws Exception {
        String errorDetails = this.readLog();

        if (errorDetails.isBlank()) {
            analyzerResponses.add("Build finished succesfully. Nothing to analyze");
        } else {
            System.out.println("Requesting from AI Analyzer...");
            String response = aiIntegration.getResponse(errorDetails);
            analyzerResponses.add(response);
        }
    }

    public String readLog() throws IOException {
        boolean errorFound = false;
        String errorDetails = "";

        try (Reader r = this.run.getLogReader();
                BufferedReader br = new BufferedReader(r)) {
            String line;
            while ((line = br.readLine()) != null) {
                for (String template : errorTemplates) {
                    Pattern pattern = Pattern.compile(template);
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        errorFound = true;
                        errorDetails = line;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!errorFound) {
            System.out.println("Warning: No 'Error' found in the log.");
        }

        return errorDetails;
    }
}
