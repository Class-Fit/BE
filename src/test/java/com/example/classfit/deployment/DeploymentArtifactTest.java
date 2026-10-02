package com.example.classfit.deployment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentArtifactTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @TempDir
    Path temporaryDirectory;

    @Test
    void dockerfileBuildsJava21ImageWithNonRootHealthcheck() throws IOException {
        String dockerfile = read("Dockerfile");

        assertThat(dockerfile)
                .contains("FROM eclipse-temurin:21-jdk")
                .contains("FROM eclipse-temurin:21-jre")
                .contains("USER classfit")
                .contains("HEALTHCHECK")
                .contains("/actuator/health");
    }

    @Test
    void composeKeepsApplicationPrivateAndEnablesProductionProfile() throws IOException {
        String compose = read("deploy/compose.yaml");

        assertThat(compose)
                .contains("SPRING_PROFILES_ACTIVE: prod")
                .contains("restart: unless-stopped")
                .contains("80:80")
                .contains("443:443")
                .doesNotContain("8080:8080");
    }

    @Test
    void caddyOnlyProxiesApiSubdomainToApplication() throws IOException {
        String caddyfile = read("deploy/Caddyfile");

        assertThat(caddyfile)
                .contains("api.{$CLASSFIT_DOMAIN}")
                .contains("reverse_proxy app:8080");
    }

    @Test
    void environmentRendererDecryptsParametersAndWritesOwnerOnlyFile() throws IOException {
        String renderer = read("deploy/scripts/render-env.sh");

        assertThat(renderer)
                .contains("ssm get-parameter")
                .contains("--with-decryption")
                .contains("chmod 600")
                .contains("mv \"$temporary_file\" \"$destination\"")
                .doesNotContain("set -x");
    }

    @Test
    void unhealthyImageRollsBackWithoutReplacingCurrentImage() throws Exception {
        Path runtime = Files.createDirectories(temporaryDirectory.resolve("runtime"));
        Path fakeBin = Files.createDirectories(temporaryDirectory.resolve("bin"));
        Path dockerLog = temporaryDirectory.resolve("docker.log");
        Files.writeString(runtime.resolve("current-image"), "previous-sha\n");

        executable(fakeBin.resolve("aws"), """
                #!/usr/bin/env bash
                if [[ "$*" == *"get-login-password"* ]]; then
                  printf 'token'
                else
                  printf 'test-value\n'
                fi
                """);
        executable(fakeBin.resolve("docker"), """
                #!/usr/bin/env bash
                printf '%s|IMAGE_TAG=%s\n' "$*" "${IMAGE_TAG:-}" >> "$DOCKER_LOG"
                if [[ "${1:-}" == "inspect" ]]; then
                  printf 'unhealthy\n'
                fi
                """);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "bash", PROJECT_ROOT.resolve("deploy/scripts/deploy.sh").toString(), "new-sha");
        Map<String, String> environment = processBuilder.environment();
        environment.put("PATH", fakeBin + ":" + environment.get("PATH"));
        environment.put("DEPLOY_ROOT", temporaryDirectory.toString());
        environment.put("DOCKER_LOG", dockerLog.toString());
        environment.put("AWS_ACCOUNT_ID", "123456789012");
        environment.put("AWS_REGION", "ap-northeast-2");
        environment.put("ECR_REPOSITORY", "classfit");
        environment.put("CLASSFIT_DOMAIN", "example.com");
        environment.put("HEALTH_RETRIES", "1");
        environment.put("HEALTH_INTERVAL_SECONDS", "0");
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        assertThat(exitCode).isNotZero();
        assertThat(output).doesNotContain("test-value").doesNotContain("token");
        assertThat(Files.readString(runtime.resolve("current-image"))).isEqualTo("previous-sha\n");
        assertThat(Files.readString(dockerLog))
                .contains("IMAGE_TAG=new-sha")
                .contains("IMAGE_TAG=previous-sha");
    }

    @Test
    void ssmCommandPassesAllRuntimeSettingsToInstance() throws Exception {
        Path fakeBin = Files.createDirectories(temporaryDirectory.resolve("bin"));
        Path parametersFile = temporaryDirectory.resolve("ssm-parameters.json");
        executable(fakeBin.resolve("aws"), """
                #!/usr/bin/env bash
                if [[ "$1 $2" != "ssm send-command" ]]; then exit 1; fi
                while [[ $# -gt 0 ]]; do
                  if [[ "$1" == "--parameters" ]]; then
                    printf '%s' "$2" > "$PARAMETERS_FILE"
                    break
                  fi
                  shift
                done
                printf 'command-test-id\n'
                """);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "bash", PROJECT_ROOT.resolve("deploy/scripts/send-command.sh").toString(), "abc123");
        Map<String, String> environment = processBuilder.environment();
        environment.put("PATH", fakeBin + ":" + environment.get("PATH"));
        environment.put("PARAMETERS_FILE", parametersFile.toString());
        environment.put("AWS_REGION", "ap-northeast-2");
        environment.put("ECR_REGISTRY", "123456789012.dkr.ecr.ap-northeast-2.amazonaws.com");
        environment.put("ECR_REPOSITORY", "classfit-backend");
        environment.put("EC2_INSTANCE_ID", "i-1234567890abcdef0");
        environment.put("CLASSFIT_DOMAIN", "example.com");
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isZero();
        assertThat(output).isEqualTo("command-test-id\n");

        JsonNode commands = new ObjectMapper().readTree(Files.readString(parametersFile)).path("commands");
        assertThat(commands.isArray()).isTrue();
        String deploymentCommand = commands.get(commands.size() - 1).asText();
        assertThat(deploymentCommand)
                .contains("AWS_ACCOUNT_ID='123456789012'")
                .contains("AWS_REGION='ap-northeast-2'")
                .contains("ECR_REPOSITORY='classfit-backend'")
                .contains("CLASSFIT_DOMAIN='example.com'")
                .contains("/opt/classfit/scripts/deploy.sh 'abc123'");
    }

    @Test
    void composePreservesDollarSignsInDecryptedSecrets() throws Exception {
        Path runtime = Files.createDirectories(temporaryDirectory.resolve("runtime"));
        Files.copy(PROJECT_ROOT.resolve("deploy/compose.yaml"), temporaryDirectory.resolve("compose.yaml"));
        Files.copy(PROJECT_ROOT.resolve("deploy/Caddyfile"), temporaryDirectory.resolve("Caddyfile"));
        Files.writeString(runtime.resolve("app.env"), "DB_PASSWORD=pa$word\n");

        ProcessBuilder processBuilder = new ProcessBuilder(
                "docker", "compose", "--env-file", runtime.resolve("app.env").toString(),
                "-f", temporaryDirectory.resolve("compose.yaml").toString(),
                "config", "--format", "json");
        processBuilder.environment().put("AWS_ACCOUNT_ID", "123456789012");
        processBuilder.environment().put("AWS_REGION", "ap-northeast-2");
        processBuilder.environment().put("ECR_REPOSITORY", "classfit-backend");
        processBuilder.environment().put("IMAGE_TAG", "abc123");
        processBuilder.environment().put("CLASSFIT_DOMAIN", "example.com");

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(errors).isZero();
        JsonNode config = new ObjectMapper().readTree(output);
        // Compose escapes a literal dollar as $$ when serializing the resolved config.
        assertThat(config.path("services").path("app").path("environment").path("DB_PASSWORD").asText())
                .isEqualTo("pa$$word");
    }

    @Test
    void failedHttpsProbeKeepsPreviousImageForRollback() throws Exception {
        Path runtime = Files.createDirectories(temporaryDirectory.resolve("runtime"));
        Path fakeBin = Files.createDirectories(temporaryDirectory.resolve("bin"));
        Path dockerLog = temporaryDirectory.resolve("docker.log");
        Files.writeString(runtime.resolve("current-image"), "previous-sha\n");
        executable(fakeBin.resolve("aws"), """
                #!/usr/bin/env bash
                if [[ "$*" == *"get-login-password"* ]]; then printf 'token';
                else printf 'test-value\n'; fi
                """);
        executable(fakeBin.resolve("docker"), """
                #!/usr/bin/env bash
                printf '%s|IMAGE_TAG=%s\n' "$*" "${IMAGE_TAG:-}" >> "$DOCKER_LOG"
                if [[ "${1:-}" == "inspect" ]]; then printf 'healthy\n'; fi
                """);
        executable(fakeBin.resolve("curl"), """
                #!/usr/bin/env bash
                exit 60
                """);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "bash", PROJECT_ROOT.resolve("deploy/scripts/deploy.sh").toString(), "new-sha");
        Map<String, String> environment = processBuilder.environment();
        environment.put("PATH", fakeBin + ":" + environment.get("PATH"));
        environment.put("DEPLOY_ROOT", temporaryDirectory.toString());
        environment.put("DOCKER_LOG", dockerLog.toString());
        environment.put("AWS_ACCOUNT_ID", "123456789012");
        environment.put("AWS_REGION", "ap-northeast-2");
        environment.put("ECR_REPOSITORY", "classfit");
        environment.put("CLASSFIT_DOMAIN", "example.com");
        environment.put("HEALTH_RETRIES", "1");
        environment.put("HEALTH_INTERVAL_SECONDS", "0");
        environment.put("PROXY_HEALTH_RETRIES", "1");
        environment.put("PROXY_HEALTH_INTERVAL_SECONDS", "0");
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isNotZero();
        assertThat(Files.readString(runtime.resolve("current-image"))).isEqualTo("previous-sha\n");
        assertThat(Files.readString(dockerLog)).contains("IMAGE_TAG=previous-sha");
    }

    private void executable(Path path, String contents) throws IOException {
        Files.writeString(path, contents);
        assertThat(path.toFile().setExecutable(true)).isTrue();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relativePath));
    }
}
