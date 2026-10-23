import { createHash, randomUUID } from "node:crypto";

import { z } from "zod";

import { insertRow, insertRowIfAbsent, PostgrestError, selectRows, updateRows } from "../db.ts";
import { writeAuditLog } from "../audit.ts";
import { ApiError } from "../errors.ts";
import { assertWithinRadius, distanceMeters, validateLocationAccuracy } from "../geo.ts";
import { requireActiveLandmark } from "./landmarks.ts";

const checkInSchema = z.object({
  landmarkId: z.string().min(1),
  challengeId: z.string().min(1),
  photoId: z.string().min(1),
  location: z.object({
    point: z.object({
      latitude: z.number().min(-90).max(90),
      longitude: z.number().min(-180).max(180),
    }),
    accuracyMeters: z.number().nonnegative(),
    capturedAt: z.string().datetime(),
  }),
});

interface CheckInRow {
  id: string;
  user_id: string;
  landmark_id: string;
  challenge_id: string;
  photo_id: string;
  distance_meters: number;
  accuracy_meters: number;
  idempotency_key: string;
  request_hash: string;
  created_at: string;
}

interface TaskRow {
  fragment_id: string;
}

interface FragmentRow {
  id: string;
  puzzle_id: string;
  piece_index: number;
}

interface GrantRow {
  id: string;
  user_id: string;
  fragment_id: string;
  check_in_id: string;
  granted_at: string;
}

export interface CheckInReceipt {
  checkInId: string;
  status: "COMPLETED" | "ALREADY_COMPLETED";
  fragmentGrant: {
    fragmentId: string;
    puzzleId: string;
    pieceIndex: number;
    grantedAt: string;
  };
}

function requestHash(input: z.infer<typeof checkInSchema>): string {
  const canonical = JSON.stringify([
    input.landmarkId,
    input.challengeId,
    input.photoId,
    input.location.point.latitude,
    input.location.point.longitude,
    input.location.accuracyMeters,
    input.location.capturedAt,
  ]);
  return createHash("sha256").update(canonical).digest("hex");
}

async function findCheckIn(
  field: "landmark_id" | "idempotency_key",
  userId: string,
  value: string,
): Promise<CheckInRow | null> {
  const rows = await selectRows<CheckInRow>("checkins", {
    select:
      "id,user_id,landmark_id,challenge_id,photo_id,distance_meters,accuracy_meters,idempotency_key,request_hash,created_at",
    user_id: `eq.${userId}`,
    [field]: `eq.${value}`,
    limit: "1",
  });
  return rows[0] ?? null;
}

async function getTask(landmarkId: string): Promise<TaskRow | null> {
  const rows = await selectRows<TaskRow>("tasks", {
    select: "fragment_id",
    landmark_id: `eq.${landmarkId}`,
    type: "eq.photo",
    limit: "1",
  });
  return rows[0] ?? null;
}

async function getFragment(fragmentId: string): Promise<FragmentRow | null> {
  const rows = await selectRows<FragmentRow>("fragments", {
    select: "id,puzzle_id,piece_index",
    id: `eq.${fragmentId}`,
    limit: "1",
  });
  return rows[0] ?? null;
}

export async function grantFragmentOnce(checkIn: CheckInRow): Promise<GrantRow> {
  const existing = await selectRows<GrantRow>("fragment_grants", {
    select: "id,user_id,fragment_id,check_in_id,granted_at",
    check_in_id: `eq.${checkIn.id}`,
    limit: "1",
  });
  if (existing[0]) {
    return existing[0];
  }

  const task = await getTask(checkIn.landmark_id);
  if (!task) {
    throw new ApiError("LANDMARK_NOT_FOUND", "An active photo task is not configured");
  }

  const inserted = await insertRowIfAbsent<GrantRow>("fragment_grants", {
    id: `grant_${randomUUID()}`,
    user_id: checkIn.user_id,
    fragment_id: task.fragment_id,
    check_in_id: checkIn.id,
  });
  if (inserted) {
    return inserted;
  }

  const afterInsert = await selectRows<GrantRow>("fragment_grants", {
    select: "id,user_id,fragment_id,check_in_id,granted_at",
    check_in_id: `eq.${checkIn.id}`,
    limit: "1",
  });
  if (afterInsert[0]) {
    return afterInsert[0];
  }

  throw new ApiError("INTERNAL_ERROR", "Unable to persist the fragment grant", {
    retryable: true,
  });
}

