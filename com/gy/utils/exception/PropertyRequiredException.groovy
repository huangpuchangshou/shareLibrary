package com.gy.utils.exception

public class PropertyRequiredException extends PipelineException  {

    public PropertyRequiredException(String stage, String properName) {
        super(stage + " Stage need define the value for the property '" + properName + "' in the pipelineConfig.yml file");
    }
}
