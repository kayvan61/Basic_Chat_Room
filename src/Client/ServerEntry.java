/* CHAT ROOM <BasicListener.java>
 *  EE422C Project 7 submission by
 *  Replace <...> with your actual data.
 *  Ali Mansoorshahi
 *  AM85386
 *  Slip days used: <0>
 *  Spring 2019
 */

package Client;

public class ServerEntry {

    private String cachedImagePath;
    private String ip;
    private int port;

    public ServerEntry(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public ServerEntry(String ip, int port, String image) {
        this.ip = ip;
        this.port = port;
        this.cachedImagePath = image;
    }

    public String getCachedImagePath() {
        return cachedImagePath;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }
}
