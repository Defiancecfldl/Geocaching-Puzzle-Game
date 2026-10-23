import { selectRows } from "../db.ts";
import { ApiError } from "../errors.ts";

interface PuzzleRow {
  id: string;
  name: string;
  rows: number;
  columns: number;
}

interface FragmentRow {
  id: string;
  piece_index: number;
  image_key: string;
}

interface GrantRow {
  fragment_id: string;
}

export async function getPuzzleProgress(userId: string, puzzleId: string) {
  const puzzles = await selectRows<PuzzleRow>("puzzles", {
    select: "id,name,rows,columns",
    id: `eq.${puzzleId}`,
    limit: "1",
  });
  const puzzle = puzzles[0];
  if (!puzzle) {
    throw new ApiError("PUZZLE_NOT_FOUND", "Puzzle does not exist");
  }

  const [fragments, grants] = await Promise.all([
    selectRows<FragmentRow>("fragments", {
      select: "id,piece_index,image_key",
      puzzle_id: `eq.${puzzleId}`,
      order: "piece_index.asc",
    }),
    selectRows<GrantRow>("fragment_grants", {
      select: "fragment_id",
      user_id: `eq.${userId}`,
    }),
  ]);

  const owned = new Set(grants.map((grant) => grant.fragment_id));
  const ownedFragments = fragments
    .filter((fragment) => owned.has(fragment.id))
    .map((fragment) => ({
      fragmentId: fragment.id,
      pieceIndex: fragment.piece_index,
      imageKey: fragment.image_key,
    }));

  return {
    puzzleId: puzzle.id,
    name: puzzle.name,
    rows: puzzle.rows,
    columns: puzzle.columns,
    ownedFragmentIds: ownedFragments.map((fragment) => fragment.fragmentId),
    fragments: ownedFragments,
    completed: ownedFragments.length === puzzle.rows * puzzle.columns,
  };
}
