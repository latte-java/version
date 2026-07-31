/*
 * Copyright (c) 2022-2026 The Latte Project
 * SPDX-License-Identifier: MIT
 */
package org.lattejava.version.tests;

import module java.base;
import module org.lattejava.version;

import org.lattejava.version.Version.*;
import org.lattejava.version.Version.PreRelease.*;
import org.lattejava.version.Version.PreRelease.PreReleasePart.*;
import org.testng.annotations.*;

import static java.util.Arrays.*;
import static org.testng.Assert.*;
import static org.testng.Assert.fail;

/**
 * Version Tester.
 *
 * @author Brian Pontarelli
 */
@SuppressWarnings("DataFlowIssue")
public class VersionTest {
  @Test
  public void asciiDigitsOnly() {
    // Character.isDigit() accepts non-ASCII digits, but Semantic Versioning is defined over ASCII only.
    assertThrows(VersionException.class, () -> new Version("١.٢.٣"));
    assertThrows(VersionException.class, () -> new Version("1.٢.3"));

    // The PreRelease charset cannot be restricted to the SemVer alphanumeric set because {integration} uses braces,
    // so a non-ASCII part is retained as a String part rather than being silently parsed as a number.
    PreRelease preRelease = new Version("1.0.0-١").preRelease();
    assertEquals(preRelease.parts(), List.of(new StringPreReleasePart("١")));
    assertFalse(preRelease.parts().getFirst().isNumber());
  }

  @Test
  public void compare() {
    // Test identity
    assertEquals(new Version("1.1.0").compareTo(new Version("1.1.0")), 0);

    // Test everything else
    assertCompareTo("2", "1");
    assertCompareTo("1.8", "1.7");
    assertCompareTo("1.8.1", "1.8.0");

    assertCompareTo("1.8.0-beta", "1.8.0-1");
    assertCompareTo("1.8.0-beta", "1.8.0-alpha");
    assertCompareTo("1.8.0-beta.2", "1.8.0-alpha");
    assertCompareTo("1.8.0-beta.2", "1.8.0-beta");
    assertCompareTo("1.8.0-beta.2", "1.8.0-beta.1");

    assertCompareTo("1.8.0-beta.2.build.2", "1.8.0-alpha");
    assertCompareTo("1.8.0-beta.2.build.2", "1.8.0-beta");
    assertCompareTo("1.8.0-beta.2.build.2", "1.8.0-beta.1");
    assertCompareTo("1.8.0-beta.2.build.2", "1.8.0-beta.2.build.1");
    assertCompareTo("1.8.0-beta.3-{integration}", "1.8.0-beta.2-{integration}");

    assertCompareTo("1.8.0-b", "1.8.0-a");
    assertCompareTo("1.8.0-b.b", "1.8.0-a.a");
    assertCompareTo("1.8.0-b.b", "1.8.0-a.b");
    assertCompareTo("1.8.0-b.b", "1.8.0-b.a");

    assertCompareTo("1.8.0-b.b.b", "1.8.0-a");
    assertCompareTo("1.8.0-b.b.b", "1.8.0-a.a");
    assertCompareTo("1.8.0-b.b.b", "1.8.0-a.a.a");
    assertCompareTo("1.8.0-b.b.b", "1.8.0-a.b.b");
    assertCompareTo("1.8.0-b.b.b", "1.8.0-b");
    assertCompareTo("1.8.0-b.b.b", "1.8.0-b.a");
    assertCompareTo("1.8.0-b.b.b", "1.8.0-b.b");
    assertCompareTo("1.8.0-b.b.b", "1.8.0-b.a.b");
    assertCompareTo("1.8.0-b.b.b", "1.8.0-b.b.a");

    assertCompareTo("1.8.0-2", "1.8.0-1");
    assertCompareTo("1.8.0-2.2", "1.8.0-2.1");
    assertCompareTo("1.8.0-2.2.2", "1.8.0-2.2.1");
    assertCompareTo("1.8.0-2.2.2.2", "1.8.0-2.2.2.1");

    List<Version> versions = new ArrayList<>(asList(new Version("3.0"), new Version("1.7"), new Version("1.0"), new Version("2.0"), new Version("1.8"), new Version("1.6-alpha"), new Version("1.6")));
    Collections.sort(versions);
    assertEquals(versions, asList(new Version("1.0"), new Version("1.6-alpha"), new Version("1.6"), new Version("1.7"), new Version("1.8"), new Version("2.0"), new Version("3.0")));
  }

