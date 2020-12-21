package com.github.luckygoldfish.maven.mergerepository.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.ogm.annotation.Relationship.INCOMING;

@Data
@EqualsAndHashCode(exclude = "dependencies")
@NodeEntity
public class MavenModel {
    @Id
    @GeneratedValue
    private Long id;

    private String groupId;

    private String artifactId;

    private String version;

    private String description;

    private String name;


    @JsonIgnore
    @Relationship(type = "dependency", direction = INCOMING)
    private List<MavenModel> dependencies = new ArrayList<>();

    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }
}
