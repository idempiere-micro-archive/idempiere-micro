package org.idempiere.orm;

import java.util.List;

public interface IEvent {
    List<String> getProperty(String eventErrorMessages);
}
