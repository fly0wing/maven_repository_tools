package com.github.luckygoldfish.maven.mergerepository.handler;

import com.github.luckygoldfish.maven.mergerepository.Application;
import com.github.luckygoldfish.maven.mergerepository.neo4j.entity.MavenModel;
import com.github.luckygoldfish.maven.mergerepository.neo4j.repository.MavenModelRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Slf4j
public class WriteNeo4jHandler implements CliHandler {
    private final String m2RepositoryDir;
    private final Path m2RepositoryDirPath;

    public WriteNeo4jHandler(String m2RepositoryDir) {
        this.m2RepositoryDir = m2RepositoryDir;
        this.m2RepositoryDirPath = Paths.get(m2RepositoryDir);
    }

    public void doWork() throws IOException {
        ConfigurableApplicationContext applicationContext = Application.start(new String[0]);
        MavenModelRepository mavenModelRepository = applicationContext.getBean(MavenModelRepository.class);


        Files.walkFileTree(m2RepositoryDirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                if (file.getFileName().toString().endsWith(".pom")) {
                    Path pom = file;
                    try (BufferedReader br = Files.newBufferedReader(pom)) {
                        Model model = parseModel(pom, br);

                        if (model.getVersion() == null || model.getGroupId() == null || model.getArtifactId() == null) {
                            log.warn("some attr is null:" + model + file.toString());
                        }

                        MavenModel mavenModel = mavenModelRepository.findByGroupIdAndArtifactId(model.getGroupId(), model.getArtifactId());

                        if (mavenModel == null) {
                            mavenModel = new MavenModel();
                            mavenModel.setGroupId(model.getGroupId());
                            mavenModel.setArtifactId(model.getArtifactId());
                        }
                        mavenModel.setDescription(model.getDescription());
                        mavenModel.setName(model.getName());

                        mavenModelRepository.save(mavenModel);

                        MavenModel finalMavenModel = mavenModel;
                        model.getDependencies().forEach(dependency -> {
                            if (dependency.getGroupId() == null || dependency.getArtifactId() == null) {
                                log.warn("getGroupId or getArtifactId is null," + model + ": " + dependency.getManagementKey());
                            } else {
                                MavenModel mavenModelDep = mavenModelRepository.findByGroupIdAndArtifactId(dependency.getGroupId(), dependency.getArtifactId());
                                if (mavenModelDep == null) {
                                    mavenModelDep = new MavenModel();
                                    mavenModelDep.setArtifactId(dependency.getArtifactId());
                                    mavenModelDep.setGroupId(dependency.getGroupId());
                                    mavenModelDep.getDependencies().add(finalMavenModel);
                                    mavenModelRepository.save(mavenModelDep);
                                } else {
                                    mavenModelDep.getDependencies().add(finalMavenModel);
                                    mavenModelRepository.save(mavenModelDep);
                                }
                            }
                        });
                    } catch (IOException | XmlPullParserException io) {
                        log.error(file.toString(), io);
                    }
                }

                return super.visitFile(file, attrs);
            }
        });
    }

    private Model parseModel(Path pom, BufferedReader br) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(br);

        model.setPomFile(pom.toFile());

        if (model.getGroupId() == null) {
            model.setGroupId(model.getParent().getGroupId());
        }
        if (model.getVersion() == null) {
            model.setVersion(model.getParent().getVersion());
        }

        return model;
    }
}
