import java.awt.*;

abstract public class Shape {
    public MyPoint p1,p2;
    public Color clr;
    public int size = 4;
    public boolean move_enable = true;
    abstract public Boolean inShape(int x, int y);
    void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(size));
        g.setColor(clr);
    }
    void move(int dx,int dy) {
        p1.x += dx;
        p1.y += dy;
        p2.x += dx;
        p2.y += dy;
    }
    //TODO: Serialize
}
class MyLine extends Shape {
    MyLine(MyPoint _p1,MyPoint _p2,Color _clr) {
        p1 = _p1;
        p2 = _p2;
        clr = _clr;
    }
    void draw(Graphics g) {
        super.draw(g);
        g.drawLine(p1.x,p1.y,p2.x,p2.y);
    }
    @Override
    public Boolean inShape(int x, int y) {
        int x1 = Math.min(p1.x,p2.x);
        int x2 = Math.max(p1.x,p2.x);
        int y1 = Math.min(p1.y,p2.y);
        int y2 = Math.max(p1.y,p2.y);
        if (x1-size <= x && x <= x2+size && y1-size <= y && y <= y2+size) {
            double dx = p1.x - p2.x;
            double dy = p1.y - p2.y;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < size * 4) return true;
            int s = Math.abs(MyPoint.cross_product(new MyPoint(p1.x - x, p1.y - y), new MyPoint(p2.x - x, p2.y - y)));
            return s <= size * len;
        }
        else return false;
    }
}

class MyRectangle extends Shape {
    public MyRectangle(MyPoint _p1,MyPoint _p2,Color _clr) {
        clr = _clr;
        p1 = _p1;
        p2 = _p2;
        size = 4;
    }
    @Override
    public Boolean inShape(int x, int y) {
        int x1 = Math.min(p1.x,p2.x);
        int x2 = Math.max(p1.x,p2.x);
        int y1 = Math.min(p1.y,p2.y);
        int y2 = Math.max(p1.y,p2.y);
        if (x1 <= x && x <= x2 && y1 <= y && y <= y2) {
            return !(x1+size <= x && x <= x2-size && y1+size <= y && y <= y2-size);
        }
        return false;
    }
    void draw(Graphics g) {
        super.draw(g);
        int x1 = Math.min(p1.x,p2.x);
        int x2 = Math.max(p1.x,p2.x);
        int y1 = Math.min(p1.y,p2.y);
        int y2 = Math.max(p1.y,p2.y);
        g.drawRect(x1,y1,x2-x1,y2-y1);
    }
}
class MyRectangleFill extends Shape {
    public MyRectangleFill(MyPoint _p1,MyPoint _p2,Color _clr) {
        clr = _clr;
        p1 = _p1;
        p2 = _p2;
    }
    @Override
    public Boolean inShape(int x, int y) {
        int x1 = Math.min(p1.x,p2.x);
        int x2 = Math.max(p1.x,p2.x);
        int y1 = Math.min(p1.y,p2.y);
        int y2 = Math.max(p1.y,p2.y);
        return x1 <= x && x <= x2 && y1 <= y && y <= y2;
    }
    void draw(Graphics g) {
        super.draw(g);
        int x1 = Math.min(p1.x,p2.x);
        int x2 = Math.max(p1.x,p2.x);
        int y1 = Math.min(p1.y,p2.y);
        int y2 = Math.max(p1.y,p2.y);
        g.fillRect(x1,y1,x2-x1,y2-y1);
    }
}
class MyCircle extends Shape {
    public MyCircle(MyPoint _p1,MyPoint _p2,Color _clr) {
        clr = _clr;
        p1 = _p1;
        p2 = _p2;
    }
    @Override
    public Boolean inShape(int x, int y) {
        double cx = ((double)p1.x + p2.x) / 2;
        double cy = ((double)p1.y + p2.y) / 2;
        double xx = x - cx;
        double yy = y - cy;
        double a = Math.max(p1.x,p2.x) - cx;
        double b = Math.max(p1.y,p2.y) - cy;
        return xx * xx / (a * a) + yy * yy / (b * b) <= 1.0;
    }
    void draw(Graphics g) {
        super.draw(g);
        int x1 = Math.min(p1.x,p2.x);
        int x2 = Math.max(p1.x,p2.x);
        int y1 = Math.min(p1.y,p2.y);
        int y2 = Math.max(p1.y,p2.y);
        g.fillOval(x1,y1,x2-x1,y2-y1);
    }
}
class MyTriangle extends Shape{
    public MyPoint p3;
    public MyTriangle(MyPoint _p1,MyPoint _p2,MyPoint _p3,Color _clr) {
        p1 = _p1;
        p2 = _p2;
        p3 = _p3;
        clr = _clr;
    }
    @Override
    public Boolean inShape(int x, int y) {
        boolean f[] = {
                MyPoint.cross_product(new MyPoint(p1.x-x,p1.y-y),new MyPoint(p2.x-x,p2.y-y)) > 0,
                MyPoint.cross_product(new MyPoint(p2.x-x,p2.y-y),new MyPoint(p3.x-x,p3.y-y)) > 0,
                MyPoint.cross_product(new MyPoint(p3.x-x,p3.y-y),new MyPoint(p1.x-x,p1.y-y)) > 0
        };
        return f[0] == f[1] && f[1] == f[2];
    }
    public void draw(Graphics g) {
        super.draw(g);
        int x[] = {p1.x,p2.x,p3.x};
        int y[] = {p1.y,p2.y,p3.y};
        g.fillPolygon(x,y,3);
    }
    @Override
    public void move(int dx,int dy) {
        super.move(dx,dy);
        p3.x += dx;
        p3.y += dy;
    }
}