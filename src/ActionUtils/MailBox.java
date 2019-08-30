/* CHAT ROOM <BasicListener.java>
 *  EE422C Project 7 submission by
 *  Replace <...> with your actual data.
 *  Ali Mansoorshahi
 *  AM85386
 *  Slip days used: <0>
 *  Spring 2019
 */

package ActionUtils;

public class MailBox<T> {
    private T val;

    public MailBox() {val = null;}
    public MailBox(T val) {
        this.val = val;
    }

    public void setVal(T val) {
        this.val = val;
    }

    public T getVal() {
        return val;
    }
}