async function toReceipt(
  checkIn: CheckInRow,
  status: CheckInReceipt["status"],
): Promise<CheckInReceipt> {
  const grant = await grantFragmentOnce(checkIn);
  const fragment = await getFragment(grant.fragment_id);
  if (!fragment) {
    throw new ApiError("INTERNAL_ERROR", "The granted fragment is missing");
  }

  return {
    checkInId: checkIn.id,
    status,
    fragmentGrant: {
      fragmentId: fragment.id,
      puzzleId: fragment.puzzle_id,
      pieceIndex: fragment.piece_index,
      grantedAt: new Date(grant.granted_at).toISOString(),
    },
  };
}

async function claimChallenge(
  challengeId: string,
  userId: string,
  landmarkId: string,
): Promise<boolean> {
  const claimed = await updateRows<{ id: string }>(
    "challenges",
    {
      id: `eq.${challengeId}`,
      user_id: `eq.${userId}`,
      landmark_id: `eq.${landmarkId}`,
      used_at: "is.null",
      expires_at: `gt.${new Date().toISOString()}`,
    },
    { used_at: new Date().toISOString() },
  );
  return claimed.length > 0;
}

export async function submitCheckIn(
  userId: string,
  requestId: string,
  idempotencyKey: string,
  body: unknown,
): Promise<CheckInReceipt> {
  const parsed = checkInSchema.safeParse(body);
  if (!parsed.success) {
    throw new ApiError("INVALID_REQUEST", "Check-in request is invalid", {
      details: parsed.error.flatten(),
    });
  }
  const input = parsed.data;
  const hash = requestHash(input);

  if (!idempotencyKey || idempotencyKey.length > 128) {
    throw new ApiError("INVALID_REQUEST", "A valid Idempotency-Key header is required");
  }

  const completed = await findCheckIn("landmark_id", userId, input.landmarkId);
  if (completed) {
    return toReceipt(completed, "ALREADY_COMPLETED");
  }

  const prior = await findCheckIn("idempotency_key", userId, idempotencyKey);
  if (prior) {
    if (prior.request_hash !== hash) {
      throw new ApiError(
        "IDEMPOTENCY_CONFLICT",
        "The Idempotency-Key was already used with a different request",
      );
    }
    return toReceipt(prior, "COMPLETED");
  }

  const claimed = await claimChallenge(input.challengeId, userId, input.landmarkId);
  if (!claimed) {
    const raced = await findCheckIn("idempotency_key", userId, idempotencyKey);
    if (raced) {
      return toReceipt(raced, "COMPLETED");
    }
    throw new ApiError("CHALLENGE_EXPIRED", "Challenge is expired or already used", {
      retryable: true,
    });
  }

  const photoRows = await selectRows<{ id: string }>("photos", {
    select: "id",
    id: `eq.${input.photoId}`,
    user_id: `eq.${userId}`,
    limit: "1",
  });
  if (!photoRows[0]) {
    throw new ApiError("PHOTO_REQUIRED", "A valid uploaded photo is required");
  }

  validateLocationAccuracy(input.location.accuracyMeters);

  const landmark = await requireActiveLandmark(input.landmarkId);
  const distance = distanceMeters(input.location.point, {
    latitude: landmark.latitude,
    longitude: landmark.longitude,
  });
  assertWithinRadius(distance, landmark.check_in_radius_meters, input.location.accuracyMeters);

  let checkIn: CheckInRow;
  try {
    checkIn = await insertRow<CheckInRow>("checkins", {
      id: `chk_${randomUUID()}`,
      user_id: userId,
      landmark_id: input.landmarkId,
      challenge_id: input.challengeId,
      photo_id: input.photoId,
      distance_meters: distance,
      accuracy_meters: input.location.accuracyMeters,
      idempotency_key: idempotencyKey,
      request_hash: hash,
    });
  } catch (error) {
    if (error instanceof PostgrestError && error.status === 409) {
      const existing =
        (await findCheckIn("idempotency_key", userId, idempotencyKey)) ??
        (await findCheckIn("landmark_id", userId, input.landmarkId));
      if (existing) {
        if (existing.idempotency_key === idempotencyKey && existing.request_hash !== hash) {
          throw new ApiError(
            "IDEMPOTENCY_CONFLICT",
            "The Idempotency-Key was already used with a different request",
          );
        }
        return toReceipt(
          existing,
          existing.idempotency_key === idempotencyKey ? "COMPLETED" : "ALREADY_COMPLETED",
        );
      }
    }
    throw error;
  }

  const receipt = await toReceipt(checkIn, "COMPLETED");

  await writeAuditLog({
    requestId,
    eventType: "CHECKIN_GRANTED",
    userId,
    entityId: checkIn.id,
    metadata: {
      landmarkId: input.landmarkId,
      challengeId: input.challengeId,
      photoId: input.photoId,
      fragmentId: receipt.fragmentGrant.fragmentId,
      distanceMeters: distance,
      accuracyMeters: input.location.accuracyMeters,
    },
  });

  return receipt;
}
