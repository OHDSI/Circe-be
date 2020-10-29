package org.ohdsi.circe.cohortdefinition.builders;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.circe.cohortdefinition.ConditionEra;
import org.ohdsi.circe.helper.ResourceHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.ohdsi.circe.cohortdefinition.builders.BuilderUtils.buildDateRangeClause;
import static org.ohdsi.circe.cohortdefinition.builders.BuilderUtils.buildNumericRangeClause;
import static org.ohdsi.circe.cohortdefinition.builders.BuilderUtils.getConceptIdsFromConcepts;

public class ConditionEraSqlBuilder<T extends ConditionEra> extends CriteriaSqlBuilder<T> {

  private final static String CONDITION_ERA_TEMPLATE = ResourceHelper.GetResourceAsString("/resources/cohortdefinition/sql/conditionEra.sql");
  // default columns are those that are specified in the template, and dont' need to be added if specifeid in 'additionalColumns'
  private final Set<CriteriaColumn> DEFAULT_COLUMNS = new HashSet<>(Arrays.asList(CriteriaColumn.START_DATE, CriteriaColumn.END_DATE));

  @Override
  protected Set<CriteriaColumn> getDefaultColumns() {
    return DEFAULT_COLUMNS;
  }

  @Override
  protected String getQueryTemplate() {

    return CONDITION_ERA_TEMPLATE;
  }

  @Override
  protected String getTableColumnForCriteriaColumn(CriteriaColumn column) {
    switch (column) {
      case DOMAIN_CONCEPT:
        return "C.condition_concept_id";
      case ERA_OCCURRENCES:
        return "C.condition_occurrence_count";
      case DURATION:
        return "(DATEDIFF(d,C.condition_era_start_date, C.condition_era_end_date))";
      default:
       throw new IllegalArgumentException("Invalid CriteriaColumn for Condition Era:" + column.toString());
    }
  }

  @Override
  protected String embedCodesetClause(String query, T criteria) {

    String codesetClause = "";
    if (criteria.codesetId != null) {
      codesetClause = String.format("where ce.condition_concept_id in (SELECT concept_id from  #Codesets where codeset_id = %d)", criteria.codesetId);
    }
    return StringUtils.replace(query, "@codesetClause", codesetClause);
  }

  @Override
  protected String embedOrdinalExpression(String query, T criteria, List<String> whereClauses) {

    // first
    if (criteria.first != null && criteria.first) {
      whereClauses.add("C.ordinal = 1");
      query = StringUtils.replace(query, "@ordinalExpression", ", row_number() over (PARTITION BY ce.person_id ORDER BY ce.condition_era_start_date, ce.condition_era_id) as ordinal");
    } else {
      query = StringUtils.replace(query, "@ordinalExpression", "");
    }
    return query;
  }

  @Override
  protected List<String> resolveJoinClauses(T criteria) {

    List<String> joinClauses = new ArrayList<>();

    // join to PERSON
    if (criteria.ageAtStart != null || criteria.ageAtEnd != null || (criteria.gender != null && criteria.gender.length > 0)) {
      joinClauses.add("JOIN @cdm_database_schema.PERSON P on C.person_id = P.person_id");
    }

    return joinClauses;
  }

  @Override
  protected List<String> resolveWhereClauses(T criteria) {

    List<String> whereClauses = new ArrayList<>();

    // eraStartDate
    if (criteria.eraStartDate != null) {
      whereClauses.add(buildDateRangeClause("C.condition_era_start_date", criteria.eraStartDate));
    }

    // eraEndDate
    if (criteria.eraEndDate != null) {
      whereClauses.add(buildDateRangeClause("C.condition_era_end_date", criteria.eraEndDate));
    }

    // occurrenceCount
    if (criteria.occurrenceCount != null) {
      whereClauses.add(buildNumericRangeClause("C.condition_occurrence_count", criteria.occurrenceCount));
    }

    // eraLength
    if (criteria.eraLength != null) {
      whereClauses.add(buildNumericRangeClause("DATEDIFF(d,C.condition_era_start_date, C.condition_era_end_date)", criteria.eraLength));
    }

    // ageAtStart
    if (criteria.ageAtStart != null) {
      whereClauses.add(buildNumericRangeClause("YEAR(C.condition_era_start_date) - P.year_of_birth", criteria.ageAtStart));
    }

    // ageAtEnd
    if (criteria.ageAtEnd != null) {
      whereClauses.add(buildNumericRangeClause("YEAR(C.condition_era_end_date) - P.year_of_birth", criteria.ageAtEnd));
    }

    // gender
    if (criteria.gender != null && criteria.gender.length > 0) {
      whereClauses.add(String.format("P.gender_concept_id in (%s)", StringUtils.join(getConceptIdsFromConcepts(criteria.gender), ",")));
    }

    return whereClauses;
  }
}
