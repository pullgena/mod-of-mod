package dev.pullgena.modofmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.jar.JarFile;

/**
 * Strictly pinned updater for THIS mod. Not a general-purpose jar downloader.
 * Runs outside the game tick. The GitHub Release asset must provide a SHA-256 digest.
 */
public final class ReleaseUpdater {
    public static final String REPO = "https://github.com/pullgena/mod-of-mod";
    private static final String API = "https://api.github.com/repos/pullgena/mod-of-mod/releases/latest";
    private static final int LIMIT = 32 * 1024 * 1024;
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(8)).build();

    private ReleaseUpdater() { }

    public static CompletableFuture<String> updateAsync(String repoUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return checkAndInstall(repoUrl);
            } catch (Exception error) {
                ModOfMod.LOGGER.warn("Update check failed", error);
                return "업데이트 실패: " + safeMessage(error);
            }
        });
    }

    /** Only an official release for the known project can be installed. */
    private static String checkAndInstall(String repoUrl) throws Exception {
        requireKnownRepo(repoUrl);
        HttpRequest request = HttpRequest.newBuilder(URI.create(API))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ModOfMod-Update-Client")
            .timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) {
            return "아직 GitHub Releases의 공개 릴리스가 없습니다. 릴리스를 먼저 게시해 주세요.";
        }
        if (response.statusCode() != 200) throw new IOException("GitHub Releases 응답: HTTP " + response.statusCode());

        JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
        if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean()) {
            throw new IOException("정식 공개 릴리스만 허용합니다.");
        }
        String tag = release.get("tag_name").getAsString();
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        if (!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) throw new IOException("릴리스 태그는 v1.2.3 형식이어야 합니다.");
        String installed = FabricLoader.getInstance().getModContainer(ModOfMod.ID)
            .orElseThrow(() -> new IOException("현재 모드를 찾을 수 없습니다."))
            .getMetadata().getVersion().getFriendlyString();
        if (compareVersions(version, installed) <= 0) return "현재 최신 버전이거나 더 높은 버전입니다. 설치: " + installed;

        String wantedName = "mod-of-mod-" + version + ".jar";
        JsonArray assets = release.getAsJsonArray("assets");
        JsonObject asset = null;
        if (assets != null) for (int i = 0; i < assets.size(); i++) {
            JsonObject candidate = assets.get(i).getAsJsonObject();
            if (wantedName.equals(candidate.get("name").getAsString())) { asset = candidate; break; }
        }
        if (asset == null) throw new IOException("릴리스에 " + wantedName + " 파일이 없습니다.");

        String digest = asset.has("digest") && !asset.get("digest").isJsonNull()
            ? asset.get("digest").getAsString() : "";
        if (!digest.matches("sha256:[0-9a-fA-F]{64}")) {
            throw new IOException("릴리스 파일의 SHA-256 검증값이 없어 설치하지 않았습니다.");
        }
        URI download = URI.create(asset.get("browser_download_url").getAsString());
        String expected = REPO + "/releases/download/";
        if (!download.toString().startsWith(expected) || !download.getPath().endsWith("/" + wantedName)
            || !"https".equalsIgnoreCase(download.getScheme())) {
            throw new IOException("GitHub 릴리스 다운로드 주소가 올바르지 않습니다.");
        }
        Path mods = FabricLoader.getInstance().getGameDir().resolve("mods").toAbsolutePath().normalize();
        Files.createDirectories(mods);
        Path stageDir = mods.resolve(".modofmod-updates");
        Files.createDirectories(stageDir);
        Path staged = stageDir.resolve(wantedName);
        Path temp = Files.createTempFile(stageDir, "download-", ".tmp");
        try {
            HttpRequest get = HttpRequest.newBuilder(download).timeout(Duration.ofSeconds(40))
                .header("User-Agent", "ModOfMod-Update-Client").GET().build();
            HttpResponse<InputStream> result = HTTP.send(get, HttpResponse.BodyHandlers.ofInputStream());
            if (result.statusCode() != 200) {
                result.body().close();
                throw new IOException("릴리스 다운로드 HTTP " + result.statusCode());
            }
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            try (InputStream input = result.body(); var output = Files.newOutputStream(temp)) {
                byte[] block = new byte[8192];
                int count, total = 0;
                while ((count = input.read(block)) >= 0) {
                    total += count;
                    if (total > LIMIT) throw new IOException("업데이트 파일이 32MiB 제한을 초과했습니다.");
                    hash.update(block, 0, count);
                    output.write(block, 0, count);
                }
            }
            String actual = HexFormat.of().formatHex(hash.digest());
            if (!actual.equalsIgnoreCase(digest.substring(7))) throw new IOException("SHA-256 불일치: 설치를 중단합니다.");
            validateJar(temp, version);
            Files.move(temp, staged, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }

        // Prefer replacing the current mod now for activation on NEXT launch.
        // Windows and read-only hosts may lock a running JAR: leave it safely staged then.
        List<Path> oldJars;
        try (var stream = Files.list(mods)) {
            oldJars = stream.filter(Files::isRegularFile).filter(path -> {
                String n = path.getFileName().toString();
                return n.matches("mod-of-mod-[0-9]+\\.[0-9]+\\.[0-9]+\\.jar");
            }).toList();
        }
        if (oldJars.size() != 1) {
            return "v" + version + " 파일 검증 및 다운로드 완료: " + staged
                + " (설치된 JAR을 정확히 하나 찾지 못해 자동 교체를 보류했습니다.)";
        }
        Path old = oldJars.get(0);
        Path target = mods.resolve(wantedName);
        Path backup = stageDir.resolve(old.getFileName().toString() + ".backup");
        try {
            Files.move(old, backup, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(staged, target);
            } catch (IOException moveError) {
                Files.move(backup, old, StandardCopyOption.REPLACE_EXISTING);
                throw moveError;
            }
            return "v" + version + " 설치 파일 교체 완료! 게임/서버를 종료하고 다시 실행하면 적용됩니다. 백업: " + backup;
        } catch (IOException locked) {
            ModOfMod.LOGGER.info("JAR in use / no write access, leaving verified update staged: {}", staged, locked);
            return "v" + version + " 안전 다운로드 완료. 실행 중인 JAR을 교체할 수 없어 보관했습니다: "
                + staged + " | 종료 후 기존 JAR을 mods에서 빼고 보관된 파일을 mods로 옮겨 주세요.";
        }
    }

    public static void requireKnownRepo(String raw) {
        if (raw == null) throw new IllegalArgumentException("GitHub 저장소 링크를 입력해 주세요.");
        URI uri;
        try { uri = URI.create(raw.strip()); } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("올바른 GitHub URL이 아닙니다.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
            || !"github.com".equalsIgnoreCase(uri.getHost())
            || uri.getUserInfo() != null || uri.getPort() != -1
            || uri.getQuery() != null || uri.getFragment() != null
            || !uri.getPath().matches("(?i)^/pullgena/mod-of-mod(?:/|/releases/?)?$")) {
            throw new IllegalArgumentException("보안상 " + REPO + " 저장소의 공식 릴리스만 설치할 수 있습니다.");
        }
    }

    private static void validateJar(Path jarPath, String version) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) throw new IOException("Fabric 모드 파일이 아닙니다.");
            JsonObject metadata;
            try (InputStream in = jar.getInputStream(entry)) {
                metadata = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            }
            if (!ModOfMod.ID.equals(metadata.get("id").getAsString())
                || !version.equals(metadata.get("version").getAsString())) {
                throw new IOException("모드 ID 또는 버전이 릴리스와 맞지 않습니다.");
            }
        }
    }

    private static int compareVersions(String a, String b) {
        if (!b.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) return 1;
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int cmp = Long.compare(Long.parseLong(x[i]), Long.parseLong(y[i]));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    private static String safeMessage(Exception error) {
        String msg = error.getMessage();
        return msg == null ? error.getClass().getSimpleName() : msg.replace('\n', ' ').substring(0, Math.min(220, msg.length()));
    }
}
