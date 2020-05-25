public class MyPoint {
    public int x,y;
    public MyPoint(int _x,int _y) {
        x = _x;
        y = _y;
    }
    public static int cross_product(MyPoint p,MyPoint q) {
        return p.x * q.y - p.y * q.x;
    }
}
