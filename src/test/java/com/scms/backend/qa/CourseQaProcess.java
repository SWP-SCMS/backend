package com.scms.backend.qa;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/**
 * Owns exactly one child JVM; never searches for or kills shared servers.
 */
public final class CourseQaProcess implements AutoCloseable {

	public final Path directory;

	public final PostgreSQLContainer postgres;

	public Process process;

	public int port;

	private int starts;

	public CourseQaProcess(Path directory, PostgreSQLContainer postgres) {
		this.directory = directory;
		this.postgres = postgres;
	}

	public void start() throws Exception {
		Files.createDirectories(directory);
		Files.deleteIfExists(directory.resolve("ready.json"));
		String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
		Path args = directory.resolve("java.args");
		Files.writeString(args, "-Xmx384m\n-cp\n\"" + classpath.replace("\\", "/") + "\"\n" + CourseQaApplication.class.getName(), StandardCharsets.UTF_8);
		var builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "@" + args.toAbsolutePath());
		builder.environment().putAll(Map.of("SCMS_QA_DIRECTORY", directory.toAbsolutePath().toString(), "SCMS_QA_JDBC_URL", postgres.getJdbcUrl(), "SCMS_QA_DB_USER", postgres.getUsername(), "SCMS_QA_DB_PASSWORD", postgres.getPassword(), "SCMS_QA_DISPOSABLE", "true"));
		builder.redirectErrorStream(true).redirectOutput(directory.resolve("jvm-" + (++starts) + ".log").toFile());
		process = builder.start();
		await(() -> Files.exists(directory.resolve("ready.json")) || !process.isAlive(), Duration.ofSeconds(90));
		if (!process.isAlive())
			throw new AssertionError("QA child exited: " + Files.readString(directory.resolve("jvm-" + starts + ".log")));
		port = new ObjectMapper().readTree(Files.readString(directory.resolve("ready.json"))).get("port").asInt();
	}

	public void kill() throws Exception {
		if (process != null && process.isAlive()) {
			process.destroyForcibly();
			if (!process.waitFor(15, TimeUnit.SECONDS))
				throw new AssertionError("Owned JVM did not stop");
		}
	}

	public String base() {
		return "http://127.0.0.1:" + port + "/api/v1";
	}

	public static void await(BooleanSupplier condition, Duration timeout) throws Exception {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (!condition.getAsBoolean()) {
			if (System.nanoTime() >= deadline)
				throw new AssertionError("QA condition timed out after " + timeout);
			Thread.sleep(50);
		}
	}

	@Override
	public void close() throws Exception {
		kill();
	}
}
