package com.hensoldt.bamboo;

import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.exceptions.PropertiesValidationException;
import com.atlassian.bamboo.specs.api.util.EntityPropertiesBuilders;
import com.hensoldt.bamboo.backend_components.*;
import org.junit.Test;

public class PlanSpecTest {
    @Test
    public void checkYourPlanOffline() throws PropertiesValidationException {        
        Plan plan = new XmlViewerPlanSpec().plan();

        EntityPropertiesBuilders.build(plan);

        Plan plan1 = new IAABasics().plan();
        EntityPropertiesBuilders.build(plan1);
    }
}
