import { describe, expect, it } from "vitest";

import { distanceMeters, isWithinRadius } from "../functions/api/geo.ts";

describe("distanceMeters", () => {
  it("givenSamePoint_whenCalculateDistance_thenReturnsZero", () => {
    const point = { latitude: 39.9592, longitude: 116.3083 };
    expect(distanceMeters(point, point)).toBe(0);
  });

  it("givenNearbyPoints_whenCalculateDistance_thenReturnsExpectedScale", () => {
    const distance = distanceMeters(
      { latitude: 39.9592, longitude: 116.3083 },
      { latitude: 39.9602, longitude: 116.3083 },
    );
    expect(distance).toBeGreaterThan(100);
    expect(distance).toBeLessThan(120);
  });
});

describe("isWithinRadius", () => {
  it("givenAccurateInRangeSample_whenCheckRadius_thenReturnsTrue", () => {
    expect(isWithinRadius(40, 100, 10)).toBe(true);
  });

  it("givenInaccurateSample_whenCheckRadius_thenReturnsFalse", () => {
    expect(isWithinRadius(40, 100, 51)).toBe(false);
  });
});
