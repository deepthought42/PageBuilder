package com.looksee.pageBuilder.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.journeys.Step;


@Repository
public interface StepRepository extends Neo4jRepository<Step, Long>{

	@Query("MATCH (step:Step{key:$key}) RETURN step LIMIT 1")
	public Step findByKey(@Param("key") String step_key);

	@Query("MATCH (:ElementInteractionStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementState(@Param("step_key") String step_key);

	@Query("MATCH (s:Step) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN s")
	public Step addStartPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:Step) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN s")
	public Step addEndPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:Step) WITH s MATCH (p:ElementState) WHERE id(s)=$step_id AND id(p)=$element_state_id MERGE (s)-[:HAS]->(p) RETURN p")
	public ElementState addElementState(@Param("step_id") long id, @Param("element_state_id") long element_state_id);
}
