-- Begin Death Criteria
select C.person_id, C.person_id as event_id, C.death_date as start_date, DATEADD(d,1,C.death_date) as end_date,
  CAST(NULL as bigint) as visit_occurrence_id, C.death_date as sort_date@additionalColumns
from 
(
  select d.*
  FROM @cdm_database_schema.DEATH d
@codesetClause
) C
@joinClause
@whereClause
-- End Death Criteria

