package edu.uni.airportsim.cli;

public interface CliCommand {
    String label();

    boolean execute();
}
