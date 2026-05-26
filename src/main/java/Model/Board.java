package Model;

import java.util.*;

public class Board {
    public static final int SIZE = 8;
    private Piece[][] grid = new Piece[SIZE][SIZE];

    public void init() {
        grid = new Piece[SIZE][SIZE];
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < SIZE; c++)
                if ((r+c)%2==1) grid[r][c] = new Piece(r, c, false);
        for (int r = 5; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if ((r+c)%2==1) grid[r][c] = new Piece(r, c, true);
    }

    public Piece get(int r, int c)  { return (inBounds(r,c)) ? grid[r][c] : null; }
    public void  set(int r, int c, Piece p) { grid[r][c] = p; }
    public void  remove(int r, int c)       { grid[r][c] = null; }
    public boolean empty(int r, int c)      { return inBounds(r,c) && grid[r][c]==null; }
    public boolean inBounds(int r, int c)   { return r>=0&&r<SIZE&&c>=0&&c<SIZE; }

    public Board copy() {
        Board b = new Board();
        for (int r=0;r<SIZE;r++)
            for (int c=0;c<SIZE;c++)
                if (grid[r][c]!=null) b.grid[r][c] = grid[r][c].copy();
        return b;
    }

    public int count(boolean red) {
        int n=0;
        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++)
            if (grid[r][c]!=null && grid[r][c].isRed()==red) n++;
        return n;
    }

    public int evaluate(boolean red) {
        int s=0;
        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++) {
            Piece p = grid[r][c]; if (p==null) continue;
            int v = p.isKing() ? 3 : 1;
            s += (p.isRed()==red) ? v : -v;
        }
        return s;
    }

    // ── Áp dụng nước đi ──────────────────────────────────────────────
    public void applyMove(Move m) {
        Piece p = get(m.getFromRow(), m.getFromCol());
        remove(m.getFromRow(), m.getFromCol());
        for (int[] cap : m.getCaptures()) remove(cap[0], cap[1]);
        p.setRow(m.getToRow()); p.setCol(m.getToCol());
        set(m.getToRow(), m.getToCol(), p);
        if ((p.isRed() && m.getToRow()==0) || (!p.isRed() && m.getToRow()==SIZE-1))
            p.setKing(true);
    }

    // ── Helper: lọc chỉ giữ nước ăn nhiều quân nhất ─────────────────
    /**
     * Trong danh sách caps, chỉ giữ lại những nước có số quân ăn được
     * bằng với số quân tối đa (bắt buộc ăn nhiều nhất).
     */
    private List<Move> filterMaxCaptures(List<Move> caps) {
        if (caps.isEmpty()) return caps;
        int max = caps.stream()
                .mapToInt(m -> m.getCaptures().size())
                .max()
                .getAsInt();
        List<Move> result = new ArrayList<>();
        for (Move m : caps)
            if (m.getCaptures().size() == max) result.add(m);
        return result;
    }

    // ── Tìm nước đi hợp lệ ─────────────────────────────────────────
    /** Tất cả nước đi hợp lệ của bên isRed (ưu tiên + bắt buộc ăn nhiều nhất). */
    public List<Move> validMoves(boolean isRed) {
        List<Move> caps = new ArrayList<>(), normals = new ArrayList<>();
        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++) {
            Piece p = grid[r][c]; if (p==null || p.isRed()!=isRed) continue;
            caps.addAll(findCaptures(r, c, isRed, p.isKing(), new boolean[64], new Move(r,c)));
        }
        // Nếu có nước ăn → chỉ trả về những nước ăn NHIỀU NHẤT
        if (!caps.isEmpty()) return filterMaxCaptures(caps);

        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++) {
            Piece p = grid[r][c]; if (p==null || p.isRed()!=isRed) continue;
            normals.addAll(findNormals(r, c, isRed, p.isKing()));
        }
        return normals;
    }

    /** Nước đi hợp lệ cho một quân cụ thể (tôn trọng forced capture + max capture). */
    public List<Move> movesForPiece(int row, int col, boolean isRed) {
        // Lấy toàn bộ capture hợp lệ của cả bên (đã lọc max)
        List<Move> allValid = validMoves(isRed);
        boolean anyCapture = allValid.stream().anyMatch(Move::isCapture);

        Piece p = get(row, col); if (p==null) return Collections.emptyList();

        if (anyCapture) {
            // Tìm capture của riêng quân này
            List<Move> pieceCaps = findCaptures(row, col, isRed, p.isKing(),
                    new boolean[64], new Move(row, col));
            // Chỉ giữ những nước khớp với số quân ăn tối đa toàn bàn
            int globalMax = allValid.get(0).getCaptures().size(); // allValid đã lọc max
            List<Move> result = new ArrayList<>();
            for (Move m : pieceCaps)
                if (m.getCaptures().size() == globalMax) result.add(m);
            return result;
        }
        return findNormals(row, col, isRed, p.isKing());
    }

    private List<Move> findNormals(int r, int c, boolean isRed, boolean isKing) {
        List<Move> res = new ArrayList<>();
        for (int[] d : dirs(isRed, isKing)) {
            int nr=r+d[0], nc=c+d[1];
            if (empty(nr,nc)) { Move m=new Move(r,c); m.addStep(nr,nc); res.add(m); }
        }
        return res;
    }

    private List<Move> findCaptures(int r, int c, boolean isRed, boolean isKing,
                                    boolean[] used, Move cur) {
        List<Move> res = new ArrayList<>();
        for (int[] d : dirs(isRed, isKing)) {
            int mr=r+d[0], mc=c+d[1], nr=r+2*d[0], nc=c+2*d[1];
            if (!inBounds(mr,mc)||!inBounds(nr,nc)) continue;
            Piece mid = get(mr,mc);
            if (mid==null || mid.isRed()==isRed || used[mr*8+mc] || !empty(nr,nc)) continue;

            Move next = new Move(cur); next.addStep(nr,nc); next.addCapture(mr,mc);
            boolean[] u2 = used.clone(); u2[mr*8+mc]=true;
            boolean crown = !isKing && ((isRed&&nr==0)||(!isRed&&nr==7));
            if (crown) { res.add(next); continue; }
            List<Move> more = findCaptures(nr,nc,isRed,isKing,u2,next);
            if (more.isEmpty()) res.add(next); else res.addAll(more);
        }
        return res;
    }

    private int[][] dirs(boolean isRed, boolean isKing) {
        if (isKing) return new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}};
        return isRed ? new int[][]{{-1,-1},{-1,1}} : new int[][]{{1,-1},{1,1}};
    }
}