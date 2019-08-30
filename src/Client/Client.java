/* CHAT ROOM <BasicListener.java>
 *  EE422C Project 7 submission by
 *  Replace <...> with your actual data.
 *  Ali Mansoorshahi
 *  AM85386
 *  Slip days used: <0>
 *  Spring 2019
 */

package Client;

import ActionUtils.MailBox;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Scanner;


public class Client extends Application {

    //todo streams for talking to servers
    private BufferedReader reader;
    private PrintWriter writer;
    private GridPane root;
    private ArrayList<ServerEntry> serverEntries;
    private GridPane serverList;
    private TextArea incomingMessageArea;
    private TextField sendField;
    private Socket currentServer;
    private ArrayList<Thread> pool;
    private MailBox<String> name;
    private MailBox<String> password;

    public static void main(String args[]) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //todo add the elements to sending and getting texts
        name = new MailBox<>();
        password = new MailBox<>();
        pool = new ArrayList<>();
        incomingMessageArea = new TextArea();
        sendField = new TextField();
        root = new GridPane();
        ScrollPane serverBrowser = new ScrollPane();
        serverEntries = new ArrayList<>();
        serverList = new GridPane();
        serverList.setVgap(10);
        serverBrowser.setContent(createServerContent());
        serverBrowser.setPrefViewportWidth(40);