  @Test
  public void equals() {
    // Test identity
    assertEquals(new Version("1.1.0"), new Version("1.1.0"));

    // Test parts
    PreRelease preRelease = new PreRelease(new StringPreReleasePart("alpha"), new NumberPreReleasePart(1), new StringPreReleasePart("build"), new NumberPreReleasePart(2));
    assertVersionEquals("1", 1, 0, 0, null);
    assertVersionEquals("1.1", 1, 1, 0, null);
    assertVersionEquals("1.2.6", 1, 2, 6, null);
    assertVersionEquals("1.2.6-alpha", 1, 2, 6, new Version.PreRelease(new StringPreReleasePart("alpha")));
    assertVersionEquals("1.2.6-alpha.beta", 1, 2, 6, new Version.PreRelease(new StringPreReleasePart("alpha"), new StringPreReleasePart("beta")));
    assertVersionEquals("1.2.6-alpha.beta.foo", 1, 2, 6, new Version.PreRelease(new StringPreReleasePart("alpha"), new StringPreReleasePart("beta"), new StringPreReleasePart("foo")));
    assertVersionEquals("1.2.6-1-2.beta.foo", 1, 2, 6, new Version.PreRelease(new StringPreReleasePart("1-2"), new StringPreReleasePart("beta"), new StringPreReleasePart("foo")));
    assertVersionEquals("1.2.6-1-2.3-4.5-6", 1, 2, 6, new Version.PreRelease(new StringPreReleasePart("1-2"), new StringPreReleasePart("3-4"), new StringPreReleasePart("5-6")));
    assertVersionEquals("1.2.6-1", 1, 2, 6, new Version.PreRelease(new NumberPreReleasePart(1)));
    assertVersionEquals("1.2.6-1.2", 1, 2, 6, new Version.PreRelease(new NumberPreReleasePart(1), new NumberPreReleasePart(2)));
    assertVersionEquals("1.2.6-1.2.3", 1, 2, 6, new Version.PreRelease(new NumberPreReleasePart(1), new NumberPreReleasePart(2), new NumberPreReleasePart(3)));
    assertVersionEquals("1.2.6-alpha.1.build.2", 1, 2, 6, preRelease);
    assertVersionEquals("4.2.6-alpha.1.build.2", 4, 2, 6, preRelease);
  }

  @Test
  public void hashCodeIgnoresMetaData() {
    // equals() excludes metaData per the SemVer spec, so hashCode() must exclude it too.
    Version a = new Version("1.0.0+build1");
    Version b = new Version("1.0.0+build2");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());

    Set<Version> set = new HashSet<>(List.of(a, b));
    assertEquals(set.size(), 1);
    assertTrue(set.contains(new Version("1.0.0")));

