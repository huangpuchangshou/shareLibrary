package com.ssc.dpe.utils.exception

public class PipelineException extends RuntimeException  {
    protected String stageName
    public String getStageName() {
        return stageName
    }

    public setStageName(String tempName) {
        stageName = tempName
    }

    public PipelineException(errorMessage) {
        super(errorMessage)
    }
}
