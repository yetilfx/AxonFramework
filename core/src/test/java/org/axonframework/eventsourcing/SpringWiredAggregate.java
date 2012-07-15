package org.axonframework.eventsourcing;

import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.StubAggregate;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Allard Buijze
 */
public class SpringWiredAggregate extends StubAggregate implements InitializingBean {

    private int initializedCount;
    private String someProperty;

    public SpringWiredAggregate() {
    }

    public SpringWiredAggregate(AggregateIdentifier identifier) {
        super(identifier);
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        this.initializedCount++;
    }

    public void setSomeProperty(String value) {
        this.someProperty = value;
    }

    public int getInitializedCount() {
        return initializedCount;
    }

    public String getSomeProperty() {
        return someProperty;
    }
}