    Version c = new Version("1.0.0-beta+build1");
    Version d = new Version("1.0.0-beta+build2");
    assertEquals(c, d);
    assertEquals(c.hashCode(), d.hashCode());
  }

  @Test
  public void ints() {
    Version v = new Version(1, 1, 2, null, null);
    assertEquals(v.major(), 1);
    assertEquals(v.minor(), 1);
    assertEquals(v.patch(), 2);

    v = new Version(0, 0, 0, null, null);
    assertEquals(v.major(), 0);
    assertEquals(v.minor(), 0);
    assertEquals(v.patch(), 0);

    try {
      new Version(-1, 0, 0, null, null);
      fail("Should have failed");
    } catch (Exception _) {
      // Ignored
    }

    try {
      new Version(0, -1, 0, null, null);
      fail("Should have failed");
    } catch (Exception _) {
    }

    try {
      new Version(0, 0, -1, null, null);
      fail("Should have failed");
    } catch (Exception _) {
    }
  }

  @Test
  public void isIntegration() {
    assertTrue(new Version("1.2.3-{integration}").isIntegration());
    assertTrue(new Version("1.2.3-beta.{integration}").isIntegration());
    assertFalse(new Version("1.2.3-beta-{integration}").isIntegration());
    assertTrue(new Version("1.2.3-beta.2.{integration}").isIntegration());
    assertFalse(new Version("1.2.3-beta.2-{integration}").isIntegration());
    assertFalse(new Version("1.2.3-beta.2+{integration}").isIntegration());
  }

  @Test
  public void leadingZeros() {
    // SemVer forbids leading zeros in numeric identifiers. Accepting them silently corrupts the version because
    // NumberPreReleasePart.toString() drops the zeros and the version no longer round-trips.
    assertThrows(VersionException.class, () -> new Version("1.0.0-01"));
    assertThrows(VersionException.class, () -> new Version("1.0.0-alpha.007"));
    assertThrows(VersionException.class, () -> new Version("1.0.0-00"));

    // A lone zero is a valid numeric identifier and leading zeros are legal inside an alphanumeric identifier.
    assertEquals(new Version("1.0.0-0").preRelease().parts(), List.of(new NumberPreReleasePart(0)));
    assertEquals(new Version("1.0.0-0a").preRelease().parts(), List.of(new StringPreReleasePart("0a")));
    assertEquals(new Version("1.0.0-01a").preRelease().parts(), List.of(new StringPreReleasePart("01a")));
  }

  @Test
  public void numberPartComparisonDoesNotOverflow() {
    // Subtracting the values overflows for widely separated inputs and flips the sign of the result.
    assertTrue(new NumberPreReleasePart(Integer.MAX_VALUE).compareTo(new NumberPreReleasePart(-1)) > 0);
    assertTrue(new NumberPreReleasePart(-1).compareTo(new NumberPreReleasePart(Integer.MAX_VALUE)) < 0);
    assertTrue(new NumberPreReleasePart(Integer.MIN_VALUE).compareTo(new NumberPreReleasePart(1)) < 0);
    assertEquals(new NumberPreReleasePart(5).compareTo(new NumberPreReleasePart(5)), 0);
  }

  @Test
  public void preReleaseConstructorGuards() {
    assertThrows(VersionException.class, () -> new PreRelease(""));
    assertThrows(VersionException.class, () -> new PreRelease("   "));
    assertThrows(VersionException.class, () -> new PreRelease((String) null));
    assertThrows(VersionException.class, () -> new PreRelease((PreReleasePart[]) null));
    assertThrows(VersionException.class, () -> new PreRelease(new StringPreReleasePart("a"), null));
    assertThrows(VersionException.class, () -> new PreRelease((PreReleasePart) null));

    // An empty PreRelease renders as "1.0.0-", which is not a parsable version.
    assertThrows(VersionException.class, PreRelease::new);
  }

  @Test
  public void preReleaseNumericOverflow() {
    // A numeric identifier too large for an int used to be silently demoted to a String part, which compares
    // alphabetically and therefore sorts incorrectly.
    assertThrows(VersionException.class, () -> new Version("1.0.0-99999999999"));
    assertThrows(VersionException.class, () -> new Version("1.0.0-alpha.2147483648"));
    assertEquals(new Version("1.0.0-2147483647").preRelease().parts(), List.of(new NumberPreReleasePart(Integer.MAX_VALUE)));
  }

  @Test
  public void preReleasePartsImmutable() {
    PreRelease preRelease = new Version("1.0.0-beta").preRelease();
    assertThrows(UnsupportedOperationException.class, () -> preRelease.parts().add(new StringPreReleasePart("x")));
    assertThrows(UnsupportedOperationException.class, () -> preRelease.parts().clear());
    assertThrows(UnsupportedOperationException.class, () -> preRelease.parts().set(0, null));
  }

  @Test
  public void string() {
    assertVersion("10.100.2000", 10, 100, 2000, false, false, true, false, false, null, null);
    assertVersion("0.0.0", 0, 0, 0, true, false, false, false, false, null, null);
    assertVersion("17", 17, 0, 0, true, false, false, false, false, null, null);
    assertVersion("3.4", 3, 4, 0, false, true, false, false, false, null, null);
    assertVersion("3.4.8", 3, 4, 8, false, false, true, false, false, null, null);

    PreRelease rc1 = new PreRelease(new StringPreReleasePart("RC1"));
    assertVersion("3-RC1", 3, 0, 0, false, false, false, true, false, rc1, null);
    assertVersion("3.4-RC1", 3, 4, 0, false, false, false, true, false, rc1, null);
    assertVersion("3.4.5-RC1", 3, 4, 5, false, false, false, true, false, rc1, null);

    PreRelease beta = new PreRelease(new StringPreReleasePart("beta"));
    PreRelease preRelease = new PreRelease(new StringPreReleasePart("beta"), new NumberPreReleasePart(2), new StringPreReleasePart("build"), new NumberPreReleasePart(4));
    PreRelease prePreRelease = new PreRelease(new StringPreReleasePart("pre-beta"), new NumberPreReleasePart(2), new StringPreReleasePart("build"), new NumberPreReleasePart(4));
    assertVersion("3.4.5-beta", 3, 4, 5, false, false, false, true, false, beta, null);
    assertVersion("3.4.5-beta.1", 3, 4, 5, false, false, false, true, false, new PreRelease(new StringPreReleasePart("beta"), new NumberPreReleasePart(1)), null);
    assertVersion("3.4.5-beta.2", 3, 4, 5, false, false, false, true, false, new PreRelease(new StringPreReleasePart("beta"), new NumberPreReleasePart(2)), null);
    assertVersion("3.4.5-beta.2.build.4", 3, 4, 5, false, false, false, true, false, preRelease, null);
    assertVersion("3.4.5-pre-beta.2.build.4", 3, 4, 5, false, false, false, true, false, prePreRelease, null);
    assertVersion("3.4.5-1-2.2", 3, 4, 5, false, false, false, true, false, new PreRelease(new StringPreReleasePart("1-2"), new NumberPreReleasePart(2)), null);

    PreRelease integration = new PreRelease(new StringPreReleasePart("{integration}"));
    assertVersion("3.4.5-{integration}", 3, 4, 5, false, false, false, true, true, integration, null);
    assertVersion("3.4.5-{integration}+metaData", 3, 4, 5, false, false, false, true, true, integration, "metaData");
    assertVersion("3.4.5-beta.{integration}", 3, 4, 5, false, false, false, true, true, new PreRelease(new StringPreReleasePart("beta"), new StringPreReleasePart("{integration}")), null);
    assertVersion("3.4.5-beta.{integration}+metaData", 3, 4, 5, false, false, false, true, true, new PreRelease(new StringPreReleasePart("beta"), new StringPreReleasePart("{integration}")), "metaData");

    assertVersion("3.4.5-beta+metaData", 3, 4, 5, false, false, false, true, false, beta, "metaData");
    assertVersion("3.4.5-beta.1+49393", 3, 4, 5, false, false, false, true, false, new Version.PreRelease(new StringPreReleasePart("beta"), new NumberPreReleasePart(1)), "49393");
    assertVersion("3.4.5-beta.2+foobar", 3, 4, 5, false, false, false, true, false, new PreRelease(new StringPreReleasePart("beta"), new NumberPreReleasePart(2)), "foobar");
    assertVersion("3.4.5-beta.2.build.4+30930927", 3, 4, 5, false, false, false, true, false, preRelease, "30930927");
    assertVersion("3.4.5-pre-beta.2.build.4+sha.f938de838ab", 3, 4, 5, false, false, false, true, false, prePreRelease, "sha.f938de838ab");
    assertVersion("3.4.5-1-2.2+meta-data", 3, 4, 5, false, false, false, true, false, new PreRelease(new StringPreReleasePart("1-2"), new NumberPreReleasePart(2)), "meta-data");

    assertVersion("3.4.5+metaData", 3, 4, 5, false, false, true, false, false, null, "metaData");
    assertVersion("3.4.5+49393", 3, 4, 5, false, false, true, false, false, null, "49393");
    assertVersion("3.4.5+foobar", 3, 4, 5, false, false, true, false, false, null, "foobar");
    assertVersion("3.4.5+30930927", 3, 4, 5, false, false, true, false, false, null, "30930927");
    assertVersion("3.4.5+sha.f938de838ab", 3, 4, 5, false, false, true, false, false, null, "sha.f938de838ab");
    assertVersion("3.4.5+meta-data", 3, 4, 5, false, false, true, false, false, null, "meta-data");

    assertBadVersion("-1.0.0");
    assertBadVersion("1.0.0.0");
    assertBadVersion("1.0.0.0.0");
    assertBadVersion("0.-1.0");
    assertBadVersion("0.0.-1");
    assertBadVersion("1.0.0-");
    assertBadVersion("1.0.0+");
    assertBadVersion("1.0.0.");
    assertBadVersion("-1.0.0-");
    assertBadVersion("+1.0.0+");
    assertBadVersion(".1.0.0.");
    assertBadVersion("-1.0.0");
    assertBadVersion("+1.0.0");
    assertBadVersion(".1.0.0");
    assertBadVersion("foo");
    assertBadVersion("0foo0foo0");
    assertBadVersion("foo.0.0");
    assertBadVersion("0.foo.0");
    assertBadVersion("0.0.foo");
    assertBadVersion("1.0.0-{integration}.beta"); // integration must be last
  }

  @Test
  public void stringPartGuards() {
    assertThrows(VersionException.class, () -> new StringPreReleasePart(null));
    assertThrows(VersionException.class, () -> new StringPreReleasePart(""));
    assertThrows(VersionException.class, () -> new StringPreReleasePart("   "));
  }

  @Test
  public void toIntegration() {
    assertTrue(new Version("1.2.3").toIntegrationVersion().isIntegration());
    assertEquals(new Version("1.2.3").toIntegrationVersion(), new Version("1.2.3-{integration}"));
    assertTrue(new Version("1.2.3-beta").toIntegrationVersion().isIntegration());
    assertEquals(new Version("1.2.3-beta").toIntegrationVersion(), new Version("1.2.3-beta.{integration}"));
    assertTrue(new Version("1.2.3-beta.2").toIntegrationVersion().isIntegration());
    assertEquals(new Version("1.2.3-beta.2").toIntegrationVersion(), new Version("1.2.3-beta.2.{integration}"));
  }

  private void assertBadVersion(String spec) {
    try {
      new Version(spec);
      fail("Should have failed with only a VersionException");
    } catch (VersionException _) {
      // Expected
    } catch (Exception e) {
      fail("Should have failed with only a VersionException", e);
    }
  }

  private void assertCompareTo(String spec1, String spec2) {
    Version v1 = new Version(spec1);
    Version v2 = new Version(spec2);
    int comparison = v1.compareTo(v2);
    assertTrue(comparison > 0);
    comparison = v2.compareTo(v1);
    assertTrue(comparison < 0);

    v1 = new Version(spec1);
    v2 = new Version(spec2 + "+bMetaData");
    comparison = v1.compareTo(v2);
    assertTrue(comparison > 0);
    comparison = v2.compareTo(v1);
    assertTrue(comparison < 0);

    v1 = new Version(spec1 + "+aMetaData");
    v2 = new Version(spec2);
    comparison = v1.compareTo(v2);
    assertTrue(comparison > 0);
    comparison = v2.compareTo(v1);
    assertTrue(comparison < 0);

    v1 = new Version(spec1 + "+aMetaData");
    v2 = new Version(spec2 + "+bMetaData");
    comparison = v1.compareTo(v2);
    assertTrue(comparison > 0);
    comparison = v2.compareTo(v1);
    assertTrue(comparison < 0);
  }

  private void assertVersion(String spec, int major, int minor, int patch, boolean isMajor, boolean isMinor,
                             boolean isPatch, boolean isPreRelease, boolean isIntegration, PreRelease preRelease, String metaData) {
    Version v = new Version(spec);
    assertEquals(v.major(), major);
    assertEquals(v.minor(), minor);
    assertEquals(v.patch(), patch);
    assertEquals(v.isMajor(), isMajor);
    assertEquals(v.isMinor(), isMinor);
    assertEquals(v.isPatch(), isPatch);
    assertEquals(v.isPreRelease(), isPreRelease);
    assertEquals(v.isIntegration(), isIntegration);
    assertEquals(v.preRelease(), preRelease);
    assertEquals(v.metaData(), metaData);
  }

  private void assertVersionEquals(String spec, int major, int minor, int patch, PreRelease preRelease) {
    Version v1 = new Version(spec);
    Version v2 = new Version(major, minor, patch, preRelease, null);
    assertEquals(v1, v2);

    v1 = new Version(spec + "+aMetaData");
    assertEquals(v1, v2);

    v1 = new Version(spec);
    v2 = new Version(major, minor, patch, preRelease, "bMetaData");
    assertEquals(v1, v2);

    v1 = new Version(spec + "+aMetaData");
    v2 = new Version(major, minor, patch, preRelease, "bMetaData");
    assertEquals(v1, v2);
  }
}
