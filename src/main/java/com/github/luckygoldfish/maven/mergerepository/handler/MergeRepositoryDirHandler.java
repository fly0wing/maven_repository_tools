package com.github.luckygoldfish.maven.mergerepository.handler;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Slf4j
public class MergeRepositoryDirHandler implements CliHandler {
    private final String sourceDirA;
    private final String sourceDirB;
    private final String targetDir;

    private final Path sourceDirAPath;
    private final Path sourceDirBPath;
    private final Path targetDirPath;
    public MergeRepositoryDirHandler(String sourceDirA, String sourceDirB, String targetDir) {
        this.sourceDirA = sourceDirA;
        this.sourceDirB = sourceDirB;
        this.targetDir = targetDir;
        this.sourceDirAPath = Paths.get(sourceDirA);
        this.sourceDirBPath = Paths.get(sourceDirB);
        this.targetDirPath = Paths.get(targetDir);
    }

    public void doWork() throws IOException {

        Files.createDirectories(targetDirPath);

        fileTree(sourceDirAPath,sourceDirBPath);
        fileTree(sourceDirBPath,sourceDirAPath);
    }

    private void fileTree(Path sourcePath, Path otherSourcePath) throws IOException {
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            private Stack<Path> currentDirStack = new Stack<>();
            private boolean currentProcessed = false;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                log.debug("preVisitDirectory:path:{}", dir);
                currentDirStack.push(dir);

                Files.createDirectories(dir);
                return super.preVisitDirectory(dir, attrs);
            }

            /**
             * 遍历文件的时候，只处理第一个文件。后续忽略。
             * 处理的时候，按照整个文件夹进行处理。
             * @param file
             * @param attrs
             * @return
             * @throws IOException
             */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.debug("visitFile:\t\tpath:{},currentProcessed:{}", file,  currentProcessed);
                Path currentDir = this.currentDirStack.peek();
                if (currentDir.equals(file.getParent()) && currentProcessed) {
                    return FileVisitResult.CONTINUE;
                }

                Path todoMergeDir = file.getParent();
                int nameCountPath = sourcePath.getNameCount();
                int nameCountFile = todoMergeDir.getNameCount();
                Path pathB = otherSourcePath;

                Path mavenGroupDir;
                if (nameCountPath < nameCountFile) {
                    mavenGroupDir = todoMergeDir.subpath(nameCountPath, nameCountFile);
                    pathB = Paths.get(otherSourcePath.toString(), mavenGroupDir.toString());
                } else {
                    mavenGroupDir = null;
                }

                doMergeDir(mavenGroupDir, todoMergeDir, pathB);

