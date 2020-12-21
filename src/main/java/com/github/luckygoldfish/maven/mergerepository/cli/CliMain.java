package com.github.luckygoldfish.maven.mergerepository.cli;

import com.github.luckygoldfish.maven.mergerepository.handler.CliHandler;
import com.github.luckygoldfish.maven.mergerepository.handler.MergeRepositoryDirHandler;
import com.github.luckygoldfish.maven.mergerepository.handler.WriteNeo4jHandler;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Arrays;

public class CliMain {

    private static String cmd = "java";
    private static Options options = new Options();


    public static void main(String[] args) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            initOptions();

            CommandLine commandLine = getCommandLine(args);

            doWork(commandLine);
        } finally {
            stopWatch.stop();
            System.out.println("used time:" + stopWatch.toString());
        }

    }


    public static void doWork(CommandLine commandLine) throws Exception {
        try {
            CliHandler cliHandler = null;

            if (commandLine.hasOption("mergeDir")) {
                // 合并m2目录
                String[] value = commandLine.getOptionValues("mergeDir");
                cliHandler = new MergeRepositoryDirHandler(value[0], value[1], value[2]);
            } else if (commandLine.hasOption("writeToNeo4j")) {
                // 将组件的元数据信息写入到Neo4中
                cliHandler = new WriteNeo4jHandler(commandLine.getOptionValue("writeToNeo4j"));
            }

            if (cliHandler != null) {
                cliHandler.doWork();
            } else {
                printHelp();
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            System.out.println("--------------------------------------");
            printHelp();
        }
    }

    private static CommandLine getCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();

        CommandLine commandLine = parser.parse(options, args);

        // 打印opts的名称和值
        printOpts(commandLine);

        return commandLine;
    }

    private static void printOpts(CommandLine commandLine) {
        System.out.println("--------------------------------------");
        Option[] opts = commandLine.getOptions();
        if (opts != null && opts.length > 0) {
            for (Option opt1 : opts) {
                String name = opt1.getLongOpt() == null ? opt1.getOpt() : opt1.getLongOpt();
                String[] value = commandLine.getOptionValues(name);
                System.out.println(name + "=>" + Arrays.toString(value));
            }
        }
    }

    private static void initOptions() {
        options.addOption(Option.builder("m").longOpt("mergeDir").desc("合并m2目录")
                .hasArgs().numberOfArgs(3).argName("dir1 dir2 mergeToDir").valueSeparator(' ').build());
        options.addOption(Option.builder("w").longOpt("writeToNeo4j").desc("将组件的元数据信息写入到Neo4中")
                .hasArgs().numberOfArgs(1).argName("dir").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Print help").build());
    }

    private static void printHelp() {
        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(120);
        hf.printHelp(cmd, "-------------------------", options, "=========================", true);
    }
}