package de.resourcebuilder.profile;

import com.google.gson.Gson;
import de.resourcebuilder.StudioSettings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ShareCode {
  private static final String PREFIX = "RB3:";
  private static final int MAX_CODE = 65_536;
  private static final int MAX_JSON = 131_072;
  private static final Gson GSON = new Gson();

  private ShareCode() {}

  public static String encode(StudioSettings settings) {
    try {
      var payload = new Payload(3, settings.visual.normalized());
      var bytes = new ByteArrayOutputStream();
      try (var gzip = new GZIPOutputStream(bytes)) {
        gzip.write(GSON.toJson(payload).getBytes(StandardCharsets.UTF_8));
      }
      return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    } catch (Exception error) {
      throw new IllegalStateException("Could not create share code", error);
    }
  }

  public static boolean apply(String code, StudioSettings target) {
    if (code == null || !code.startsWith(PREFIX) || code.length() > MAX_CODE) return false;
    try {
      byte[] compressed = Base64.getUrlDecoder().decode(code.substring(PREFIX.length()));
      var output = new ByteArrayOutputStream();
      try (var gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
        gzip.transferTo(new LimitedOutputStream(output, MAX_JSON));
      }
      Payload payload = GSON.fromJson(output.toString(StandardCharsets.UTF_8), Payload.class);
      if (payload == null || payload.version != 3 || payload.visual == null) return false;
      target.visual = payload.visual.normalized();
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private record Payload(int version, VisualProfile visual) {}

  private static final class LimitedOutputStream extends java.io.OutputStream {
    private final ByteArrayOutputStream target;
    private final int limit;

    LimitedOutputStream(ByteArrayOutputStream target, int limit) {
      this.target = target;
      this.limit = limit;
    }

    @Override
    public void write(int value) {
      if (target.size() >= limit) throw new IllegalArgumentException("Share code is too large");
      target.write(value);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) {
      if (target.size() + length > limit)
        throw new IllegalArgumentException("Share code is too large");
      target.write(bytes, offset, length);
    }
  }
}
