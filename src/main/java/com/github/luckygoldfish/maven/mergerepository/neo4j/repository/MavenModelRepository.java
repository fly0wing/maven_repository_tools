package com.github.luckygoldfish.maven.mergerepository.neo4j.repository;

import com.github.luckygoldfish.maven.mergerepository.neo4j.entity.MavenModel;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface MavenModelRepository extends Neo4jRepository<MavenModel, Long> {

    MavenModel findByGroupIdAndArtifactId(String groupId, String artifactId);

}