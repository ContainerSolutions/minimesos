package com.containersolutions.mesoshelloworld.scheduler;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 */
public class Configuration {
    public static final String MESOS_MASTER = "--mesosMaster";
    public static final String EXECUTOR_IMAGE = "--executorImage";
    public static final String DEFAULT_EXECUTOR_IMAGE = "containersol/mesos-hello-world-executor";
    public static final String EXECUTOR_NUMBER = "--executorNumber";
    public static final String EXECUTOR_FORCE_PULL_IMAGE = "--executorForcePullImage";
    @Parameter(names = {MESOS_MASTER}, description = "The Mesos master IP", validateWith = NotEmptyString.class)
    private String mesosMaster = "";
    @Parameter(names = {EXECUTOR_IMAGE}, description = "The docker executor image to use.")
    private String executorImage = DEFAULT_EXECUTOR_IMAGE;
    @Parameter(names = {EXECUTOR_NUMBER}, description = "Number of executors")
    private Integer executorNumber = 3;
    @Parameter(names = {EXECUTOR_FORCE_PULL_IMAGE}, arity = 1, description = "Option to force pull the executor image.")
    private Boolean executorForcePullImage = false;

    @Parameter(names = {"--frameworkPrincipal"}, description = "The principal to authenticate as")
    private String frameworkPrincipal = null;
    @Parameter(names = {"--frameworkSecret"}, description = "The secret to authenticate with, if authenticating as a principal")
    private String frameworkSecret = null;

    public Configuration(String[] args) {
        final JCommander jCommander = new JCommander();
        jCommander.addObject(this);
        try {
            jCommander.parse(args); // Parse command line args into configuration class.
        } catch (com.beust.jcommander.ParameterException ex) {
            System.out.println(ex);
            jCommander.setProgramName("(Options preceded by an asterisk are required)");
            jCommander.usage();
            throw ex;
        }
    }

    public String getMesosMaster() {
        if (mesosMaster.isEmpty()) {
            throw new IllegalArgumentException("You must pass the mesos master IP address");
        }
        return mesosMaster;
    }

    public String getExecutorImage() {
        return executorImage;
    }

    public Integer getExecutorNumber() {
        return executorNumber;
    }

    public Boolean getExecutorForcePullImage() {
        return executorForcePullImage;
    }

    public String getFrameworkPrincipal() {
        return frameworkPrincipal;
    }

    public String getFrameworkSecret() {
        return frameworkSecret;
    }

    /**
     * Ensures that the string is not empty. Will strip spaces.
     */
    public static class NotEmptyString implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            if (value.replace(" ", "").isEmpty()) {
                throw new ParameterException("Parameter " + name + " cannot be empty");
            }
        }
    }
}
