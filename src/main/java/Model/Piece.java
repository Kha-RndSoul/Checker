package Model;

public class Piece {
    private int row, col;
    private final boolean red; // true = đỏ (người chơi), false = đen (AI hoặc P2)
    private boolean king;

    public Piece(int row, int col, boolean red) {
        this.row = row; this.col = col; this.red = red;
    }

    public Piece copy() {
        Piece p = new Piece(row, col, red);
        p.king = this.king;
        return p;
    }

    public int getRow()              { return row; }
    public void setRow(int r)        { this.row = r; }
    public int getCol()              { return col; }
    public void setCol(int c)        { this.col = c; }
    public boolean isRed()           { return red; }
    public boolean isKing()          { return king; }
    public void setKing(boolean k)   { this.king = k; }
}