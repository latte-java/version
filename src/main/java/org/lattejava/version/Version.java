/*
 * Copyright (c) 2022-2026 The Latte Project
 * SPDX-License-Identifier: MIT
 */
package org.lattejava.version;

import java.util.*;
import java.util.stream.*;

import org.lattejava.version.Version.PreRelease.PreReleasePart.*;

/**
 * This class models a simple three number version as well as any free form version String. It has two modes of
 * operation, strict and relaxed.
 * <p>
 * When this class is constructed, it tries everything in its power to figure out what the heck a version string is.
 * It strictly supports SemVer and will fail if passed a non-SemVer String.
 * <p>
 * <a href="http://semver.org">http://semver.org</a>
 * <p>
 * The only notable except to this scheme is that Latte supports integration builds. Integration builds are versions
 * that are not yet released to a repository. To be clear, an integration should NEVER exist in any repository and
 * should ONLY exist in local caches. Latte will refuse to publish integration builds. If a developer decides to break
 * this rule and manually adds an integration build to a repository, they should be loudly and publicly ridiculed until
 * they delete it. 😉
 * <p>
 * Integration builds are always denoted by an additional version specifier of {@code -{integration}}. For example,
 * {@code 1.0.0-{integration}} denotes an integration build.
 *
 * @author Brian Pontarelli
 */
@SuppressWarnings("unused")
public record Version(int major, int minor, int patch, PreRelease preRelease, String metaData) implements Comparable<Version> {
  public static final String INTEGRATION = "{integration}";

  // Constructor used for de-serialization
  public Version() {
    this(0, 0, 0, null, null);
  }

  /**
   * Constructs a version with the given major, minor and patch version numbers.
   *
   * @param major      The major version number.
   * @param minor      The minor version number.
   * @param patch      The patch version number.
   * @param preRelease The pre-release object.
   * @param metaData   The build metadata string.
   */
  public Version {
    if (major < 0 || minor < 0 || patch < 0) {
      throw new VersionException("Major, minor and patch must be positive integers");
    }
  }

  /**
   * Constructs a version by parsing the given String.
   *
   * @param version The version String to parse.
   * @throws VersionException If the string is incorrectly formatted and does not conform to the semantic versioning
   *                          scheme (starts with a delimiter (. or -), contains two delimiters in a row, doesn't have
   *                          proper pre-release or meta-data information).
   */
  public Version(String version) {
    if (version == null || version.isBlank()) {
      throw new VersionException("Invalid Semantic Version string (it was null or empty).");
    }

    Integer major = null;
    Integer minor = null;
    Integer patch = null;
    PreRelease preRelease = null;
    String metaData = null;
    try {
      version = version.trim();
      char start = version.charAt(0);
      char end = version.charAt(version.length() - 1);
      if (start == '.' || start == '-' || start == '+' || end == '.' || end == '-' || end == '+') {
        throw new VersionException("Invalid Semantic Version string [" + version + "]. Version strings should not begin or end with . - or +");
      }

      StringBuilder num = new StringBuilder();

      // Number loop
      int i = 0;
      for (; i < version.length(); i++) {
        char c = version.charAt(i);
        if (c == '.') {
          if (num.isEmpty()) {
            throw new VersionException("Invalid Semantic Version string [" + version + "]. Two version delimiters should not be next to each other.");
          }

          if (major == null) {
            major = Integer.parseInt(num.toString());
          } else if (minor == null) {
            minor = Integer.parseInt(num.toString());
          } else if (patch == null) {
            patch = Integer.parseInt(num.toString());
          } else {
            throw new VersionException("Invalid Semantic Version string [" + version + "]. A version can only have at most 3 dotted parts <major>.<minor>.<patch>");
          }

          num.setLength(0);
        } else if (c == '-' || c == '+') {
          if (num.isEmpty()) {
            throw new VersionException("Invalid Semantic Version string [" + version + "]. Two version delimiters should not be next to each other.");
          }

          break;
        } else if (Character.isDigit(c)) {
          num.append(c);
        } else {
          throw new VersionException("Invalid Semantic Version string [" + version + "]. Alphabetic characters are not allowed in the initial version string.");
        }
      }

      // Handle the final value
      if (!num.isEmpty()) {
        if (major == null) {
          major = Integer.parseInt(num.toString());
        } else if (minor == null) {
          minor = Integer.parseInt(num.toString());
        } else if (patch == null) {
          patch = Integer.parseInt(num.toString());
        } else {
          throw new VersionException("Invalid Semantic Version string [" + version + "]. A version can only have at most 3 dotted parts <major>.<minor>.<patch>");
        }
      }

      // Pre-release and meta
      int plus = version.indexOf('+', i);
      if (i < version.length() && version.charAt(i) == '-') {
        preRelease = new PreRelease((plus == -1) ? version.substring(i + 1) : version.substring(i + 1, plus));
      }

      if (plus != -1) {
        metaData = version.substring(plus + 1);
      }
    } catch (VersionException e) {
      throw e;
    } catch (Exception e) {
      throw new VersionException("Invalid Semantic Version string [" + version + "]. See the `Caused by` stacktrace for the reason why.", e);
    }

    this(major != null ? major : 0, minor != null ? minor : 0, patch != null ? patch : 0, preRelease, metaData);
  }

