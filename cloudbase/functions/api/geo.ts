import { ApiError } from "./errors.ts";

const EARTH_RADIUS_METERS = 6_371_008.8;

function toRadians(value: number): number {
  return (value * Math.PI) / 180;
}

export function distanceMeters(
  from: { latitude: number; longitude: number },
  to: { latitude: number; longitude: number },
): number {
  const latitudeDelta = toRadians(to.latitude - from.latitude);
  const longitudeDelta = toRadians(to.longitude - from.longitude);
  const fromLatitude = toRadians(from.latitude);
  const toLatitude = toRadians(to.latitude);

  const haversine =
    Math.sin(latitudeDelta / 2) ** 2 +
    Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.sin(longitudeDelta / 2) ** 2;

  return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(haversine)));
}

export function isWithinRadius(
  distance: number,
  radiusMeters: number,
  accuracyMeters: number,
): boolean {
  return distance <= radiusMeters && accuracyMeters <= 50;
}

export function validateLocationAccuracy(accuracyMeters: number): void {
  if (accuracyMeters > 50) {
    throw new ApiError("LOCATION_ACCURACY_TOO_LOW", "Location accuracy must be 50m or better", {
      details: { accuracyMeters },
    });
  }
}

export function assertWithinRadius(
  distance: number,
  radiusMeters: number,
  accuracyMeters: number,
): void {
  if (!isWithinRadius(distance, radiusMeters, accuracyMeters)) {
    throw new ApiError("OUT_OF_RANGE", "Current location is outside the check-in radius", {
      details: {
        distanceMeters: distance,
        radiusMeters,
      },
    });
  }
}
