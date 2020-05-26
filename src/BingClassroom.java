import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

enum DrawTool {
    EMPTY,PEN,RECT,RECTF,CIR,LINE,TRI,ERASER,MOVE
}

class Main {
    public static Random ran;
    public static OpenGLApp app;
    public static DrawTool ToolSel = DrawTool.PEN;
    public static Color foreColor = Color.black;
    public static Map<Long,Shape> graph_store;
    public static ServerConn conn;
    public static void main(String[] argv) {
        ran = new Random();
        graph_store = new ConcurrentHashMap<Long,Shape>();//ConcurrentHashMap自带线程安全
        app = new OpenGLApp();
        conn = new ServerConn();
        String server_addr = JOptionPane.showInputDialog("请输入服务器地址:端口。若要本地使用请点击取消");
        if (server_addr != null) {
            int pos = server_addr.lastIndexOf(':');
            String host;
            int port;
            if (pos == -1) {
                host = server_addr;
                port = 2333;
            }
            else {
                host = server_addr.substring(0,pos);
                port = Integer.parseInt(server_addr.substring(pos+1));
            }
            boolean stat = conn.connect(host,port);
            if (!stat) {
                JOptionPane.showMessageDialog(new JFrame(), "连接服务器失败，自动回落到本地使用", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public static void setShape(Long id, Shape g) {
        graph_store.put(id,g);
        if (conn.conn) conn.out.println(g.serialize()+" "+id.toString());
    }
    public static void delShape(Long id) {
        graph_store.remove(id);
        if (conn.conn) conn.out.println("delete "+id.toString());
    }
    public static void clearShapes() {
        graph_store.clear();
        if (conn.conn) conn.out.println("clear");
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
    Board() {
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
                        repaint();
                        break;
                    case LINE:
                        preview = new MyLine(new MyPoint(mouse_x,mouse_y),new MyPoint(mouseEvent.getX(),mouseEvent.getY()),Main.foreColor);
                        repaint();
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
                        repaint();
                        break;
                    case RECT:
                        preview = new MyRectangle(new MyPoint(mouse_x,mouse_y),new MyPoint(mouseEvent.getX(),mouseEvent.getY()),Main.foreColor);
                        repaint();
                        break;
                    case RECTF:
                        preview = new MyRectangleFill(new MyPoint(mouse_x,mouse_y),new MyPoint(mouseEvent.getX(),mouseEvent.getY()),Main.foreColor);
                        repaint();
                        break;
                    case CIR:
                        preview = new MyCircle(new MyPoint(mouse_x,mouse_y),new MyPoint(mouseEvent.getX(),mouseEvent.getY()),Main.foreColor);
                        repaint();
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
                        repaint();
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
                    repaint();
                }
                else if (triangle_step == 2) {
                    preview = new MyTriangle(preview.p1,preview.p2,new MyPoint(mouseEvent.getX(), mouseEvent.getY()),Main.foreColor);
                    repaint();
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
                    repaint();
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
                    repaint();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                switch (Main.ToolSel) {
                    case RECT:
                    case RECTF:
                    case CIR:
                    case LINE:
                        if (preview != null) {
                            Main.setShape(Main.ran.nextLong(),preview);
                        }
                        preview = null;
                        repaint();
                        break;
                    case MOVE:
                        if (cur_sel != -1L) {
                            Main.setShape(cur_sel,Main.graph_store.get(cur_sel));
                            cur_sel = -1L;
                        }
                        repaint();
                        break;
                    default:
                        break;
                }
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
class ServerConn {
    public String addr;
    public boolean conn = false;
    private Socket client;
    BufferedReader buf;
    PrintStream out;
    Thread recv = new Thread() {
        public void run() {
            while (conn) {
                try {
                    String data = buf.readLine();
                    if (data == null || data.equals("")) {
                        conn = false;
                    }
                    String[] param = data.split(" ");
                    if (param[0].equals("clear")) {
                        Main.graph_store.clear();
                    }
                    else if (param[0].equals("delete")) {
                        Main.graph_store.remove(Long.parseLong(param[1]));
                    }
                    else if (param[0].equals("circle")) {
                        MyCircle to_put = new MyCircle(
                                new MyPoint(Integer.parseInt(param[1]),Integer.parseInt(param[2])),
                                new MyPoint(Integer.parseInt(param[3]),Integer.parseInt(param[4])),
                                new Color(Integer.parseInt(param[5]),Integer.parseInt(param[6]),Integer.parseInt(param[7]))
                        );
                        to_put.size = Integer.parseInt(param[8]);
                        to_put.move_enable = param[9].equals("1");
                        Main.graph_store.put(Long.parseLong(param[10]),to_put);
                    }
                    else if (param[0].equals("line")) {
                        MyLine to_put = new MyLine(
                                new MyPoint(Integer.parseInt(param[1]),Integer.parseInt(param[2])),
                                new MyPoint(Integer.parseInt(param[3]),Integer.parseInt(param[4])),
                                new Color(Integer.parseInt(param[5]),Integer.parseInt(param[6]),Integer.parseInt(param[7]))
                        );
                        to_put.size = Integer.parseInt(param[8]);
                        to_put.move_enable = param[9].equals("1");
                        Main.graph_store.put(Long.parseLong(param[10]),to_put);
                    }
                    else if (param[0].equals("rect")) {
                        MyRectangle to_put = new MyRectangle(
                                new MyPoint(Integer.parseInt(param[1]),Integer.parseInt(param[2])),
                                new MyPoint(Integer.parseInt(param[3]),Integer.parseInt(param[4])),
                                new Color(Integer.parseInt(param[5]),Integer.parseInt(param[6]),Integer.parseInt(param[7]))
                        );
                        to_put.size = Integer.parseInt(param[8]);
                        to_put.move_enable = param[9].equals("1");
                        Main.graph_store.put(Long.parseLong(param[10]),to_put);
                    }
                    else if (param[0].equals("rectf")) {
                        MyRectangleFill to_put = new MyRectangleFill(
                                new MyPoint(Integer.parseInt(param[1]),Integer.parseInt(param[2])),
                                new MyPoint(Integer.parseInt(param[3]),Integer.parseInt(param[4])),
                                new Color(Integer.parseInt(param[5]),Integer.parseInt(param[6]),Integer.parseInt(param[7]))
                        );
                        to_put.size = Integer.parseInt(param[8]);
                        to_put.move_enable = param[9].equals("1");
                        Main.graph_store.put(Long.parseLong(param[10]),to_put);
                    }
                    else if (param.equals("triangle")) {
                        MyTriangle to_put = new MyTriangle(
                                new MyPoint(Integer.parseInt(param[1]),Integer.parseInt(param[2])),
                                new MyPoint(Integer.parseInt(param[3]),Integer.parseInt(param[4])),
                                new MyPoint(Integer.parseInt(param[10]),Integer.parseInt(param[11])),
                                new Color(Integer.parseInt(param[5]),Integer.parseInt(param[6]),Integer.parseInt(param[7]))
                        );
                        to_put.size = Integer.parseInt(param[8]);
                        to_put.move_enable = param[9].equals("1");
                        Main.graph_store.put(Long.parseLong(param[12]),to_put);
                    }
                } catch (IOException e) {
                    conn = false;
                }
                catch (NullPointerException e) {
                    conn = false;
                }
                Main.app.canvas.repaint();
            }
            JOptionPane.showMessageDialog(new JFrame(), "与服务器失去连接", "错误", JOptionPane.ERROR_MESSAGE);
        }
    };
    public Boolean connect(String host,int port) {
        try {
            client = new Socket(host,port);
            buf = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintStream(client.getOutputStream(),true);
        } catch (IOException e) {
            conn = false;
            System.out.println("error");
            return false;
        }
        conn = true;
        recv.start();
        return true;
    }

}