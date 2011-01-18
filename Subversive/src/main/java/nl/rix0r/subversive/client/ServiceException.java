
package nl.rix0r.subversive.client;

/**
 *
 * @author rix0rrr
 */
public class ServiceException extends Exception {
    public ServiceException() {
    }

    public ServiceException(String string) {
        super(string);
    }

    public ServiceException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public ServiceException(Throwable thrwbl) {
        super(thrwbl.getMessage(), thrwbl);
    }
}
