package szp.app;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class App {

    // The window handle
    private long window;
    private int oneTriangleVAO;
    private int oneTriangleShaderProgram;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() ){
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);  // not the fixed pipeline, the other one
        
        // Create the window
        window = glfwCreateWindow(800, 600, "OpenGL Test!", NULL, NULL);
        if ( window == NULL ){
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });
        
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // some additional info
        System.out.println("Vendor: " + glGetString(GL_VENDOR));
        System.out.println("Renderer: " + glGetString(GL_RENDERER));

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );            
        }
        // create content
        // shaders for a triangle
        try ( MemoryStack stack = stackPush() ) {                        
            // vertex shader program
            String vertexShaderSource = "#version 330 core\nlayout (location = 0) in vec3 aPos;\nvoid main()\n{\n  gl_Position = vec4(aPos.x, aPos.y, aPos.z, 1.0);\n}\0";
            IntBuffer vertexShader = stack.mallocInt(1);
            vertexShader.put(glCreateShader(GL_VERTEX_SHADER));
            glShaderSource(vertexShader.get(0), vertexShaderSource);
            glCompileShader(vertexShader.get(0));

            // check compile error
            IntBuffer success = stack.mallocInt(1);
            IntBuffer size = stack.mallocInt(1);
            size.put(0, 512);
            ByteBuffer infoLog = ByteBuffer.allocate(512);
            glGetShaderiv(vertexShader.get(0), GL_COMPILE_STATUS, success);
            if( success.get(0) == 0 ){
                glGetShaderInfoLog(vertexShader.get(0), size, infoLog);
                System.out.println("Vertex Shader compile error:" + infoLog.toString());
            }

            // fragment shader program
            String fragmentShaderSource = "#version 330 core\nout vec4 FragColor;\nvoid main()\n{\n  FragColor = vec4(1.0f, 0.5f, 0.2f, 1.0f);\n}\0";
            IntBuffer fragmentShader = stack.mallocInt(1);
            fragmentShader.put(glCreateShader(GL_FRAGMENT_SHADER));
            glShaderSource(fragmentShader.get(0), fragmentShaderSource);
            glCompileShader(fragmentShader.get(0));

            // check compile error
            success.clear();
            infoLog.clear();
            glGetShaderiv(fragmentShader.get(0), GL_COMPILE_STATUS, success);
            if( success.get() == 0 ){
                glGetShaderInfoLog(fragmentShader.get(0), size, infoLog);
                System.out.println(infoLog.toString());
            }
            
            // attach and link shader programs
            IntBuffer shaderProgram = stack.mallocInt(1);
            shaderProgram.put(glCreateProgram());
            oneTriangleShaderProgram = shaderProgram.get(0);

            glAttachShader(shaderProgram.get(0), vertexShader.get(0));
            glAttachShader(shaderProgram.get(0), fragmentShader.get(0));
            glLinkProgram(shaderProgram.get(0));
            // check program errors
            success.clear();
            infoLog.clear();
            glGetProgramiv(shaderProgram.get(0), GL_LINK_STATUS, success);
            if( success.get() == 0 ){
                glGetProgramInfoLog(shaderProgram.get(0), size, infoLog);
                System.out.println(infoLog.toString());
            }
            //glUseProgram(shaderProgram.get(0));
            glDeleteShader(vertexShader.get(0));
            glDeleteShader(fragmentShader.get(0));

            // vertex shader input vertices
            double[] vertices = new double[]{ -0.5, -0.5, 0.0,  0.05, -0.05, 0.0,  0.0, 0.05, 0.0 };

            // initialize a vertex array object
            IntBuffer VAO = stack.mallocInt(1);
            VAO.put(glGenVertexArrays());
            oneTriangleVAO = VAO.get(0);  // save it for later
            // initialize a vertex buffer object
            IntBuffer VBO = stack.mallocInt(1);
            glGenBuffers(VBO);

            // bind VAO and VBO
            glBindVertexArray(oneTriangleVAO);
            glBindBuffer(GL_ARRAY_BUFFER, VBO.get(0));

            // load input to VBO
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

            // set attribute format
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 3*Float.SIZE/8, 0);
            glEnableVertexAttribArray(0);
    
            // unbind VBO and VAO after they set
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        } // the stack frame is popped automatically, all malloced variable garbage collected

        // set viewport
        glViewport(0, 0, 800, 600);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        // Set the clear color
        glClearColor(0.05f, 0.12f, 0.05f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            glUseProgram(oneTriangleShaderProgram);
            glBindVertexArray(oneTriangleVAO);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            
            glfwSwapBuffers(window); // swap the color buffers
            glfwPollEvents();   // poll for window events
        }
    }

    public static void main(String[] args) {
        new App().run();
    }

}