        sendField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode() == KeyCode.ENTER) {
                    sendMessage(sendField.getText());
                    sendField.setText("");
                }
            }
        });

        ScrollPane msgParent = new ScrollPane(incomingMessageArea);
        incomingMessageArea.prefWidthProperty().bind(msgParent.widthProperty());
        incomingMessageArea.prefHeightProperty().bind(msgParent.heightProperty());
        HBox container = new HBox(msgParent);
        HBox sendContainer = new HBox(sendField);
        HBox.setHgrow(sendField, Priority.ALWAYS);
        HBox.setHgrow(msgParent, Priority.ALWAYS);
        root.add(serverBrowser, 0, 0);
        root.add(container, 1, 0);
        root.add(sendContainer, 1, 1);
        root.setPadding(new Insets(10, 10, 10, 10));

        ColumnConstraints column = new ColumnConstraints();
        column.setPrefWidth(60);
        root.getColumnConstraints().add(column);

        column = new ColumnConstraints();
        column.setPercentWidth(90);
        root.getColumnConstraints().add(column);

        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setPercentHeight(90);
        root.getRowConstraints().add(rowConstraints);

        rowConstraints = new RowConstraints();
        rowConstraints.setPercentHeight(10);
        root.getRowConstraints().add(rowConstraints);

        root.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                                           @Override
                                           public void handle(WindowEvent event) {
                                               try {
                                                   if (currentServer != null) {
                                                       currentServer.close();
                                                       currentServer = null;
                                                   }
                                                   for (Thread t : pool) {
                                                       t.join();
                                                   }
                                               } catch (Exception e) {
                                                   e.printStackTrace();
                                               }
                                           }
                                       }
        );
        File userCred = new File("uesrCred.cred");
        if(userCred.exists() && !userCred.isDirectory()) {
            Scanner fin = new Scanner(userCred);
            name.setVal(fin.nextLine());
            password.setVal(fin.nextLine());
        }else {
            logInPrompt();
            PrintWriter pw = new PrintWriter(userCred);
            pw.println(name.getVal());
            pw.println(password.getVal());
            pw.flush();
            pw.close();
        }
        primaryStage.show();
    }

    private void sendMessage(String msg) {
        if(currentServer.isConnected()) {
            if(writer != null) {
                writer.println("--usrMsg--"+msg);
                writer.flush();
            }
        }
    }

    private GridPane createServerContent() {
        GridPane ret = new GridPane();

        Button addServer = new Button("");
        Background addImg = new Background (new BackgroundImage(
                new Image(Client.class.getResource("/plus.png").toString()),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                new BackgroundSize(1,1,true,true,false,true)
        ));
        addServer.setBackground(addImg);
        addServer.setPrefSize(40, 40);
        addServer.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ServerEntry newEnt = createServerDialog();
                serverEntries.add(newEnt);
                updateServerList();
            }
        });
        ret.add(addServer, 0, 0);
        ret.add(serverList, 0, 1);

        updateServerList();

        return ret;
    }

    private void logInPrompt() {
        Stage dialog = new Stage();
        GridPane newServerRoot = new GridPane();
        TextField name_tf = new TextField("UserName");
        TextField pw_tf = new TextField("Password");
        Button confirmButton = new Button("log in");
        confirmButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                name.setVal(name_tf.getText());
                password.setVal(pw_tf.getText());
                dialog.close();
            }
        });
        newServerRoot.add(name_tf, 0, 0);
        newServerRoot.add(pw_tf,0, 1);
        newServerRoot.add(confirmButton, 0, 2);
        dialog.setScene(new Scene(newServerRoot, 300, 300));
        dialog.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });
        dialog.showAndWait();
    }


    private ServerEntry createServerDialog() {
        MailBox<String> ip = new MailBox<>();
        MailBox<Integer> port = new MailBox<>();
        Stage dialog = new Stage();
        GridPane newServerRoot = new GridPane();
        TextField ip_tf = new TextField("Server IP");
        TextField port_tf = new TextField("Server Port");
        Button confirmButton = new Button("add");
        confirmButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ip.setVal(ip_tf.getText());
                try {
                    port.setVal(Integer.parseInt(port_tf.getText()));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                //todo get picture from the other server
                dialog.close();
            }
        });
        newServerRoot.add(ip_tf, 0, 0);
        newServerRoot.add(port_tf,0, 1);
        newServerRoot.add(confirmButton, 0, 2);
        dialog.setScene(new Scene(newServerRoot, 300, 300));
        dialog.showAndWait();
        String imgPath = getImage(ip.getVal(), port.getVal());
        if(!imgPath.equals("")) {
            return new ServerEntry(ip.getVal(), port.getVal(), imgPath);
        }
        else {
            return new ServerEntry(ip.getVal(), port.getVal(), "/plus.png");
        }
    }

    public String getImage(String ip, int prt) {
        try {
            Socket s = new Socket(ip, prt);
            PrintWriter w = new PrintWriter(s.getOutputStream());
            w.println("--Image Req--");
            w.flush();


            return readImage(s);
        } catch(IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String readImage(Socket s) {
        try {
            byte[] sizeAr = new byte[4];
            s.getInputStream().read(sizeAr);
            int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();

            byte[] imageAr = new byte[size];
            s.getInputStream().read(imageAr);

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageAr));

            System.out.println("Received " + image.getHeight() + "x" + image.getWidth() + ": " + System.currentTimeMillis());

            String dir = "." + File.separator;
            String imgDir = dir + fnv_hash(imageAr) + ".jpg";
            new File(imgDir).createNewFile();
            ImageIO.write(image, "jpg",new FileOutputStream(imgDir));
            return imgDir;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private int fnv_hash (byte[] in) {
        int h = 216613626;
        int i;

        for (i = 0; i < in.length; i++)
            h = (h*16777619) ^ in[i];

        return h;
    }

    private void updateServerList() {
        int row = 1;
        serverList.getChildren().clear();
        for(ServerEntry ser: serverEntries) {
            Circle outline = new Circle();
            Image img;
            img = new Image(new File(ser.getCachedImagePath()).toURI().toString());
            Background imgPat = new Background (new BackgroundImage(
                    img,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                    new BackgroundSize(1,1,true,true,false,true)
            ));
            outline.setRadius(20);
            Button btn = new Button("");
            btn.setPrefSize(outline.getRadius()*2, outline.getRadius()*2);
            btn.setBackground(imgPat);
            btn.setShape(outline);
            btn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                ServerEntry self = ser;

                @Override
                public void handle(MouseEvent event) {
                    if(event.getButton() == MouseButton.SECONDARY) {
                        serverEntries.remove(self);
                        updateServerList();
                    }
                }
            });
            btn.setOnAction(new EventHandler<ActionEvent>() {
                String ip = ser.getIp();
                int port = ser.getPort();

                @Override
                public void handle(ActionEvent event) {
                    System.out.println(ip + ":" +port);
                    connectToServer(ip, port);
                }
            });

            serverList.add(btn, 0, row++);
        }
    }

    boolean connectToServer(String ip, int prt) {
        //todo connect to server

        try {
            if(currentServer != null) {
                currentServer.close();
            }
            currentServer = new Socket(ip, prt);
            reader = new BufferedReader(new InputStreamReader(currentServer.getInputStream()));
            writer = new PrintWriter(currentServer.getOutputStream());
            pool.add(new Thread(new IncomingReader(currentServer, reader)));
            pool.get(pool.size()-1).start();
        } catch(IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Failed to connect to "+ip+":" + prt,
                                ButtonType.OK);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.show();
            e.printStackTrace();
            return false;
        }
        return true;
    }

    String saltAndHashPw(String pw) {
        StringBuilder ret = new StringBuilder();
        SecureRandom random = new SecureRandom();
        File f = new File("Salt.crypt");
        byte[] salt = new byte[16];
        if(f.exists() && !f.isDirectory()) {
            try {
                new FileInputStream(f).read(salt);
            }catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            salt = new byte[16];
            random.nextBytes(salt);
            try {
                new FileOutputStream(f).write(salt);
            }catch(Exception e) {
                e.printStackTrace();
            }
        }
        try {
            KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            byte[] hash = factory.generateSecret(spec).getEncoded();

            for(byte i: hash) {
                ret.append(i);
            }

            return ret.toString();
        }
        catch(InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    class IncomingReader implements Runnable{

        Socket thisSock;
        BufferedReader curReader;

        public IncomingReader(Socket s, BufferedReader b) {
            thisSock = s;
            curReader = b;
        }
        @Override
        public void run() {
            String incomingMessage;
            try{
                authUser();
                writer.println("--greeting--");
                writer.flush();
                while(thisSock == currentServer) {
                    try {
                        if ((incomingMessage = curReader.readLine()) != null) {
                            incomingMessageArea.appendText(incomingMessage + System.lineSeparator());
                            System.out.println(incomingMessage);
                        }
                    } catch(java.net.SocketException e) {
                        break;
                    }
                }
                thisSock.close();
                System.out.println("changing servers");
                incomingMessageArea.clear();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }

        void authUser() {
            writer.println("--userauth-- " + name.getVal() + " " + saltAndHashPw(password.getVal()));
        }
    }
}
