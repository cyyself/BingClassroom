import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

enum DrawTool {
    EMPTY,PEN,RECT,RECTF,CIR,LINE,TRI,ERASER,MOVE
}

class Main {
    public static Random ran;
    public static OpenGLApp app;
    public static DrawTool ToolSel = DrawTool.PEN;
    public static Color foreColor = Color.black;
    public static Map<Long,Shape> graph_store;
    public static void main(String[] argv) {
        ran = new Random();
        graph_store = new HashMap<Long,Shape>();
        app = new OpenGLApp();
    }
    public static void setShape(Long id, Shape g) {
        graph_store.put(id,g);
        //TODO: send to tcp socket
    }
    public static void delShape(Long id) {
        graph_store.remove(id);
        //TODO: send to tcp socket
    }
    public static void clearShapes() {
        graph_store.clear();
        //TODO: send to tcp socket
    }
}
class OpenGLApp extends JFrame{
    public Board canvas = new Board();
    public Toolbox toolbox = new Toolbox();
    public OpenGLApp() {
        setTitle("靐课堂");
        setSize(480,320);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(canvas,BorderLayout.CENTER);
        add(toolbox,BorderLayout.WEST);
        setVisible(true);
    }
}

class Board extends JPanel {
    private int mouse_x = 0, mouse_y = 0;
    private Long cur_sel = -1L;
    private Shape preview = null;
    private int triangle_step = 0;
    public void paint(Graphics g) {
        //JPanel自带双缓冲
        super.paint(g);
        for (Map.Entry<Long, Shape> entry : Main.graph_store.entrySet()) {
            Shape to_draw = entry.getValue();
            to_draw.draw(g);
        }
        if (preview != null) preview.draw(g);
    }
    Thread auto_refresh = new Thread() {
        public void run() {
            while (true) {
                repaint();
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    Board() {
        auto_refresh.start();
        setBackground(Color.WHITE);
        var that = this;
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                switch (Main.ToolSel) {
                    case PEN:
                        MyLine ng = new MyLine(new MyPoint(mouse_x,mouse_y),new MyPoint(mouseEvent.getX(),mouseEvent.getY()),Main.foreColor);
                        ng.move_enable = false;
                        Main.setShape(Main.ran.nextLong(),ng);
                        mouse_x = mouseEvent.getX();
                        mouse_y = mouseEvent.getY();
                        break;
                    case LINE:
                        preview = new MyLine(new MyPoint(mouse_x,mouse_y),new MyPoint(mouseEvent.getX(),mouseEvent.getY()),Main.foreColor);
                        break;
                    case ERASER:
                        mouse_x = mouseEvent.getX();
                        mouse_y = mouseEvent.getY();
                        for (Map.Entry<Long, Shape> entry : Main.graph_store.entrySet()) {
                            if (entry.getValue().inShape(mouse_x,mouse_y)) {
                                Main.delShape(entry.getKey());
                                break;
                            }
                        }
                        break;
                    case RECT:
                        preview = new MyRectangle(new MyPoint(mouse_x,mouse_y),new MyPoint(mouseEvent.getX(),mouseEvent.getY()),Main.foreColor);
                        break;
                    case RECTF:
                        preview = new MyRectangleFill(new MyPoint(mouse_x,mouse_y),new MyPoint(mouseEvent.getX(),mouseEvent.getY()),Main.foreColor);
                        break;
                    case CIR:
                        preview = new MyCircle(new MyPoint(mouse_x,mouse_y),new MyPoint(mouseEvent.getX(),mouseEvent.getY()),Main.foreColor);
                        break;
                    case MOVE:
                        if (cur_sel != -1L) {
                            Shape obj = Main.graph_store.get(cur_sel);
                            int dx = mouseEvent.getX() - mouse_x;
                            int dy = mouseEvent.getY() - mouse_y;
                            obj.move(dx,dy);
                            mouse_x = mouseEvent.getX();
                            mouse_y = mouseEvent.getY();
                        }
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void mouseMoved(MouseEvent mouseEvent) {
                if (Main.ToolSel != DrawTool.TRI) triangle_step = 0;
                if (triangle_step == 1) {
                    preview = new MyLine(new MyPoint(mouse_x,mouse_y),new MyPoint(mouseEvent.getX(),mouseEvent.getY()),Main.foreColor);
                }
                else if (triangle_step == 2) {
                    preview = new MyTriangle(preview.p1,preview.p2,new MyPoint(mouseEvent.getX(), mouseEvent.getY()),Main.foreColor);
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                cur_sel = -1L;
                mouse_x = e.getX();
                mouse_y = e.getY();
                if (Main.ToolSel != DrawTool.TRI) triangle_step = 0;
                if (Main.ToolSel == DrawTool.MOVE) {
                    for (Map.Entry<Long, Shape> entry : Main.graph_store.entrySet()) {
                        if (entry.getValue().move_enable && entry.getValue().inShape(mouse_x,mouse_y)) {
                            cur_sel = entry.getKey();
                            break;
                        }
                    }
                }
                else if (Main.ToolSel == DrawTool.TRI) {
                    if (triangle_step == 0) {
                        triangle_step = 1;
                    }
                    else if (triangle_step == 1) {
                        preview = new MyTriangle(preview.p1,preview.p2,preview.p2,Main.foreColor);
                        triangle_step = 2;
                    }
                    else if (triangle_step == 2) {
                        Main.setShape(Main.ran.nextLong(),preview);
                        triangle_step = 0;
                        preview = null;
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                switch (Main.ToolSel) {
                    case RECT:
                    case RECTF:
                    case CIR:
                    case LINE:
                        if (preview != null) Main.setShape(Main.ran.nextLong(),preview);
                        preview = null;
                        break;
                    case MOVE:
                        if (cur_sel != -1L) {
                            Main.setShape(cur_sel,Main.graph_store.get(cur_sel));
                            cur_sel = -1L;
                        }
                        break;
                    default:
                        break;
                }
                super.mouseReleased(e);
            }
        });
    }
}
class Toolbox extends JPanel {
    public JRadioButton pen,rect,rectF,cir,line,tri,eraser,move;
    public ButtonGroup bg;
    public JButton clear,foreColorBtn;
    public void refreshBtnColor() {
        foreColorBtn.setBackground(Main.foreColor);
        foreColorBtn.setForeground(revColor(Main.foreColor));
    }
    private Color revColor(Color a) {
        int r = a.getRed();
        int g = a.getGreen();
        int b = a.getBlue();
        return new Color(255-r,255-g,255-b);
    }
    public Toolbox() {
        pen = new JRadioButton("画笔",true);
        rect = new JRadioButton("矩形框",false);
        rectF = new JRadioButton("矩形",false);
        cir = new JRadioButton("圆形",false);
        line = new JRadioButton("直线",false);
        tri = new JRadioButton("三角形",false);
        eraser = new JRadioButton("橡皮擦",false);
        move = new JRadioButton("移动",false);
        bg = new ButtonGroup();
        clear = new JButton("清屏");
        foreColorBtn = new JButton("颜色");
        foreColorBtn.setOpaque(true);
        foreColorBtn.setBorderPainted(false);
        setLayout(new GridLayout(10,1));
        bg.add(pen);
        bg.add(rect);
        bg.add(rectF);
        bg.add(cir);
        bg.add(line);
        bg.add(tri);
        bg.add(eraser);
        bg.add(move);
        add(pen);
        add(rect);
        add(rectF);
        add(cir);
        add(line);
        add(tri);
        add(eraser);
        add(move);
        add(clear);
        add(foreColorBtn);
        refreshBtnColor();
        pen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.ToolSel = DrawTool.PEN;
            }
        });
        rectF.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.ToolSel = DrawTool.RECTF;
            }
        });
        rect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.ToolSel = DrawTool.RECT;
            }
        });
        cir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.ToolSel = DrawTool.CIR;
            }
        });
        line.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.ToolSel = DrawTool.LINE;
            }
        });
        tri.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.ToolSel = DrawTool.TRI;
            }
        });
        eraser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.ToolSel = DrawTool.ERASER;
            }
        });
        move.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.ToolSel = DrawTool.MOVE;
            }
        });
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.clearShapes();
            }
        });
        Toolbox that = this;
        foreColorBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Color newColor = JColorChooser.showDialog(that,"设置前景色",Main.foreColor);
                if (newColor != null) Main.foreColor = newColor;
                refreshBtnColor();
            }
        });
    }
}