                currentProcessed = true;
                return super.visitFile(file, attrs);
            }


            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                log.debug("postVisitDirectory:path:{},exce:", dir, exc);
                currentDirStack.pop();
                currentProcessed = false;
                return super.postVisitDirectory(dir, exc);
            }
        });
    }
    private void doMergeDir(Path mavenGroupDir, Path pathA, Path pathB) throws IOException {
        List<Path> copyList = new ArrayList<>();
        if (!pathB.toFile().exists()) {
            // b不存在。则直接对A进行整理
            MavenDependencyFiles files = new MavenDependencyFiles();
            Files.list(pathA).forEach(path -> {
                buildFiles(files, path);
            });
            files.copyFiles(copyList);
        } else {
            // a,b均存在，则进行整理。
            MavenDependencyFiles files = new MavenDependencyFiles();
            Files.list(pathA).forEach(path -> {
                buildFiles(files, path);
            });
            Files.list(pathB).forEach(path -> {
                buildFiles(files, path);
            });
            files.copyFiles(copyList);
        }

        doCopyFile(mavenGroupDir, copyList);
    }
    private void doCopyFile(Path mavenGroupDir,  List<Path> copyList) {
        copyList.forEach(path -> {
            Path toPath = Paths.get(targetDir, mavenGroupDir.toString(), path.getFileName().toString());
            File toFile = toPath.toFile();
            if (toFile.exists()) {
                return;
            }
            toFile.getParentFile().mkdirs();

            try {
                FileUtils.copyFile(path.toFile(), toFile);
            } catch (IOException e) {
                log.error("copy file err,from:{},to:{}", path, toFile, e);
            }

        });
    }
    private void buildFiles(MavenDependencyFiles files, Path path) {
        if (path.toFile().isDirectory()) {
            return;
        }
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(".jar.sha1")) {
            if (fileName.endsWith("sources.jar.sha1")) {
                files.getSourceJarSha1().put(path.getFileName().toString(), path);
            } else if (fileName.endsWith("javadoc.jar.sha1")) {
                files.getJavadocJarSha1().put(path.getFileName().toString(), path);
            } else {
                files.getJarSha1().put(path.getFileName().toString(), path);
            }
        } else if (fileName.endsWith(".jar")) {
            // red5-example-bwcheck-2.0-sources.jar
            // red5-example-bwcheck-2.0-javadoc.jar
            if (fileName.endsWith("sources.jar")) {
                files.getSourceJar().put(path.getFileName().toString(), path);
            } else if (fileName.endsWith("javadoc.jar")) {
                files.getJavadocJar().put(path.getFileName().toString(), path);
            } else {
                files.getJar().put(path.getFileName().toString(), path);
            }
        } else if (fileName.endsWith(".pom.sha1")) {
            files.getPomSha1().put(path.getFileName().toString(), path);
        } else if (fileName.endsWith(".pom")) {
            files.getPom().put(path.getFileName().toString(), path);
        } else if (fileName.endsWith("_remote.repositories")) {
            files.setRemoteRepositories(path);
        } else if (fileName.endsWith("_maven.repositories")) {
            files.setMavenRepositories(path);
        } else if (fileName.endsWith("resolver-status.properties")) {
            files.setResolverStatusProperties(path);
        } else if (fileName.endsWith(".part")
                || fileName.endsWith(".part.lock")
                || fileName.endsWith(".tmp")
                || (fileName.contains("maven-metadata-") && (fileName.endsWith(".xml.md5") || fileName.endsWith(".xml.sha1") || fileName.endsWith(".xml")))
                || fileName.endsWith(".war")
                || fileName.endsWith(".war.sha1")
                || fileName.endsWith(".exe")
                || fileName.endsWith(".exe.sha1")
                || fileName.endsWith(".zip.sha1")
                || fileName.endsWith(".zip")
                || fileName.endsWith(".tar.gz.sha1")
                || fileName.endsWith(".tar.gz")
                || fileName.endsWith(".hpi.sha1")
                || fileName.endsWith(".hpi")
                || fileName.endsWith(".md5")
                || fileName.endsWith(".signature")
                || fileName.endsWith(".signature.sha1")
                || fileName.endsWith(".lastUpdated")) {
            // ignore...
            log.debug("ignore file:{}", path);
        } else {
            log.info("other file: \t\tfilename:{},path:{}", fileName, path);
        }
    }
    @Data
    public static class MavenDependencyFiles {

        /**
         * xml-apis-1.0.b2.jar.sha1
         */
        private Map<String, Path> jarSha1 = new HashMap<>();
        /**
         * xml-apis-1.0.b2.jar
         */
        private Map<String, Path> jar = new HashMap<>();
        /**
         *
         */
        private Map<String, Path> javadocJarSha1 = new HashMap<>();
        /**
         *
         */
        private Map<String, Path> javadocJar = new HashMap<>();
        /**
         *
         */
        private Map<String, Path> sourceJarSha1 = new HashMap<>();
        /**
         *
         */
        private Map<String, Path> sourceJar = new HashMap<>();
        /**
         * xml-apis-1.4.01.pom.sha1
         */
        private Map<String, Path> pomSha1 = new HashMap<>();
        /**
         * xml-apis-1.4.01.pom
         */
        private Map<String, Path> pom = new HashMap<>();
        /**
         * _remote.repositories
         */
        private Path remoteRepositories;
        /**
         * _maven.repositories
         */
        private Path mavenRepositories;
        /**
         * resolver-status.properties
         */
        private Path resolverStatusProperties;

        // xercesImpl-2.11.0.jar.part
        // xercesImpl-2.11.0.jar.part.lock
        // ehcache-core-2.6.5.pom.part
        // ehcache-core-2.6.5.pom.sha1-94e7759b5969132868675104204.tmp
        // wlogin-server-sdk-1.5.1.pom.lastUpdated
        // maven-source-plugin-.pom.lastUpdated
        // maven-metadata-local.xml
        // maven-metadata-local.xml.md5
        // maven-metadata-local.xml.sha1
        // maven-metadata-central.xml.sha1
        // maven-metadata-central.xml
        // maven-metadata-sonatype-snapshots.xml.sha1
        // maven-metadata-sonatype-snapshots.xml
        // maven-metadata-public.xml.sha1
        // maven-metadata-snapshots-repo.xml.sha1
        // maven-metadata-Analytical Labs snapshots.xml.sha1
        // maven-metadata-oss.sonatype.org.xml.sha1
        // maven-metadata-Analytical Labs snapshots.xml
        // maven-metadata-oss.sonatype.org.xml
        // maven-metadata-conjars.xml
        // maven-metadata-public.xml
        // maven-metadata-snapshots-repo.xml
        // maven-metadata-pentaho-repo-sn.xml.sha1
        // maven-metadata-pentaho-repo-sn.xml
        // maven-metadata-pentaho-releases.xml.sha1
        // maven-metadata-pentaho-releases.xml
        // maven-metadata-pentaho-repo3.xml
        // maven-metadata-pentaho-repo.xml
        // red5-example-bwcheck-2.0.war
        // junit-1.2-beta-3.hpi.sha1
        // junit-1.2-beta-3.hpi
        // red5-service-1.0.9-RELEASE-daemon.tar.gz
        // red5-service-1.0.9-RELEASE-daemon.tar.gz.sha1
        // elasticsearch-transport-wares-2.7.1-SNAPSHOT.zip
        // elasticsearch-transport-wares-2.7.1-SNAPSHOT.zip.sha1
        // winsw-1.16-bin.exe
        // winsw-1.16-bin.exe.sha1

        public void copyFiles(List<Path> copyList) {
            validPom(copyList);
            validJar(copyList);
            validSourceJar(copyList);
            validJavadocJar(copyList);
        }

        public void validPom(List<Path> copyList) {
            doCopyFiles(copyList, pom, pomSha1);
        }


        public void validJar(List<Path> copyList) {
            doCopyFiles(copyList, jar, jarSha1);
        }

        public void validSourceJar(List<Path> copyList) {
            doCopyFiles(copyList, sourceJar, sourceJarSha1);
        }

        public void validJavadocJar(List<Path> copyList) {
            doCopyFiles(copyList, javadocJar, javadocJarSha1);
        }

        private void doCopyFiles(List<Path> copyList, Map<String, Path> fileMap, Map<String, Path> sha1Map) {
            for (Map.Entry<String, Path> entry : fileMap.entrySet()) {
                Path file = entry.getValue();
                Path fileSha1 = sha1Map.get(entry.getKey() + ".sha1");
                if (checkSha1(file, fileSha1)) {
                    copyList.add(file);
                    if (fileSha1 != null) {
                        copyList.add(fileSha1);
                    }
                }
            }
        }

        private boolean checkSha1(Path file, Path fileSha1) {
            if (file == null) {
                return false;
            }
            if (fileSha1 == null) {
                return true;
            }

            try {
                String sha1Hex = DigestUtils.sha1Hex(FileUtils.readFileToByteArray(file.toFile()));
                String sha1FileString = FileUtils.readFileToString(fileSha1.toFile(), "utf-8").trim();
                String[] s = sha1FileString.split("\\s");
                if (s.length > 1) {
                    sha1FileString = s[0];
                }
                boolean b = sha1Hex.equalsIgnoreCase(sha1FileString);
                if (!b) {
                    log.error("sha1 failed,path:{},sha1:{}", file, fileSha1);
                }
                return b;
            } catch (IOException e) {
                log.error("validPom error,{}", JSON.toJSONString(this), e);
            }
            return false;
        }
    }
}
