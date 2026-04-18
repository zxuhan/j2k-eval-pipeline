package trickysyntax;

// HYPOTHESIS:
// Labelled `break outer;` in Java maps to `break@outer` in Kotlin. The
// conversion is direct, but j2k sometimes emits an un-labelled `break` when
// the label is the immediately enclosing loop, assuming it's redundant. That's
// correct for simple cases but WRONG when the labelled break is inside a
// nested inline-lambda scope — `forEach { }` for example — because the label
// is the only way to escape the inline boundary. Predicted: compiles when
// labels are redundant; may fail when nested inside inlined higher-order
// functions emitted by other j2k rewrites.
public final class LabelledBreak {
    public int find(int[][] grid, int target) {
        int row = -1;
        outer:
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (grid[i][j] == target) {
                    row = i;
                    break outer;
                }
            }
        }
        return row;
    }
}
