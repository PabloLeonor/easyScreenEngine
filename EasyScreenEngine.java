/*
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║ EasyScreenEngine                                                                                                                 ║
║ Simple Engine for drawing in a java screen, in a native way						                                                           ║	
║ Only uses 4 functions, but its the base for making all the other functions that the users may need														   ║
║ por Pablo Leonor                                                                                                                 ║
║ 18-06-2026                                                                                                                       ║
║ EasyScreenEngine © 2026 by Pablo Leonor is licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╝
*/
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.Arrays;

public class EasyScreenEngine {

    private int width, height;
    private int backgroundColor;
    private boolean scalable;
    private boolean pixelPerfect;

    private int[] pixels;
    private BufferedImage image;
    private int[] imageBuffer;

    private JFrame frame;
    private JPanel panel;

    
    
    /**
     * Creates and displays a window with a pixel framebuffer.
     * <p>
     * This is the entry point of EasyScreenEngine. Call this once at the start
     * of your program before any {@link #setPixel} or {@link #update} calls.
     * </p>
     *
     * <pre>
     * engine.drawScreen(400, 300, "My Window", 0xFF000000, true, true);
     * </pre>
     *
     * @param w               screen width in pixels
     * @param h               screen height in pixels
     * @param title           window title
     * @param backgroundColor background color in ARGB format (e.g. {@code 0xFF000000} for black)
     * @param scalable        if {@code true}, the window can be resized
     * @param pixelPerfect    if {@code true}, pixels scale as solid blocks when resizing.
     *                        if {@code false}, the canvas resizes with the window, adding or removing pixels.
     *                        ignored when {@code scalable} is {@code false}.
     */
    public void drawScreen(int w, int h, String title, int backgroundColor, boolean scalable, boolean pixelPerfect) {
        //======VARIABLES======
    	width = w;
        height = h;
        pixels = new int[w * h];
        this.backgroundColor = backgroundColor;
        this.scalable = scalable;
        this.pixelPerfect = pixelPerfect;
        Arrays.fill(pixels, backgroundColor);
        
        
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); //creamos la imagen
        imageBuffer = ((DataBufferInt) image.getRaster()
                .getDataBuffer())
                .getData();
        
        frame = new JFrame(title); // creamos la ventana
        panel = new JPanel() {	
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (scalable && pixelPerfect) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                    double scaleX = (double) getWidth() / width;
                    double scaleY = (double) getHeight() / height;
                    int drawW = (int)(width * scaleX);
                    int drawH = (int)(height * scaleY);
                    g2d.drawImage(image, 0, 0, drawW, drawH, null);
                } else if (scalable && !pixelPerfect) {
                    g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g.drawImage(image, 0, 0, null);
                }
            }
        };
        
        panel.addComponentListener(new ComponentAdapter() { //Esto lo que hace es que mira el tema del redimensionado 
            @Override
            public void componentResized(ComponentEvent e) {
                if (scalable && !pixelPerfect) { //si el escalado es true pero no hay pixelperfect lo que ocurre es que se redimensiona la ventana pero no el contenido, esto es 
                    int newWidth = panel.getWidth();   //recereando pixels[]
                    int newHeight = panel.getHeight();
                    int[] newPixels = new int[newWidth * newHeight];
                    Arrays.fill(newPixels, backgroundColor);
                    int copyW = Math.min(width, newWidth);
                    int copyH = Math.min(height, newHeight);
                    for (int y = 0; y < copyH; y++) {
                        System.arraycopy(pixels, y * width, newPixels, y * newWidth, copyW);
                    }
                    width = newWidth;
                    height = newHeight;
                    pixels = newPixels;
                    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    imageBuffer = ((DataBufferInt) image.getRaster()
                            .getDataBuffer())
                            .getData();
                    update(); //se actualiza
                }
            }
        });
        panel.setPreferredSize(new Dimension(  //maquetación, aquí se coloca en la pantalla del ordenador las configuraciones previas
                scalable && !pixelPerfect ? w * 2 : w,
                scalable && !pixelPerfect ? h * 2 : h
        ));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(scalable);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        update();
    }

    /**
     * Renders the current framebuffer to the screen.
     * <p>
     * Call this once per frame after all {@link #setPixel} calls.
     * </p>
     *
     * <pre>
     * engine.clear();
     * engine.setPixel(10, 10, 0xFFFF0000);
     * engine.update(); // now it shows on screen
     * </pre>
     */
    public void update() {
        System.arraycopy(pixels, 0, imageBuffer, 0, pixels.length); // vuelca el framebuffer a la imagen
        panel.repaint(); 
    }
    
    /**
     * Clears the screen by filling the framebuffer with the background color.
     * <p>
     * Call this at the beginning of each frame before drawing pixels,
     * then call {@link #update()} when done to render the result.
     * </p>
     *
     * <pre>
     * while (true) {
     *     engine.clear();
     *     engine.setPixel(10, 10, 0xFFFF0000);
     *     engine.update();
     * }
     * </pre>
     */
    
    public void clear() {
        Arrays.fill(pixels, backgroundColor); //torna los píxeles de color del fondo
    }
    
    /**
     * Sets a pixel at the given coordinates with the given color.
     * <p>
     * Colors must be in ARGB format (e.g. {@code 0xFFFF0000} for opaque red).
     * Coordinates outside the screen bounds are silently ignored.
     * </p>
     *
     * <pre>
     * engine.setPixel(10, 10, 0xFFFF0000); // opaque red
     * engine.setPixel(20, 20, 0x80FF0000); // semi-transparent red
     * </pre>
     *
     * @param x     horizontal coordinate, left to right
     * @param y     vertical coordinate, top to bottom
     * @param color pixel color in ARGB format
     */
    public void setPixel(int x, int y, int color) {
        if (x < 0 || y < 0 || x >= width || y >= height) return; //gracias a esto, dibuja los píxeles dentro de la pantalla
        pixels[y * width + x] = color; // pinta los píxeles de X color en la posición dada, es un array, multiplica y*width para saber dónde pintarlo en vertical y luego pone la X
    }
}
