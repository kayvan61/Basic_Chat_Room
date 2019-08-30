/* CHAT ROOM <BasicListener.java>
 *  EE422C Project 7 submission by
 *  Replace <...> with your actual data.
 *  Ali Mansoorshahi
 *  AM85386
 *  Slip days used: <0>
 *  Spring 2019
 */

package assignment7;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;

public class BasicListener {
    private Map<String, PrintWriter> clientOutputStreams;
    private String greeting = "welcome to the thunder dome thot";
    private String defaultImage = "/default.jpg";
    private String userImage = "";
    private StringBuilder previousLog;
    private Map<String, String> users;

    public static void main(String[] args) {
        new BasicListener().start();
    }

    void start() {
        previousLog = new StringBuilder();
        clientOutputStreams = new HashMap<>();
        users = new HashMap<>();
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Enter path to server's picture (empty to use default)");
        userImage = keyboard.nextLine();
        try {
            ServerSocket serverSock = new ServerSocket(4242);
            while (true) {
                Socket clientSocket = serverSock.accept();
                PrintWriter writer = new
                        PrintWriter(clientSocket.getOutputStream());
                Thread t = new Thread(new ClientHandler(clientSocket, writer));
                t.start();
                System.out.println("got a connection");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendServerImage(Socket s) {
        try {
            File f = new File(userImage);
            BufferedImage image;
            if(f.exists() && !f.isDirectory()) {
                image = ImageIO.read(f);
            } else {
                userImage = "";
                System.out.println("using default image");
                image = ImageIO.read(BasicListener.class.getResource(defaultImage));
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", byteArrayOutputStream);

            byte[] size = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array();
            s.getOutputStream().write(size);
            s.getOutputStream().write(byteArrayOutputStream.toByteArray());
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyClients(String message) {
        for (PrintWriter writer : clientOutputStreams.values()) {
            writer.println(message);
            writer.flush();
        }
    }

    private void notifySomeUser(ArrayList<String> users, String msg, boolean isWhisper) {
        String sender = "";
        if(isWhisper) {
            sender = users.get(users.size() - 1);
            users.remove(sender);
        }
        for(String s: users) {
            String w = s.trim();
            if(clientOutputStreams.keySet().contains(w)) {
                if(isWhisper) {
                    clientOutputStreams.get(w).println("Whisper from " + sender + " to " + w + " : " + msg);
                    clientOutputStreams.get(sender).println("Whisper from " + sender + " to " + w + " : " + msg);
                    clientOutputStreams.get(sender).flush();
                    clientOutputStreams.get(w).flush();
                }
                else {
                    clientOutputStreams.get(w).println(msg);
                    clientOutputStreams.get(w).flush();
                }
            }
        }
    }

    private void notifyOneUser(String user, String msg) {
        clientOutputStreams.get(user).println("server : " + msg);
        clientOutputStreams.get(user).flush();

    }

    private ArrayList<String> getUserList(String s) {
        return new ArrayList<>(Arrays.asList(s.split("\\s{0,},\\s{0,}")));
    }

    class ClientHandler implements Runnable {
        private BufferedReader reader;
        private PrintWriter writer;
        Socket cs;
        private boolean authed;
        private String curUser;
        ClientHandler(Socket clientSocket, PrintWriter p) throws
                IOException {
            writer = p;
            authed = true;
            cs = clientSocket;
            reader = new BufferedReader(new
                    InputStreamReader(clientSocket.getInputStream()));
        }
        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) !=
                        null) {
                    if(!authed) {
                        writer.println("invalid log-in (user exists with this name/wrong password)");
                        writer.flush();
                        continue;
                    }
                    if(message.equals("--greeting--")) {
                        writer.println(previousLog.toString());
                        writer.println(greeting);
                        writer.flush();
                        ArrayList<String> curUsers = new ArrayList<>(clientOutputStreams.keySet());
                        curUsers.remove(curUser);
                        notifySomeUser(curUsers, curUser + " has joined!", false);
                        notifyOneUser(curUser, "connected as " + curUser);
                    } else if(message.startsWith("--userauth--")) {
                        String userName = message.split(" ")[1];
                        String pw = message.split(" ")[2];
                        curUser = userName;
                        if(!users.keySet().contains(userName)) {
                            users.put(userName, pw);
                            clientOutputStreams.put(curUser, writer);
                        }
                        if(users.get(userName).equals(pw)) {
                            authed = true;
                            clientOutputStreams.put(curUser, writer);
                        }
                        else {
                            authed = false;
                            writer.println("invalid log-in (user exists with this name/wrong password)");
                            writer.flush();
                        }

                    } else if(message.startsWith("--Image Req--")) {
                        sendServerImage(cs);
                    }
                    else if(message.startsWith("--usrMsg--")){
                        String rawMsg = message.substring("--usrMsg--".length());
                        System.out.println("read " +
                                message);
                        if(rawMsg.startsWith("/w")) {
                            ArrayList<String> inUsers = getUserList(rawMsg.split(";", 2)[0].substring("/w".length()).trim());
                            inUsers.add(curUser);
                            if(!clientOutputStreams.keySet().containsAll(inUsers)) {
                                ArrayList<String> fjds = new ArrayList<>();
                                fjds.add(curUser);
                                notifySomeUser((fjds), "some users arent online", false);
                            } else {
                                notifySomeUser(inUsers, rawMsg.split(";", 2)[1], true);
                            }
                        } else if(rawMsg.startsWith("/users")) {
                            StringBuilder userStrings = new StringBuilder();
                            for(String name : clientOutputStreams.keySet()) {
                                userStrings.append(name).append(System.getProperty("line.separator"));
                            }
                            notifyOneUser(curUser, userStrings.toString());
                        }else {
                            if (!rawMsg.equals("")) {
                                previousLog.append(curUser).append(" : ").append(rawMsg).append("\n");
                                notifyClients(curUser + " : " + rawMsg);
                            }
                        }
                    }
                }
            } catch(IOException e) {
                writer.close();
                clientOutputStreams.remove(curUser);
                e.printStackTrace();
            }
        }
    }
}