  /**
   * Returns the value of the comparison between this Version and the given Object. This throws an exception if the
   * object given is not a Version.
   *
   * @param other The other Object to compare against.
   * @return A positive integer if this Version is larger than the given version. Zero if the given Version is the exact
   *     same as this Version. A negative integer is this Version is smaller that the given Version.
   */
  public int compareTo(Version other) {
    int result = major - other.major;
    if (result == 0) {
      result = minor - other.minor;
    }

    if (result == 0) {
      result = patch - other.patch;
    }

    if (result == 0) {
      if (preRelease != null && other.preRelease != null) {
        result = preRelease.compareTo(other.preRelease);
      } else if (preRelease != null) {
        result = -1;
      } else if (other.preRelease != null) {
        result = 1;
      }
    }

    return result;
  }

  /**
   * Compares this Version with the given Object. This explicitly excludes MetaData from the equality checks. This is
   * based on the SemVer specification, which states that MetaData is not a factor in a versions value. It is simply
   * additional information about a version.
   *
   * @param o The object to compare with this Version for equality.
   * @return True if they are both Versions and equal, false otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Version that)) {
      return false;
    }

    return major == that.major &&
        minor == that.minor &&
        patch == that.patch &&
        (Objects.equals(preRelease, that.preRelease));
  }

  public int getMajor() {
    return major;
  }

  public String getMetaData() {
    return metaData;
  }

  public int getMinor() {
    return minor;
  }

  public int getPatch() {
    return patch;
  }

  public PreRelease getPreRelease() {
    return preRelease;
  }

  /**
   * Performs semantic version compatibility checking. If this version is on the 0.x line, than the other version has to
   * be on the same minor line (e.g. 0.3 is not compatible with 0.4). Otherwise, the two versions have to be on the same
   * major line (e.g. 1.0 is not compatible with 2.0).
   *
   * @param other The other version to check against.
   * @return True if the versions are compatible, false if they aren't.
   */
  public boolean isCompatibleWith(Version other) {
    if (major == 0) {
      return minor == other.minor;
    }

    return major == other.major;
  }

  /**
   * @return True if this Version is an integration, false for all other types of versions.
   */
  public boolean isIntegration() {
    return preRelease != null && preRelease.isIntegration();
  }

  /**
   * @return True if this Version is a major, false for all other types of versions.
   */
  public boolean isMajor() {
    return minor == 0 && patch == 0 && preRelease == null;
  }

  /**
   * @return True if this Version is a minor, false for all other types of versions.
   */
  public boolean isMinor() {
    return minor > 0 && patch == 0 && preRelease == null;
  }

  /**
   * @return True if this Version is a patch, false for all other types of versions.
   */
  public boolean isPatch() {
    return patch > 0 && preRelease == null;
  }

  /**
   * @return True if this Version is a snapshot, false for all other types of versions.
   */
  public boolean isPreRelease() {
    return preRelease != null;
  }

  public Version toIntegrationVersion() {
    if (isIntegration()) {
      return this;
    }

    PreRelease integrationPreRelease = new PreRelease();
    if (preRelease != null) {
      integrationPreRelease.parts.addAll(preRelease.parts);
    }

    integrationPreRelease.parts.add(new StringPreReleasePart(INTEGRATION));

    return new Version(major, minor, patch, integrationPreRelease, metaData);
  }

  /**
   * Converts the version number to a string suitable for debugging.
   *
   * @return A String of the version number.
   */
  public String toString() {
    return major + "." + minor + "." + patch + (preRelease != null ? "-" + preRelease : "") + (metaData != null ? "+" + metaData : "");
  }

