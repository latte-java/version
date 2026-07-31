/*
 * Copyright (c) 2026 The Latte Project
 * SPDX-License-Identifier: MIT
 */
package org.lattejava.version.tests;

import module java.base;
import module org.lattejava.version;

import org.lattejava.version.Version.*;
import org.testng.annotations.*;

import static org.testng.Assert.*;

/**
 * Exercises every public entry point of {@link Version} against an exhaustive corpus of candidate version strings to
 * prove that {@link VersionException} is the only exception the class ever throws.
 * <p>
 * The corpus in {@code version-strings.txt} is every string of length 0 through 5 over the alphabet
 * {@code 0 9 . - + a { } <space>} — 66,430 strings. That alphabet covers one digit that is meaningful in a numeric
 * identifier, one that is not, all three SemVer delimiters, an alphabetic character, the braces of the
 * {@code {integration}} marker, and whitespace. Five characters is enough depth to produce every adjacency of those
 * classes, including the empty string, bare and doubled delimiters, and delimiters in leading and trailing position.
 *
 * @author Brian Pontarelli
 */
public class VersionStringsTest {
  private static final int EXPECTED_COUNT = 66430;

  private static final String RESOURCE = "/version-strings.txt";

  /**
   * Every string either parses or throws a VersionException. Anything else escaping is a defect.
   */
  @Test
  public void parsingThrowsOnlyVersionException() throws Exception {
    int parsed = 0;
    for (String spec : loadSpecs()) {
      Version version;
      try {
        version = new Version(spec);
      } catch (VersionException _) {
        continue;
      } catch (Throwable t) {
        throw new AssertionError("new Version([" + spec + "]) threw " + t.getClass().getName(), t);
      }

      parsed++;
      exercise(version, spec);
    }

    // Guards against the corpus degenerating to "everything is rejected", which would make the loop vacuous.
    assertTrue(parsed > 1000, "Only [" + parsed + "] of the corpus parsed, the corpus is no longer meaningful");
  }

  /**
   * The PreRelease String constructor is public and reachable independently of Version, so it carries the same
   * guarantee.
   */
  @Test
  public void preReleaseParsingThrowsOnlyVersionException() throws Exception {
    for (String spec : loadSpecs()) {
      PreRelease preRelease;
      try {
        preRelease = new PreRelease(spec);
      } catch (VersionException _) {
        continue;
      } catch (Throwable t) {
        throw new AssertionError("new PreRelease([" + spec + "]) threw " + t.getClass().getName(), t);
      }

      try {
        preRelease.toString();
        preRelease.hashCode();
        preRelease.isIntegration();
        assertEquals(preRelease.compareTo(preRelease), 0);
        assertEquals(preRelease, new PreRelease(spec));
      } catch (Throwable t) {
        throw new AssertionError("PreRelease([" + spec + "]) threw " + t.getClass().getName() + " after construction", t);
      }
    }
  }

  /**
   * The corpus must survive checkout intact. 7,381 of the strings end in a space, so an editor or tool that strips
   * trailing whitespace would silently collapse them into duplicates and quietly shrink the coverage.
   */
  @Test
  public void resourceIntegrity() throws Exception {
    List<String> specs = loadSpecs();
    assertEquals(specs.size(), EXPECTED_COUNT);
    assertEquals(Set.copyOf(specs).size(), EXPECTED_COUNT, "The corpus contains duplicates, trailing whitespace was likely stripped");
    assertTrue(specs.contains(""), "The corpus is missing the empty string");
    assertTrue(specs.contains(" "), "The corpus is missing the single space, trailing whitespace was likely stripped");
    assertTrue(specs.contains("0 "), "The corpus is missing [0 ], trailing whitespace was likely stripped");
    assertTrue(specs.contains("0.9.0"), "The corpus is missing [0.9.0]");
  }

  /**
   * Every string that parses must survive a round trip through toString(), otherwise the parser is silently discarding
   * or rewriting information.
   */
  @Test
  public void roundTrip() throws Exception {
    for (String spec : loadSpecs()) {
      Version version;
      try {
        version = new Version(spec);
      } catch (VersionException _) {
        continue;
      }

      String rendered = version.toString();
      Version reparsed = new Version(rendered);
      assertEquals(reparsed, version, "[" + spec + "] rendered as [" + rendered + "] which did not round trip");
      assertEquals(reparsed.toString(), rendered, "[" + spec + "] did not render stably");
      assertEquals(reparsed.metaData(), version.metaData(), "[" + spec + "] lost its metaData on the round trip");
    }
  }

  private void exercise(Version version, String spec) {
    try {
      version.toString();
      version.hashCode();
      version.isIntegration();
      version.isMajor();
      version.isMinor();
      version.isPatch();
      version.isPreRelease();
      version.isCompatibleWith(version);
      version.toIntegrationVersion();
      assertEquals(version.compareTo(version), 0);
      assertEquals(version, new Version(spec));
      assertTrue(version.toIntegrationVersion().isIntegration());
    } catch (Throwable t) {
      throw new AssertionError("Version([" + spec + "]) threw " + t.getClass().getName() + " after construction", t);
    }
  }

  private List<String> loadSpecs() throws IOException {
    try (InputStream is = VersionStringsTest.class.getResourceAsStream(RESOURCE)) {
      assertNotNull(is, "Could not find the test resource [" + RESOURCE + "]");

      // Deliberately not trimmed. Whitespace is part of the corpus.
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        return reader.lines().toList();
      }
    }
  }
}
