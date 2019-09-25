package org.ohdsi.circe.cohortdefinition.features;

public enum VisitOccurrenceFeature implements Feature {

    LENGTH_OF_STAY("DATEDIFF(dd, ISNULL(@tableName.visit_start_datetime, @tableName.visit_start_date), ISNULL(@tableName.visit_end_datetime, @tableName.visit_end_date))");

    private final static String tableName = "visit_occurrence";
    private String expression;

    VisitOccurrenceFeature(String expression) {

        this.expression = expression;
    }

    @Override
    public String getTableName() {

        return tableName;
    }

    @Override
    public String getExpression(String tableName) {

        return expression.replaceAll("@tableName", tableName);
    }

    @Override
    public String getAlias() {

        return this.name();
    }
}
