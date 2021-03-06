-- Begin Observation Criteria
select C.person_id, C.observation_id as event_id, C.observation_date as start_date, DATEADD(d,1,C.observation_date) as END_DATE,
       C.visit_occurrence_id, C.observation_date as sort_date@additionalColumns
from 
(
  select o.* @ordinalExpression
  FROM @cdm_database_schema.OBSERVATION o
@codesetClause
) C
@joinClause
@whereClause
-- End Observation Criteria