  /**
   * Models the PreRelease portion of the Semantic Version String.
   *
   * @author Brian Pontarelli
   */
  public static class PreRelease implements Comparable<PreRelease> {
    public final List<PreReleasePart> parts = new ArrayList<>();

    public PreRelease(PreReleasePart... parts) {
      Collections.addAll(this.parts, parts);
    }

    public PreRelease(String spec) {
      char start = spec.charAt(0);
      char end = spec.charAt(spec.length() - 1);
      if (start == '.' || start == '-' || start == '+' || end == '.' || end == '-' || end == '+') {
        throw new VersionException("Invalid Semantic Version PreRelease string [" + spec + "]. PreRelease Version strings should not begin or end with . - or +");
      }

      StringBuilder part = new StringBuilder();
      int i = 0;
      for (; i < spec.length(); i++) {
        char c = spec.charAt(i);
        if (c == '.') {
          if (part.isEmpty()) {
            throw new VersionException("Invalid Semantic Version PreRelease string [" + spec + "]. Two version separators (.) should not be next to each other.");
          }

          if (part.toString().equals(INTEGRATION)) {
            throw new VersionException("Invalid Semantic Version PreRelease string [" + spec + "]. The {integration} indicator must be the last PreRelease part.");
          }

          addPart(part);
        } else {
          part.append(c);
        }
      }

      addPart(part);
    }

    @Override
    public int compareTo(PreRelease o) {
      for (int i = 0; i < Integer.max(parts.size(), o.parts.size()); i++) {
        PreReleasePart mine = (i < parts.size()) ? parts.get(i) : null;
        PreReleasePart theirs = (i < o.parts.size()) ? o.parts.get(i) : null;
        if (mine == null && theirs == null) {
          return 0;
        }

        if (mine == null) {
          return -1;
        }

        if (theirs == null) {
          return 1;
        }

        int result = mine.compareTo(theirs);
        if (result != 0) {
          return result;
        }
      }

      return 0;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final PreRelease that = (PreRelease) o;
      return parts.equals(that.parts);
    }

    @Override
    public int hashCode() {
      return parts.hashCode();
    }

    /**
     * @return True if the PreRelease contains a part that is an integration indicator. This part must be the last part.
     */
    public boolean isIntegration() {
      return !parts.isEmpty() && parts.getLast().isIntegration();
    }

    @Override
    public String toString() {
      return parts.stream().map(Object::toString).collect(Collectors.joining("."));
    }

    private void addPart(StringBuilder part) {
      if (part.isEmpty()) {
        return;
      }

      String partStr = part.toString();
      try {
        int value = Integer.parseInt(partStr);
        parts.add(new NumberPreReleasePart(value));
      } catch (NumberFormatException e) {
        parts.add(new StringPreReleasePart(partStr));
      }

      part.setLength(0);
    }

    /**
     * Defines parts of a PreRelease version string.
     *
     * @author Brian Pontarelli
     */
    public sealed interface PreReleasePart extends Comparable<PreReleasePart>
        permits NumberPreReleasePart, StringPreReleasePart {
      /**
       * @return True if this PreReleasePart is an integration build indicator.
       */
      boolean isIntegration();

      /**
       * @return True if this PreReleasePart is a numeric part.
       */
      boolean isNumber();

      /**
       * A number part of the PreRelease portion of the Semantic Version String.
       */
      record NumberPreReleasePart(int value) implements PreReleasePart {
        @Override
        public int compareTo(PreReleasePart o) {
          if (o instanceof StringPreReleasePart) {
            return -1;
          }

          return value - ((NumberPreReleasePart) o).value;
        }

        @Override
        public boolean isIntegration() {
          return false;
        }

        @Override
        public boolean isNumber() {
          return true;
        }

        @Override
        public String toString() {
          return "" + value;
        }
      }

      /**
       * A String part of the PreRelease portion of the Semantic Version String.
       */
      record StringPreReleasePart(String value) implements PreReleasePart {
        @Override
        public int compareTo(PreReleasePart o) {
          if (o instanceof NumberPreReleasePart) {
            return 1;
          }

          return value.compareTo(((StringPreReleasePart) o).value);
        }

        @Override
        public boolean isIntegration() {
          return value.equals(INTEGRATION);
        }

        @Override
        public boolean isNumber() {
          return false;
        }

        @Override
        public String toString() {
          return value;
        }
      }
    }
  }
}
