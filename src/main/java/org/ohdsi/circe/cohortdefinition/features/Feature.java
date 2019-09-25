package org.ohdsi.circe.cohortdefinition.features;

import org.ohdsi.circe.cohortdefinition.domain.CdmDomain;

public interface Feature<T extends CdmDomain> {

    String name();
    String getTableName();
    String getExpression(String tableName);
    String getAlias();
}
