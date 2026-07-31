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
 * When this class is constructed, it tries everything in its power to figure out what the heck a version string is. It
 * strictly supports SemVer and will fail if passed a non-SemVer String.
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
@SuppressWarnings({"unused", "UnusedReturnValue"})
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
        } else if (c >= '0' && c <= '9') {
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
   * Computes the hash code for this Version. This explicitly excludes MetaData in order to stay consistent with
   * {@link #equals(Object)}, which excludes it per the SemVer specification.
   *
   * @return The hash code.
   */
  public int hashCode() {
    return Objects.hash(major, minor, patch, preRelease);
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

    List<PreRelease.PreReleasePart> parts = new ArrayList<>();
    if (preRelease != null) {
      parts.addAll(preRelease.parts);
    }

    parts.add(new StringPreReleasePart(INTEGRATION));

    return new Version(major, minor, patch, new PreRelease(parts.toArray(new PreRelease.PreReleasePart[0])), metaData);
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
   * @param parts The parts of this PreRelease. This List is immutable and never contains a null element.
   * @author Brian Pontarelli
   */
  public record PreRelease(List<PreReleasePart> parts) implements Comparable<PreRelease> {
    /**
     * Constructs a PreRelease from the given parts.
     *
     * @param parts The parts. Must contain at least one part and must not contain a null part.
     * @throws VersionException If the parts are null, empty, or contain a null part.
     */
    public PreRelease(PreReleasePart... parts) {
      if (parts == null || parts.length == 0) {
        throw new VersionException("Invalid Semantic Version PreRelease (it did not contain any parts).");
      }

      for (PreReleasePart part : parts) {
        if (part == null) {
          throw new VersionException("Invalid Semantic Version PreRelease (it contained a null part).");
        }
      }

      this(List.of(parts));
    }

    /**
     * Constructs a PreRelease by parsing the given String.
     *
     * @param spec The PreRelease String to parse.
     * @throws VersionException If the string is null, blank, or incorrectly formatted.
     */
    public PreRelease(String spec) {
      this(parse(spec));
    }

    private static void addPart(List<PreReleasePart> parts, String part) {
      if (part.isEmpty()) {
        return;
      }

      if (!isNumeric(part)) {
        parts.add(new StringPreReleasePart(part));
        return;
      }

      if (part.length() > 1 && part.charAt(0) == '0') {
        throw new VersionException("Invalid Semantic Version PreRelease part [" + part + "]. Numeric parts must not have leading zeros.");
      }

      try {
        parts.add(new NumberPreReleasePart(Integer.parseInt(part)));
      } catch (NumberFormatException e) {
        throw new VersionException("Invalid Semantic Version PreRelease part [" + part + "]. Numeric parts must fit in a 32 bit signed integer.", e);
      }
    }

    /**
     * Determines if the given String is a SemVer numeric identifier. {@code Character.isDigit()} is deliberately not
     * used here because it accepts non-ASCII digits, which SemVer does not allow.
     *
     * @param str The String to check.
     * @return True if the String consists entirely of ASCII digits.
     */
    private static boolean isNumeric(String str) {
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        if (c < '0' || c > '9') {
          return false;
        }
      }

      return true;
    }

    private static PreReleasePart[] parse(String spec) {
      if (spec == null || spec.isBlank()) {
        throw new VersionException("Invalid Semantic Version PreRelease string (it was null or empty).");
      }

      char start = spec.charAt(0);
      char end = spec.charAt(spec.length() - 1);
      if (start == '.' || start == '-' || start == '+' || end == '.' || end == '-' || end == '+') {
        throw new VersionException("Invalid Semantic Version PreRelease string [" + spec + "]. PreRelease Version strings should not begin or end with . - or +");
      }

      List<PreReleasePart> parts = new ArrayList<>();
      StringBuilder part = new StringBuilder();
      for (int i = 0; i < spec.length(); i++) {
        char c = spec.charAt(i);
        if (c == '.') {
          if (part.isEmpty()) {
            throw new VersionException("Invalid Semantic Version PreRelease string [" + spec + "]. Two version separators (.) should not be next to each other.");
          }

          if (part.toString().equals(INTEGRATION)) {
            throw new VersionException("Invalid Semantic Version PreRelease string [" + spec + "]. The {integration} indicator must be the last PreRelease part.");
          }

          String partStr = part.toString();
          part.setLength(0);
          addPart(parts, partStr);
        } else {
          part.append(c);
        }
      }

      addPart(parts, part.toString());

      return parts.toArray(new PreReleasePart[0]);
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

          return Integer.compare(value, ((NumberPreReleasePart) o).value);
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
        /**
         * @param value The value of this part.
         * @throws VersionException If the value is null or blank.
         */
        public StringPreReleasePart {
          if (value == null || value.isBlank()) {
            throw new VersionException("Invalid Semantic Version PreRelease part (it was null or empty).");
          }
        }

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
